package com.itheima.service.impl;


import com.itheima.common.UserConstant;
import com.itheima.config.MediaProperties;
import com.itheima.dto.MediaQueryDTO;
import com.itheima.dto.MediaUpdateDTO;
import com.itheima.dto.MediaUploadDTO;
import com.itheima.dto.SubmitBatchRequest;
import com.itheima.exception.BusinessException;
import com.itheima.mapper.SubmitLikeMapper;
import com.itheima.mapper.SubmitMediaMapper;
import com.itheima.mapper.UserDailyUploadMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.MediaLike;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.User;
import com.itheima.pojo.UserMedia;
import com.itheima.service.DailyUploadService;
import com.itheima.service.SubmitMediaService;
import com.itheima.utils.AliOssUtil;
import com.itheima.utils.MediaConverter;
import com.itheima.vo.MediaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmitMediaServiceImpl implements SubmitMediaService {

    private final MediaProperties mediaProperties;

    private final SubmitMediaMapper submitMediaMapper;

    private final SubmitLikeMapper submitLikeMapper;

    private final UserMapper userMapper;

    private final AliOssUtil aliOssUtil;

    private final DailyUploadService dailyUploadService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MediaVO> batchUpload(MediaUploadDTO dto) {
        try {
            // 1. 验证用户是否存在
            User user = userMapper.findUserById(dto.getUserId());
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            // 2. 检查用户状态和VIP状态
            if (!user.isActive()) {
                throw new RuntimeException("账号状态异常，无法上传");
            }

            // 检查VIP是否过期（如果不是终身VIP）
            if (!UserConstant.VIP_TYPE_LIFETIME.equals(user.getVipType()) &&
                    user.getVipExpireTime() != null &&
                    LocalDateTime.now().isAfter(user.getVipExpireTime())) {

                // 可以在这里添加逻辑：自动将过期VIP降级
                log.warn("用户 {} 的VIP已过期，时间：{}",
                        dto.getUserId(), user.getVipExpireTime());
            }

            List<MultipartFile> files = dto.getFiles();

            // 3. 验证文件列表
            if (files == null || files.isEmpty()) {
                throw new RuntimeException("文件不能为空");
            }

            // 4. 检查上传限制（根据VIP类型）
            dailyUploadService.checkUploadLimit(dto.getUserId(), files.size());

            // 5. 检查是否有视频文件
            boolean hasVideo = files.stream().anyMatch(file ->
                    "video".equals(determineMediaType(file.getContentType(), file.getOriginalFilename()))
            );

            // 视频文件只能单独上传
            if (hasVideo && files.size() > 1) {
                throw new RuntimeException("视频文件只能单独上传，不能批量上传");
            }

            // 验证视频是否合法
            if (hasVideo) {
                validateVideoFile(files.get(0));
            }

            int imageMaxBatchCount = mediaProperties.getImage().getMaxBatchCount();
            // 6. 限制批量上传数量（图片最多10个）
            if (!hasVideo && files.size() > imageMaxBatchCount) {
                throw new RuntimeException("一次最多只能上传" + imageMaxBatchCount + "个文件");
            }

            List<MediaVO> results = new ArrayList<>();
            List<String> failedFiles = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            // 记录成功的文件数
            int successCount = 0;

            // 逐个处理文件
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String fileName = file.getOriginalFilename();

                try {
                    MediaVO result;
                    String mediaType = determineMediaType(file.getContentType(), fileName);

                    if ("video".equals(mediaType)) {
                        checkFFmpeg();
                        result = processVideoFile(file, dto, now);
                    } else {
                        result = processSingleFile(file, dto, now);
                    }

                    results.add(result);
                    successCount++;

                    log.info("用户 {} 成功上传文件 {}/{}: {} (VIP类型: {})",
                            dto.getUserId(), i + 1, files.size(), fileName,
                            dailyUploadService.getVipTypeName(user.getVipType()));

                } catch (Exception e) {
                    // 记录失败的文件，但继续处理其他文件
                    log.error("用户 {} 文件 {} 上传失败: {}",
                            dto.getUserId(), fileName, e.getMessage());
                    failedFiles.add(fileName + " - " + e.getMessage());
                }
            }

            // 7. 更新上传计数（只计算成功的文件）
            if (successCount > 0) {
                dailyUploadService.updateUploadCount(dto.getUserId(), successCount);
            }

            // 8. 检查是否有文件失败
            if (!failedFiles.isEmpty()) {
                // 如果有失败的文件，但至少有一个成功，就返回成功的部分
                if (!results.isEmpty()) {
                    log.warn("用户 {} 批量上传部分成功: {}/{} 个文件成功, 失败文件: {}",
                            dto.getUserId(), results.size(), files.size(), failedFiles);

                    // 可以在这里抛出一个业务异常，提示部分失败
                    throw new BusinessException("部分文件上传失败: " + String.join(", ", failedFiles));
                } else {
                    // 全部失败，抛出异常触发回滚
                    throw new RuntimeException("所有文件上传失败: " + String.join(", ", failedFiles));
                }
            }

            log.info("用户 {} 批量上传了 {} 个媒体文件 (VIP类型: {}, 今日累计: {})",
                    dto.getUserId(), results.size(),
                    dailyUploadService.getVipTypeName(user.getVipType()),
                    dailyUploadService.getTodayUploadCount(dto.getUserId()));

            return results;

        } catch (BusinessException e) {
            // 业务异常，不触发回滚（如果配置了）
            throw e;
        } catch (Exception e) {
            log.error("批量上传媒体文件失败", e);
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PageBean<MediaVO> querySubmitList(MediaQueryDTO queryDTO) {
        // 1. 设置默认值
        if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
            queryDTO.setPageSize(20);
        }
        if (queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(100); // 限制每页最大100条
        }

        // 3. 查询总记录数
        Long total = submitMediaMapper.countByCondition(queryDTO);

        // 4. 查询数据列表
        List<UserMedia> mediaList = submitMediaMapper.selectByCondition(queryDTO);

        // 5. 转换为VO列表，并设置点赞状态
        List<MediaVO> voList = mediaList.stream()
                .map(media -> {
                    MediaVO vo = MediaConverter.toVO(media);

                    // 检查当前用户是否已点赞
                    if (queryDTO.getUserId() != null) {
                        Integer likeCount = submitLikeMapper.countByMediaAndUser(media.getId(), queryDTO.getUserId());
                        vo.setLiked(likeCount > 0);
                    }

                    return vo;
                })
                .collect(Collectors.toList());

        // 6. 创建PageBean
        PageBean<MediaVO> pageBean = new PageBean<>();
        pageBean.setTotal(total);
        pageBean.setPageNum(queryDTO.getPageNum());
        pageBean.setPageSize(queryDTO.getPageSize());
        pageBean.setItems(voList);

        return pageBean;
    }

    @Override
    public PageBean<MediaVO> querySubmitByUserAndStatus(MediaQueryDTO queryDTO) {
        // 1. 设置默认值
        if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
            queryDTO.setPageSize(20);
        }
        if (queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(100);
        }

        // 2. 验证必需参数
        if (queryDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (queryDTO.getStatus() == null || queryDTO.getStatus().isEmpty()) {
            throw new IllegalArgumentException("状态不能为空");
        }
        if (!"active".equals(queryDTO.getStatus()) && !"pending".equals(queryDTO.getStatus()) && !"hidden".equals(queryDTO.getStatus())) {
            throw new IllegalArgumentException("状态参数必须是 active，pending或hidden");
        }

        // 3. 查询总记录数
        Long total = submitMediaMapper.countByUserAndStatus(queryDTO);

        // 4. 查询数据列表
        List<UserMedia> mediaList = submitMediaMapper.selectByUserAndStatus(queryDTO);

        // 5. 转换为VO列表，并设置点赞状态
        List<MediaVO> voList = mediaList.stream()
                .map(media -> {
                    MediaVO vo = MediaConverter.toVO(media);

                    // 检查当前用户是否已点赞
                    if (queryDTO.getUserId() != null) {
                        Integer likeCount = submitLikeMapper.countByMediaAndUser(
                                media.getId(), queryDTO.getUserId());
                        vo.setLiked(likeCount > 0);
                    }

                    return vo;
                })
                .collect(Collectors.toList());

        // 6. 创建PageBean
        PageBean<MediaVO> pageBean = new PageBean<>();
        pageBean.setTotal(total);
        pageBean.setPageNum(queryDTO.getPageNum());
        pageBean.setPageSize(queryDTO.getPageSize());
        pageBean.setItems(voList);

        return pageBean;
    }

    @Override
    public PageBean<MediaVO> queryAllStatusSubmitByUser(MediaQueryDTO queryDTO) {
        // 1. 设置默认值
        if (queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1);
        }
        if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
            queryDTO.setPageSize(20);
        }
        if (queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(100);
        }

        // 2. 验证必需参数
        if (queryDTO.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 3. 查询总记录数（不筛选状态）
        Long total = submitMediaMapper.countAllStatusByUser(queryDTO);

        // 4. 查询数据列表（不筛选状态）
        List<UserMedia> mediaList = submitMediaMapper.selectAllStatusByUser(queryDTO);

        // 5. 转换为VO列表，并设置点赞状态
        List<MediaVO> voList = mediaList.stream()
                .map(media -> {
                    MediaVO vo = MediaConverter.toVO(media);

                    // 检查当前用户是否已点赞
                    if (queryDTO.getUserId() != null) {
                        Integer likeCount = submitLikeMapper.countByMediaAndUser(
                                media.getId(), queryDTO.getUserId());
                        vo.setLiked(likeCount > 0);
                    }

                    return vo;
                })
                .collect(Collectors.toList());

        // 6. 创建PageBean
        PageBean<MediaVO> pageBean = new PageBean<>();
        pageBean.setTotal(total);
        pageBean.setPageNum(queryDTO.getPageNum());
        pageBean.setPageSize(queryDTO.getPageSize());
        pageBean.setItems(voList);

        return pageBean;
    }

    @Override
    @Transactional
    public boolean deleteMedia(Integer mediaId, Integer userId) {
        // 1. 验证权限
        UserMedia media = submitMediaMapper.selectById(mediaId);
        if (media == null) {
            throw new RuntimeException("媒体文件不存在");
        }

        // 2. 检查是否是文件所有者
        if (!media.getUserId().equals(userId)) {
            throw new RuntimeException("没有权限删除此文件");
        }

        // 3. 直接删除数据库中的数据
        int rows = submitMediaMapper.delete(mediaId);

        log.info("用户 {} 删除了媒体文件: {}", userId, mediaId);

        return rows > 0;
    }

    @Override
    @Transactional
    public MediaVO updateMedia(Integer mediaId, MediaUpdateDTO updateDTO) {
        // 1. 验证权限
        UserMedia media = submitMediaMapper.selectById(mediaId);
        if (media == null || "hidden".equals(media.getStatus())) {
            throw new RuntimeException("媒体文件不存在或未通过审核");
        }

        if (!media.getUserId().equals(updateDTO.getUserId())) {
            throw new RuntimeException("没有权限修改此文件");
        }

        // 2. 构建更新对象
        UserMedia updateMedia = new UserMedia();
        updateMedia.setId(mediaId);
        updateMedia.setUpdateTime(LocalDateTime.now());

        // 3. 只更新非空字段
        if (updateDTO.getFilename() != null) {
            updateMedia.setFilename(updateDTO.getFilename());
        }
        if (updateDTO.getDescription() != null) {
            updateMedia.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getIsPublic() != null) {
            updateMedia.setIsPublic(updateDTO.getIsPublic());
        }
        updateMedia.setStatus("active");
        updateMedia.setUpdateTime(LocalDateTime.now());

        // 4. 执行更新
        int rows = submitMediaMapper.update(updateMedia);
        if (rows == 0) {
            throw new RuntimeException("更新失败");
        }

        // 5. 获取更新后的数据并返回
        UserMedia updatedMedia = submitMediaMapper.selectById(mediaId);
        return MediaConverter.toVO(updatedMedia);
    }

    @Override
    @Transactional
    public boolean toggleLike(Integer mediaId, Integer userId) {
        try {
            // 1. 检查媒体是否存在且未删除
            UserMedia media = submitMediaMapper.selectById(mediaId);
            if (media == null || "pending".equals(media.getStatus()) || "hidden".equals(media.getStatus())) {
                throw new RuntimeException("媒体文件暂时无法点赞");
            }

            // 2. 检查用户是否有权限点赞（如果是私有的，只有所有者能点赞）
            if (!media.getIsPublic() && !media.getUserId().equals(userId)) {
                throw new RuntimeException("没有权限对此文件进行点赞");
            }

            // 3. 检查是否已经点赞
            MediaLike existingLike = submitLikeMapper.selectByMediaAndUser(mediaId, userId);

            if (existingLike != null) {
                // 已经点赞，执行取消点赞
                submitLikeMapper.delete(existingLike.getId());
                // 更新点赞数
                submitMediaMapper.decrementLikeCount(mediaId);
                //更新点赞状态
                submitMediaMapper.updateLikeStatus(mediaId, 0);
                log.info("用户 {} 取消点赞媒体文件: {}", userId, mediaId);
            } else {
                // 未点赞，执行点赞
                MediaLike newLike = new MediaLike();
                newLike.setMediaId(mediaId);
                newLike.setUserId(userId);
                newLike.setCreateTime(LocalDateTime.now());

                submitLikeMapper.insert(newLike);
                // 更新点赞数
                submitMediaMapper.incrementLikeCount(mediaId);
                //更新点赞状态
                submitMediaMapper.updateLikeStatus(mediaId, 1);
                log.info("用户 {} 点赞媒体文件: {}", userId, mediaId);
            }
            return true;
        } catch (Exception e) {
            log.error("点赞操作失败", e);
            throw new RuntimeException("点赞操作失败: " + e.getMessage());
        }
    }


    @Override
    @Transactional
    public boolean updateStatus(Integer id, String status) {
        try {
            UserMedia media = submitMediaMapper.selectById(id);
            if (media == null) {
                throw new RuntimeException("媒体文件不存在");
            }
            int rows = submitMediaMapper.updateStatus(id, status);
            log.info("媒体文件id为: {} 状态修改成了: {}", id, status);
            return rows > 0;
        } catch (Exception e) {
            log.error("修改状态失败", e);
        }
        return false;
    }

    // 批量更新媒体状态
    @Override
    public boolean batchUpdateStatus(List<Integer> ids, String status, String tags) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ID列表不能为空");
        }

        if (!Arrays.asList("active", "pending", "hidden").contains(status)) {
            throw new IllegalArgumentException("无效的状态值");
        }

        // 更新数据库
        int affectedRows = submitMediaMapper.batchUpdateStatus(ids, status, tags);

        // 只要有行被影响就认为是成功的
        // 有些行可能已经是目标状态，所以affectedRows可能小于ids.size()
        return affectedRows >= 0;  // 总是返回true，除非SQL执行失败
    }


    //获取用户最近上传
    @Override
    public List<UserMedia> getRecentMedia(Integer userId, Integer limit) {
        // 添加业务逻辑校验
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }

        // 设置合理的limit范围
        limit = limit == null ? 20 : Math.min(limit, 100);

        return submitMediaMapper.selectRecentByUserId(userId, limit);
    }

    /**
     * 处理单个文件的上传逻辑
     */
    private MediaVO processSingleFile(MultipartFile file, MediaUploadDTO dto, LocalDateTime uploadTime)
            throws Exception {
        // 1. 基本验证
        validateFile(file);

        // 2. 获取媒体信息
        MediaMetaInfo metaInfo = extractMediaMetaInfo(file);

        String originalFilename = file.getOriginalFilename();
        String mediaType = determineMediaType(file.getContentType(), originalFilename);

        // 4. 生成唯一文件名
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = generateUniqueFilename(fileExtension);


        // 5. 上传到阿里云OSS
        String ossUrl;
        try (InputStream inputStream = file.getInputStream()) {
            // 构建OSS存储路径：media/{userId}/{year}/{month}/{filename}
            String ossObjectName = buildOssObjectName(dto.getUserId(), newFilename);
            ossUrl = aliOssUtil.uploadFile(ossObjectName, inputStream);
        }

        // 6. 创建并保存媒体记录到数据库
        UserMedia userMedia = new UserMedia();
        userMedia.setUserId(dto.getUserId());
        userMedia.setMediaType(mediaType);
        userMedia.setWidth(metaInfo.width);
        userMedia.setHeight(metaInfo.height);
        userMedia.setDuration(metaInfo.duration);

        //如果filename不为空就直接使用
        if (dto.getFilename() != null && !dto.getFilename().isEmpty()) {
            userMedia.setFilename(dto.getFilename());
        } else {
            userMedia.setFilename(newFilename);
        }
        userMedia.setOriginalName(originalFilename);
        userMedia.setFilePath(ossUrl); // OSS URL
        userMedia.setThumbnailPath(ossUrl); // 简化处理，使用原图作为缩略图
        userMedia.setFileSize(file.getSize());
        userMedia.setMimeType(file.getContentType());
        userMedia.setDescription(dto.getDescription());
        userMedia.setCategory(dto.getCategory());
        String leftOrRight = Math.random() > 0.5 ? "left" : "right";
        userMedia.setWall(dto.getWall() == null ? leftOrRight : dto.getWall());
        userMedia.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);
        userMedia.setUploadIp(dto.getUploadIp());
        userMedia.setUploadTime(uploadTime);
        userMedia.setUpdateTime(uploadTime);
        userMedia.setViewCount(0);
        userMedia.setLikeCount(0);
        userMedia.setStatus("pending");

        submitMediaMapper.insert(userMedia);

        return MediaConverter.toVO(userMedia);
    }


    /**
     * 专门处理视频文件的上传逻辑
     */
    private MediaVO processVideoFile(MultipartFile file, MediaUploadDTO dto, LocalDateTime uploadTime)
            throws Exception {
        File tempFile = null;
        try {
            log.debug("开始处理视频文件: {}", file.getOriginalFilename());

            // 1. 创建临时文件副本
            tempFile = File.createTempFile("upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);
            long fileSize = tempFile.length();
            double fileSizeMB = fileSize / (1024.0 * 1024);
            log.debug("临时文件创建成功: {} (大小: {} MB)",
                    tempFile.getAbsolutePath(), String.format("%.2f", fileSizeMB));

            // 2. 提取元数据
            MediaMetaInfo metaInfo = extractVideoMetaInfo(tempFile);
            log.debug("元数据提取完成: {}x{}, {}秒",
                    metaInfo.width, metaInfo.height, metaInfo.duration);

            // 3. 根据文件大小选择上传方式
            String ossObjectName = buildOssObjectName(dto.getUserId(),
                    generateUniqueFilename(getFileExtension(file.getOriginalFilename())));

            String ossUrl;
            long uploadStartTime = System.currentTimeMillis();

            if (fileSize > 10 * 1024 * 1024) { // 大于10MB使用分片上传
                log.info("文件较大({} MB)，使用分片上传加速", String.format("%.2f", fileSizeMB));
                ossUrl = uploadVideoWithMultipart(ossObjectName, tempFile);
            } else {
                log.info("文件较小({} MB)，使用普通上传", String.format("%.2f", fileSizeMB));
                ossUrl = aliOssUtil.uploadFile(ossObjectName, tempFile);
            }

            long uploadEndTime = System.currentTimeMillis();
            long uploadDuration = (uploadEndTime - uploadStartTime) / 1000;
            double uploadSpeed = fileSize / 1024.0 / uploadDuration; // KB/ms

            log.info("OSS上传完成，耗时: {}s, 平均速度: {} KB/s",
                    uploadDuration, String.format("%.2f", uploadSpeed));

            // 4. 生成缩略图（添加异常处理）
//            log.debug("开始生成缩略图...");
//            String thumbnailPath;
//            try {
//                thumbnailPath = generateThumbnailForVideo(tempFile);
//                log.debug("缩略图生成成功: {}", thumbnailPath);
//            } catch (Exception e) {
//                log.error("生成缩略图失败，使用默认", e);
//                thumbnailPath = getDefaultThumbnailUrl();
//            }

            // 5. 保存到数据库（添加详细日志和异常处理）
            log.debug("开始保存到数据库...");

            UserMedia userMedia = new UserMedia();
            userMedia.setUserId(dto.getUserId());
            userMedia.setMediaType("video");
            userMedia.setWidth(metaInfo.width);
            userMedia.setHeight(metaInfo.height);
            userMedia.setDuration(metaInfo.duration);
            userMedia.setFilename(dto.getFilename() != null && !dto.getFilename().isEmpty()
                    ? dto.getFilename() : file.getOriginalFilename());
            userMedia.setOriginalName(file.getOriginalFilename());
            userMedia.setFilePath(ossUrl);
            userMedia.setThumbnailPath(getDefaultThumbnailUrl());
            userMedia.setFileSize(tempFile.length());
            userMedia.setMimeType(file.getContentType());
            userMedia.setDescription(dto.getDescription());
            userMedia.setCategory(dto.getCategory());
            userMedia.setWall(dto.getWall() != null ? dto.getWall() : Math.random() > 0.5 ? "left" : "right");
            userMedia.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);
            userMedia.setUploadIp(dto.getUploadIp());
            userMedia.setUploadTime(uploadTime);
            userMedia.setUpdateTime(uploadTime);
            userMedia.setViewCount(0);
            userMedia.setLikeCount(0);
            userMedia.setStatus("pending");

            // 打印要插入的数据
            log.debug("准备插入的UserMedia对象: {}", userMedia);
            log.debug("UserMedia字段值: userId={}, mediaType={}, filename={}, filePath={}, fileSize={}, status={}",
                    userMedia.getUserId(), userMedia.getMediaType(), userMedia.getFilename(),
                    userMedia.getFilePath(), userMedia.getFileSize(), userMedia.getStatus());

            try {
                int result = submitMediaMapper.insert(userMedia);
                log.info("数据库插入完成，影响行数: {}, 生成的ID: {}", result, userMedia.getId());

                // 验证是否真的插入了
                if (userMedia.getId() != null) {
                    log.info("记录已保存，ID: {}", userMedia.getId());
                } else {
                    log.warn("插入成功但未获取到ID，可能useGeneratedKeys配置有问题");
                }

            } catch (Exception e) {
                log.error("数据库插入异常", e);
                // 打印SQL相关的详细信息
                log.error("插入SQL参数详情: userId={}, filePath={}, thumbnailPath={}",
                        userMedia.getUserId(), userMedia.getFilePath(), userMedia.getThumbnailPath());
                throw new RuntimeException("数据库保存失败: " + e.getMessage(), e);
            }

            MediaVO vo = MediaConverter.toVO(userMedia);
            log.debug("视频处理完成，返回VO: {}", vo);

            return vo;

        } catch (Exception e) {
            log.error("处理视频文件时发生异常: {}", file.getOriginalFilename(), e);
            throw e;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                    log.debug("临时文件已清理");
                } catch (IOException e) {
                    log.warn("临时文件清理失败", e);
                }
            }
        }
    }


    // 辅助方法：验证文件
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 获取媒体类型
        String mediaType = determineMediaType(file.getContentType(), file.getOriginalFilename());
        //获取文件大小配置信息
        long imageMaxSize = mediaProperties.getImage().getMaxSize();
        // 根据不同类型设置不同限制
        if ("image".equals(mediaType)) {
            if (file.getSize() > imageMaxSize) {
                throw new RuntimeException("图片大小不能超过30MB");
            }
        } else {
            // 其他类型使用默认限制
            if (file.getSize() > imageMaxSize) {
                throw new RuntimeException("文件大小不能超过" + (imageMaxSize / 1024 / 1024) + "MB");
            }
        }
    }

    // 视频专用验证
    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("视频文件不能为空");
        }

        long videoMaxSize = mediaProperties.getVideo().getMaxSize();
        if (file.getSize() > videoMaxSize) {
            throw new RuntimeException("视频大小不能超过500MB");
        }

        // 验证视频类型
        String mediaType = determineMediaType(file.getContentType(), file.getOriginalFilename());
        if (!"video".equals(mediaType)) {
            throw new RuntimeException("文件不是有效的视频格式");
        }
    }

    // 视频专用验证（重载版本，支持File参数）

    private void validateVideoFile(File file) {
        if (file == null || !file.exists()) {
            throw new RuntimeException("视频文件不存在");
        }
        long videoMaxSize = mediaProperties.getVideo().getMaxSize();
        long fileSize = file.length();
        if (fileSize > videoMaxSize) {
            double sizeInMB = fileSize / (1024.0 * 1024);
            throw new RuntimeException(String.format("视频大小不能超过500MB，当前大小: %.2f", sizeInMB));
        }

        log.debug("视频文件大小验证通过: {} bytes ({} MB)",
                fileSize, String.format("%.2f", fileSize / (1024.0 * 1024)));

        // 2. 验证视频格式
        validateVideoFormat(file);

        // 3. 验证视频类型
        String fileName = file.getName();
        String contentType = null;
        try {
            contentType = Files.probeContentType(file.toPath());
            log.debug("探测到的Content-Type: {}", contentType);
        } catch (IOException e) {
            log.warn("无法探测文件类型: {}", fileName);
        }

        String mediaType = determineMediaType(contentType, fileName);
        if (!"video".equals(mediaType)) {
            throw new RuntimeException("文件不是有效的视频格式: " + fileName);
        }

        log.debug("视频文件验证通过: {}", fileName);
    }

    /**
     * 验证视频格式（支持File参数）
     */
    private void validateVideoFormat(File file) {
        String fileName = file.getName();

        // 支持的视频格式
        String[] supportedFormats = {"mp4", "avi", "mov", "mkv", "flv", "wmv", "webm", "m4v"};

        // 获取文件扩展名
        String ext = getFileExtension(fileName);
        if (ext == null || ext.isEmpty()) {
            throw new RuntimeException("文件没有扩展名: " + fileName);
        }

        String lowerExt = ext.toLowerCase();

        // 检查是否支持
        boolean isSupported = false;
        for (String format : supportedFormats) {
            if (format.equals(lowerExt)) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            String supportedList = String.join(", ", supportedFormats);
            throw new RuntimeException(String.format(
                    "不支持的视频格式: %s。支持的格式: %s", ext, supportedList));
        }

        log.debug("视频格式验证通过: {}", ext);
    }

    /**
     * 验证视频格式（保留原有的MultipartFile版本）
     */
    private void validateVideoFormat(MultipartFile file) {
        String fileName = file.getOriginalFilename();

        // 支持的视频格式
        String[] supportedFormats = {"mp4", "avi", "mov", "mkv", "flv", "wmv", "webm", "m4v"};

        // 获取文件扩展名
        String ext = getFileExtension(fileName);
        if (ext == null || ext.isEmpty()) {
            throw new RuntimeException("文件没有扩展名: " + fileName);
        }

        String lowerExt = ext.toLowerCase();

        // 检查是否支持
        boolean isSupported = false;
        for (String format : supportedFormats) {
            if (format.equals(lowerExt)) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            String supportedList = String.join(", ", supportedFormats);
            throw new RuntimeException(String.format(
                    "不支持的视频格式: %s。支持的格式: %s", ext, supportedList));
        }

        log.debug("视频格式验证通过: {}", ext);
    }

    // 辅助方法：提取图片元信息
    private MediaMetaInfo extractMediaMetaInfo(MultipartFile file) throws IOException, InterruptedException {
        MediaMetaInfo metaInfo = new MediaMetaInfo();
        String mediaType = determineMediaType(file.getContentType(), file.getOriginalFilename());

        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image != null) {
                metaInfo.width = image.getWidth();
                metaInfo.height = image.getHeight();
                log.info("图片分辨率: {}x{}", metaInfo.width, metaInfo.height);
            }
        }
        return metaInfo;
    }

    // 提取视频元信息（修改为接收File参数）
    private MediaMetaInfo extractVideoMetaInfo(File videoFile) throws IOException, InterruptedException {
        MediaMetaInfo metaInfo = new MediaMetaInfo();

        try {
            // 处理文件（保持原有逻辑）
            Process process = new ProcessBuilder(
                    mediaProperties.getFfmpeg().getPath(),
                    "-i",
                    videoFile.getAbsolutePath(),
                    "-f", "null", "-"
            ).redirectErrorStream(true).start();

            try {
                String output = readProcessOutput(process);
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    throw new RuntimeException("FFmpeg处理失败，退出码: " + exitCode);
                }

                // 解析元数据...
                metaInfo.duration = parseDuration(output);
                int[] resolution = parseResolution(output);
                metaInfo.width = resolution[0];
                metaInfo.height = resolution[1];

                log.info("视频元数据提取成功 - 时长: {}秒, 分辨率: {}x{}",
                        metaInfo.duration, metaInfo.width, metaInfo.height);
            } finally {
                process.destroy();
            }

            return metaInfo;

        } finally {
            // 这里不需要删除文件，由调用方管理
            log.debug("元信息提取完成，临时文件由调用方管理: {}", videoFile.getAbsolutePath());
        }
    }


    // 元信息辅助类
    private static class MediaMetaInfo {
        int width;
        int height;
        double duration;
    }


    /**
     * 消费流，避免阻塞
     */
    private void consumeStream(InputStream inputStream) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                while (reader.readLine() != null) {
                    // 只读取，不处理
                }
            } catch (IOException e) {
                // 忽略
            }
        }).start();
    }

    // 添加默认缩略图URL方法
    private String getDefaultThumbnailUrl() {
        return "https://web-aliyun-zsw.oss-cn-shanghai.aliyuncs.com/%E6%83%A0%E5%A4%A9%E4%B8%8BLOGO.png"; // 替换为实际的默认图片
    }

    // ========== 私有工具方法 ==========

    // 在服务初始化时检查FFmpeg
    private void checkFFmpeg() {
        try {
            // 明确指定ffmpeg路径（根据你的实际安装路径调整）
            String ffmpegPath = mediaProperties.getFfmpeg().getPath();

            Process process = new ProcessBuilder(ffmpegPath, "-version")
                    .redirectErrorStream(true)
                    .start();

            if (process.waitFor() != 0) {
                throw new RuntimeException("FFmpeg未正确安装或配置");
            }

            // 读取输出验证版本
            String output = readProcessOutput(process);
            if (!output.contains("FFmpeg developers")) {
                throw new RuntimeException("FFmpeg版本信息异常");
            }

        } catch (Exception e) {
            throw new RuntimeException("FFmpeg检查失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分片上传视频文件（加速上传）
     */
    private String uploadVideoWithMultipart(String objectName, File videoFile) {
        log.info("开始分片上传加速: {}, 大小: {} MB",
                objectName, String.format("%.2f", videoFile.length() / (1024.0 * 1024)));

        long fileSize = videoFile.length();

        // 根据文件大小动态调整分片大小
        long partSize;
        if (fileSize < 50 * 1024 * 1024) { // <50MB
            partSize = 5 * 1024 * 1024;    // 5MB
        } else if (fileSize < 200 * 1024 * 1024) { // 50-200MB
            partSize = 10 * 1024 * 1024;   // 10MB
        } else { // >200MB
            partSize = 20 * 1024 * 1024;   // 20MB
        }

        // 使用优化版本的分片上传
        return aliOssUtil.uploadLargeFileOptimized(objectName, videoFile, partSize);
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    // 根据MIME类型和文件名判断媒体类型
    private String determineMediaType(String mimeType, String filename) {
        String extension = getFileExtension(filename);

        // 根据MIME类型判断
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                return "image";
            } else if (mimeType.startsWith("video/")) {
                return "video";
            }
        }

        // 根据扩展名判断
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"};
        String[] videoExtensions = {"mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"};

        for (String ext : imageExtensions) {
            if (ext.equals(extension)) {
                return "image";
            }
        }

        for (String ext : videoExtensions) {
            if (ext.equals(extension)) {
                return "video";
            }
        }

        // 默认作为其他类型处理
        return "other";
    }

    private String generateUniqueFilename(String extension) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    private String buildOssObjectName(Integer userId, String filename) {
        LocalDateTime now = LocalDateTime.now();
        // 格式: media/{userId}/{year}/{month}/{filename}
        return String.format("media/%d/%d/%02d/%s",
                userId,
                now.getYear(),
                now.getMonthValue(),
                filename);
    }

    // 读取进程输出
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    // 解析视频时长
    private double parseDuration(String ffmpegOutput) {
        // 示例输出行: Duration: 00:01:30.45
        String durationLine = ffmpegOutput.lines()
                .filter(line -> line.contains("Duration:"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("无法解析视频时长"));

        String durationStr = durationLine.split("Duration:")[1].trim().split(",")[0].trim();
        String[] parts = durationStr.split(":");

        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);

        return hours * 3600 + minutes * 60 + seconds;
    }

    // 解析视频分辨率
    private int[] parseResolution(String ffmpegOutput) {
        // 更健壮的解析逻辑
        String resolutionLine = ffmpegOutput.lines()
                .filter(line -> line.contains("Video:") && line.contains(" yuv"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("无法解析视频分辨率"));

        // 示例行: Stream #0:0[0x1](und): Video: h264 (Main) (avc1 / 0x31637661), yuv420p(progressive), 1080x1920
        Pattern pattern = Pattern.compile("\\d{3,4}x\\d{3,4}");
        Matcher matcher = pattern.matcher(resolutionLine);

        if (!matcher.find()) {
            throw new RuntimeException("无法从FFmpeg输出中提取分辨率: " + resolutionLine);
        }

        String[] dimensions = matcher.group().split("x");
        return new int[]{
                Integer.parseInt(dimensions[0]),
                Integer.parseInt(dimensions[1])
        };
    }

    @Override
    @Transactional
    public List<MediaVO> batchSubmit(Integer userId, SubmitBatchRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("提交列表不能为空");
        }
        User user = userMapper.findUserById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (!user.isActive()) throw new BusinessException("账号状态异常，无法提交");

        List<MediaVO> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int successCount = 0;

        for (SubmitBatchRequest.SubmitItem item : request.getItems()) {
            String originalName = item.getOriginalName() != null ? item.getOriginalName()
                    : item.getFileUrl().substring(item.getFileUrl().lastIndexOf('/') + 1);
            String fileName = item.getFileName() != null ? item.getFileName()
                    : originalName.replaceFirst("\\.[^.]+$", "");
            UserMedia media = new UserMedia();
            media.setUserId(userId);
            media.setMediaType(item.getMediaType() != null ? item.getMediaType() : "image");
            media.setMimeType(item.getMimeType() != null && !item.getMimeType().isEmpty()
                    ? item.getMimeType() : "application/octet-stream");
            media.setWidth(item.getWidth());
            media.setHeight(item.getHeight());
            media.setDuration(item.getDuration());
            media.setFilename(fileName);
            media.setOriginalName(originalName);
            media.setFilePath(item.getFileUrl());
            media.setThumbnailPath(item.getFileUrl());
            media.setFileSize(item.getFileSize());
            media.setMimeType(item.getMediaType());
            media.setDescription(item.getDescription());
            media.setCategory(item.getCategory());
            media.setWall(item.getWall() != null ? item.getWall() :
                    (Math.random() > 0.5 ? "left" : "right"));
            media.setIsPublic(item.getIsPublic() != null ? item.getIsPublic() : true);
            media.setUploadIp(request.getUploadIp());
            media.setUploadTime(now);
            media.setUpdateTime(now);
            media.setViewCount(0);
            media.setLikeCount(0);
            media.setStatus("pending");
            submitMediaMapper.insert(media);
            results.add(MediaConverter.toVO(media));
            successCount++;
        }

        if (successCount > 0) {
            try { dailyUploadService.updateUploadCount(userId, successCount); }
            catch (Exception e) { log.warn("更新上传计数失败: userId={}", userId, e); }
        }
        log.info("用户 {} 批量提交了 {} 个媒体文件", userId, results.size());
        return results;
    }

}
