package com.itheima.service.impl;

import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PartListing;
import com.aliyun.oss.model.PartSummary;
import com.itheima.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OSS 上传服务（封装分片上传 / 断点续传 / 签名URL / 防盗链）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssUploadService {

    private final AliOssUtil aliOssUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String UPLOAD_KEY_PREFIX = "oss:upload:";
    private static final long UPLOAD_TTL_HOURS = 24;

    /** 存上传状态到 Redis */
    private void saveUploadState(String uploadId, Map<String, Object> state) {
        redisTemplate.opsForValue().set(
                UPLOAD_KEY_PREFIX + uploadId, state, UPLOAD_TTL_HOURS, TimeUnit.HOURS);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUploadState(String uploadId) {
        Object val = redisTemplate.opsForValue().get(UPLOAD_KEY_PREFIX + uploadId);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }

    private void deleteUploadState(String uploadId) {
        redisTemplate.delete(UPLOAD_KEY_PREFIX + uploadId);
    }

    /** 生成OSS存储路径 */
    public String generateObjectName(Integer userId, String originalFilename, String folder) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ext = getFileExtension(originalFilename);
        if (ext.isEmpty()) ext = "bin";
        return String.format("%s/%d/%s/%s.%s", folder, userId, datePath, uuid, ext);
    }

    /**
     * 初始化分片上传
     * @return { uploadId, objectName }
     */
    public Map<String, String> initChunkUpload(Integer userId, String fileName,
                                               long fileSize, String folder) {
        String objectName = generateObjectName(userId, fileName, folder);
        String uploadId = aliOssUtil.initMultipartUpload(objectName);

        Map<String, Object> state = new HashMap<>();
        state.put("objectName", objectName);
        state.put("userId", userId);
        state.put("fileName", fileName);
        state.put("fileSize", fileSize);
        state.put("folder", folder);
        state.put("createTime", System.currentTimeMillis());
        saveUploadState(uploadId, state);

        Map<String, String> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("objectName", objectName);
        return result;
    }

    /**
     * 上传单个分片
     * @param uploadId    上传ID
     * @param partNumber  分片编号（从1开始）
     * @param chunkData   分片数据
     * @param partSize    分片大小
     */
    public PartETag uploadChunk(String uploadId, int partNumber, byte[] chunkData, long partSize) {
        Map<String, Object> state = getUploadState(uploadId);
        if (state == null) throw new RuntimeException("上传会话不存在或已过期: " + uploadId);

        String objectName = (String) state.get("objectName");
        return aliOssUtil.uploadPart(objectName, uploadId, partNumber, chunkData, partSize);
    }

    /**
     * 完成分片上传
     * @return 文件URL
     */
    public String completeChunkUpload(String uploadId, List<PartETag> partETags) {
        Map<String, Object> state = getUploadState(uploadId);
        if (state == null) throw new RuntimeException("上传会话不存在或已过期: " + uploadId);

        String objectName = (String) state.get("objectName");
        String url = aliOssUtil.completeMultipartUpload(objectName, uploadId, partETags);
        deleteUploadState(uploadId);
        return url;
    }

    /** 取消分片上传 */
    public void abortChunkUpload(String uploadId) {
        Map<String, Object> state = getUploadState(uploadId);
        if (state != null) {
            aliOssUtil.abortMultipartUpload((String) state.get("objectName"), uploadId);
            deleteUploadState(uploadId);
        }
    }

    /** 获取已上传的分片列表（断点续传） */
    public List<Integer> getUploadedParts(String uploadId) {
        Map<String, Object> state = getUploadState(uploadId);
        if (state == null) return Collections.emptyList();

        String objectName = (String) state.get("objectName");
        PartListing listing = aliOssUtil.listParts(objectName, uploadId);
        return listing.getParts().stream().map(PartSummary::getPartNumber).toList();
    }

    /**
     * 小文件直接上传（<5MB），返回URL
     */
    public String directUpload(MultipartFile file, Integer userId, String folder) throws IOException {
        String objectName = generateObjectName(userId, file.getOriginalFilename(), folder);
        // 直接传 InputStream，AliOssUtil 内部会处理临时文件
        return aliOssUtil.uploadFile(objectName, file.getInputStream());
    }

    /**
     * 大文件分片上传（带进度回调）
     */
    public String uploadLargeFile(MultipartFile mfile, Integer userId, String folder,
                                  Consumer<Integer> progressCallback) throws IOException {
        String objectName = generateObjectName(userId, mfile.getOriginalFilename(), folder);
        File tempFile = File.createTempFile("oss_large_", ".tmp");
        try {
            mfile.transferTo(tempFile);
            long fileSize = tempFile.length();
            // 动态分片大小：<50MB→5MB, 50-200MB→10MB, >200MB→20MB
            long partSize = fileSize < 50 * 1024 * 1024 ? 5 * 1024 * 1024 :
                    fileSize < 200 * 1024 * 1024 ? 10 * 1024 * 1024 : 20 * 1024 * 1024;
            return aliOssUtil.uploadWithCheckpoint(tempFile, objectName, partSize, progressCallback);
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }

    /**
     * 生成签名URL（GET，防盗链）
     */
    public String generateSignedUrl(String objectName, long expireSeconds) {
        return aliOssUtil.generateSignedUrl(objectName, expireSeconds);
    }

    /** 生成 PUT 签名URL（前端直传到OSS，需传入 Content-Type 以通过签名校验） */
    public String generatePutSignedUrl(String objectName, long expireSeconds, String contentType) {
        return aliOssUtil.generatePutSignedUrl(objectName, expireSeconds, contentType);
    }

    /**
     * 从完整URL中提取ObjectName
     */
    public String extractObjectName(String fullUrl) {
        if (fullUrl == null) return null;
        int idx = fullUrl.indexOf(".com/");
        return idx > 0 ? fullUrl.substring(idx + 5) : fullUrl;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? "" : fileName.substring(dot + 1).toLowerCase();
    }
}
