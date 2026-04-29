package com.itheima.service.impl;


import com.itheima.dto.ChatFileDTO;
import com.itheima.mapper.SocialMapper;
import com.itheima.pojo.ChatFile;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.impl.SensitiveFilterService;
import com.itheima.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    @Value("${upload.storage.type:oss}")  // 从配置读取存储类型
    private String storageType;

    @Value("${upload.max-size.chat:10485760}") // 聊天文件最大10MB
    private long maxFileSize;

    private final SocialMapper socialMapper;
    private final SensitiveFilterService sensitiveFilterService;
    private final AliOssUtil aliOssUtil;  // 注入已有的AliOssUtil

    /**
     * 上传聊天文件（整合OSS）
     */
    public Result<ChatFileDTO> uploadChatFile(MultipartFile file,
                                              Integer uploaderId,
                                              Integer receiverId,
                                              String messageId) {
        try {
            // 1. 文件大小校验
            if (file.getSize() > maxFileSize) {
                return Result.error("聊天文件大小不能超过 " + (maxFileSize / 1024 / 1024) + "MB");
            }

            // 2. 文件类型和名称校验
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return Result.error("文件名不能为空");
            }

            // 文件名敏感词过滤
            String filteredName = sensitiveFilterService.filter(originalFilename);
            if (filteredName == null) {
                return Result.error("文件名包含敏感词");
            }

            // 3. 根据存储类型选择上传方式
            String fileUrl;
            String thumbnailUrl = null;
            String fileType = getFileType(file.getContentType(), originalFilename);

            if ("oss".equals(storageType)) {
                // 使用OSS上传
                fileUrl = uploadToOSS(file, fileType, filteredName);
                thumbnailUrl = fileUrl;
            } else {
               return  Result.error("不支持的存储类型");
            }

            // 4. 保存到数据库
            ChatFile chatFile = new ChatFile();
            chatFile.setUploaderId(uploaderId);
            chatFile.setReceiverId(receiverId);
            chatFile.setFileName(filteredName);
            chatFile.setFilePath(fileUrl); // 存储完整的URL或相对路径
            chatFile.setFileType(fileType);
            chatFile.setFileSize(file.getSize());
            chatFile.setThumbnailPath(thumbnailUrl == null ? fileUrl : thumbnailUrl);
            chatFile.setMessageId(messageId);
            chatFile.setCreateTime(LocalDateTime.now());

            socialMapper.insertChatFile(chatFile);

            // 5. 返回文件信息
            ChatFileDTO dto = new ChatFileDTO();
            dto.setId(chatFile.getId());
            dto.setUploaderId(uploaderId);
            dto.setFileName(filteredName);
            dto.setFileUrl(fileUrl);
            dto.setFileType(fileType);
            dto.setFileSize(formatFileSize(file.getSize()));
            dto.setThumbnailUrl(thumbnailUrl);
            dto.setMessageId(messageId);
            dto.setCreateTime(chatFile.getCreateTime());

            return Result.success(dto);

        } catch (Exception e) {
            log.error("聊天文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传到OSS
     */
    private String uploadToOSS(MultipartFile file, String fileType, String fileName) throws IOException {
        // 生成OSS存储路径
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString();
        String extension = getFileExtension(fileName);

        // OSS对象名称：chat/{date}/{type}/{uuid}.{ext}
        String objectName = String.format("chat/%s/%s/%s.%s",
                datePath, fileType, uuid, extension);

        log.info("上传聊天文件到OSS: {}", objectName);

        // 使用已有的AliOssUtil上传
        return aliOssUtil.uploadFile(objectName, file.getInputStream());
    }

    /**
     * 获取聊天文件列表
     */
    public Result<List<ChatFileDTO>> getChatFiles(Integer userId,
                                                  String fileType, Integer pageNum,
                                                  Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        List<ChatFileDTO> files = socialMapper.selectChatFiles(userId, fileType, offset, pageSize);
        return Result.success(files);
    }

    /**
     * 删除聊天文件
     */
    @Transactional
    public Result deleteChatFile(Long fileId, Integer userId) {
        ChatFile file = socialMapper.selectChatFileById(fileId);
        if (file == null) {
            return Result.error("文件不存在");
        }

        // 验证权限：只能删除自己上传的文件
        if (!file.getUploaderId().equals(userId)) {
            return Result.error("无权删除此文件");
        }

        // 删除物理文件
        // 本地存储（备用方案）
        String filePath = file.getFilePath();
        if (filePath != null) {
            File fileToDelete = new File(filePath);
            if (fileToDelete.exists()) {
                fileToDelete.delete();
            }
        }

        // 如果有缩略图，也删除
        String thumbnailPath = file.getThumbnailPath();
        if (thumbnailPath != null) {
            File thumbnailToDelete = new File(thumbnailPath);
            if (thumbnailToDelete.exists()) {
                thumbnailToDelete.delete();
            }
        }


        // 删除数据库记录
        int i = socialMapper.deleteChatFile(fileId);
        if (i == 0) {
            return Result.error("文件删除失败");
        }
        return Result.success("文件删除成功");

    }

    // ==================== 辅助方法 ====================

    private String getFileType(String contentType, String fileName) {
        if (contentType != null) {
            if (contentType.startsWith("image/")) return "image";
            if (contentType.startsWith("video/")) return "video";
            if (contentType.startsWith("audio/")) return "audio";
        }

        String extension = getFileExtension(fileName).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                return "image";
            case "mp4":
            case "avi":
            case "mov":
            case "wmv":
            case "flv":
                return "video";
            case "mp3":
            case "wav":
            case "flac":
            case "aac":
                return "audio";
            default:
                return "file";
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
    }

    private boolean isImage(String fileType) {
        return "image".equals(fileType);
    }


    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }



}
