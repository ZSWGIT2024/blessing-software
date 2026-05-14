package com.itheima.controller;

import com.aliyun.oss.common.utils.DateUtil;
import com.itheima.config.OssProperties;
import com.itheima.pojo.Result;
import com.itheima.service.impl.OssUploadService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 分片上传控制器
 * <p>
 * 提供前端分片上传 REST API：
 * init → chunk (×N) → complete（大文件）
 * 或直接 POST /oss/upload/direct（小文件 <5MB）
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/oss/upload")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final OssUploadService ossUploadService;
    private final OssProperties ossProperties;
    private final com.itheima.service.DailyUploadService dailyUploadService;

    /** 获取当天上传信息 */
    @GetMapping("/daily-info")
    public Result<Map<String, Object>> getDailyUploadInfo() {
        Integer userId = getUserId();
        int used = dailyUploadService.getTodayUploadCount(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("todayUsed", used);
        return Result.success(data);
    }

    /** 获取当前用户ID */
    private Integer getUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("id");
    }

    /**
     * 初始化分片上传
     * POST /api/oss/upload/init
     * Body: { fileName, fileSize, folder }
     */
    @PostMapping("/init")
    public Result<Map<String, String>> initChunk(
            @RequestBody Map<String, Object> params) {
        Integer userId = getUserId();
        String fileName = (String) params.get("fileName");
        long fileSize = ((Number) params.get("fileSize")).longValue();
        String folder = (String) params.getOrDefault("folder", "media");

        Map<String, String> data = ossUploadService.initChunkUpload(
                userId, fileName, fileSize, folder);
        return Result.success(data);
    }

    /**
     * 上传单个分片
     * POST /api/oss/upload/chunk
     * FormData: uploadId, partNumber, chunk (file part)
     */
    @PostMapping("/chunk")
    public Result<Map<String, Object>> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber,
            @RequestParam("chunk") MultipartFile chunk) {
        try {
            byte[] data = chunk.getBytes();
            long partSize = chunk.getSize();
            ossUploadService.uploadChunk(uploadId, partNumber, data, partSize);

            Map<String, Object> result = new HashMap<>();
            result.put("partNumber", partNumber);
            result.put("uploaded", true);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分片上传失败: uploadId={}, part={}", uploadId, partNumber, e);
            return Result.error("分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 完成分片上传
     * POST /api/oss/upload/complete
     * Body: { uploadId, partETags: [{partNumber, eTag}] }
     */
    @PostMapping("/complete")
    public Result<Map<String, Object>> completeChunk(
            @RequestBody Map<String, Object> params) {
        try {
            String uploadId = (String) params.get("uploadId");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tagsList = (List<Map<String, Object>>) params.get("partETags");

            List<com.aliyun.oss.model.PartETag> partETags = new ArrayList<>();
            for (Map<String, Object> tag : tagsList) {
                partETags.add(new com.aliyun.oss.model.PartETag(
                        ((Number) tag.get("partNumber")).intValue(),
                        (String) tag.get("eTag")));
            }

            String fileUrl = ossUploadService.completeChunkUpload(uploadId, partETags);

            // 生成签名URL (1小时有效期)
            String objectName = ossUploadService.extractObjectName(fileUrl);
            String signedUrl = ossUploadService.generateSignedUrl(objectName, 3600);

            Map<String, Object> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("signedUrl", signedUrl);
            return Result.success(result);
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            return Result.error("完成上传失败: " + e.getMessage());
        }
    }

    /**
     * 取消/中止分片上传
     * POST /api/oss/upload/abort
     * Body: { uploadId }
     */
    @PostMapping("/abort")
    public Result<Void> abortUpload(@RequestBody Map<String, String> params) {
        String uploadId = params.get("uploadId");
        ossUploadService.abortChunkUpload(uploadId);
        return Result.success();
    }

    /**
     * 查询已上传的分片（断点续传用）
     * GET /api/oss/upload/progress/{uploadId}
     */
    @GetMapping("/progress/{uploadId}")
    public Result<Map<String, Object>> getProgress(@PathVariable String uploadId) {
        List<Integer> parts = ossUploadService.getUploadedParts(uploadId);
        Map<String, Object> data = new HashMap<>();
        data.put("uploadId", uploadId);
        data.put("uploadedParts", parts);
        return Result.success(data);
    }

    /**
     * 生成 OSS 直传签名策略（前端直接上传到OSS，绕过后端，速度最快）
     * POST /api/oss/upload/signature
     * Body: { fileName, fileSize, folder }
     */
    @PostMapping("/signature")
    public Result<Map<String, Object>> generateUploadSignature(
            @RequestBody Map<String, Object> params) {
        try {
            Integer userId = getUserId();
            String fileName = (String) params.get("fileName");
            String folder = (String) params.getOrDefault("folder", "media");
            long fileSize = params.get("fileSize") != null
                    ? ((Number) params.get("fileSize")).longValue() : 0;

            String objectName = ossUploadService.generateObjectName(userId, fileName, folder);
            long expireSeconds = 300; // 5 minutes to start upload
            String contentType = (String) params.getOrDefault("contentType", "application/octet-stream");
            String signedUrl = ossUploadService.generatePutSignedUrl(objectName, expireSeconds, contentType);

            // Build policy for direct POST upload
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
            String isoExpiration = DateUtil.formatIso8601Date(expiration);

            Map<String, Object> data = new HashMap<>();
            data.put("host", "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint());
            data.put("objectName", objectName);
            data.put("signedUrl", signedUrl);
            data.put("expireTime", isoExpiration);
            data.put("expireSeconds", expireSeconds);
            return Result.success(data);
        } catch (Exception e) {
            log.error("生成上传签名失败", e);
            return Result.error("生成签名失败: " + e.getMessage());
        }
    }

    /**
     * 小文件直接上传（<5MB，无需分片）
     * POST /api/oss/upload/direct
     */
    @PostMapping("/direct")
    public Result<Map<String, Object>> directUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "media") String folder) {
        try {
            Integer userId = getUserId();
            String fileUrl = ossUploadService.directUpload(file, userId, folder);
            String objectName = ossUploadService.extractObjectName(fileUrl);

            Map<String, Object> data = new HashMap<>();
            data.put("fileUrl", fileUrl);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileSize", file.getSize());
            data.put("objectName", objectName);
            return Result.success(data);
        } catch (Exception e) {
            log.error("直接上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
