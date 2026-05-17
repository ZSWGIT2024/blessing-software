package com.itheima.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.model.*;
import com.itheima.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Component
public class AliOssUtil {

    @Autowired
    private OssProperties ossProperties;

    public AliOssUtil(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
        log.info("AliOssUtil初始化完成，Endpoint: {}, Bucket: {}",
                ossProperties.getEndpoint(), ossProperties.getBucketName());
    }

    /**
     * 获取OSS客户端
     */
    private OSS createOssClient() {
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setConnectionTimeout(10000);
        conf.setSocketTimeout(60000);
        conf.setMaxErrorRetry(3);

        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret(),
                conf);
    }

    /**
     * 获取Bucket名称
     */
    private String getBucketName() {
        return ossProperties.getBucketName();
    }

    /**
     * 获取完整URL
     */
    private String getFileUrl(String objectName) {
        return ossProperties.getFileUrl(objectName);
    }

    /**
     * 上传文件（接收File参数）- 推荐用于视频文件
     */
    public String uploadFile(String objectName, File file) {
        log.info("开始上传文件到OSS，对象名: {}, 文件大小: {} bytes",
                objectName, file.length());

        OSS ossClient = createOssClient();

        try {
            // 检查Bucket是否存在
            boolean bucketExists = ossClient.doesBucketExist(getBucketName());
            if (!bucketExists) {
                throw new RuntimeException("OSS Bucket不存在: " + getBucketName());
            }

            // 创建元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(objectName));
            metadata.setContentLength(file.length());
            metadata.setCacheControl("max-age=31536000");

            log.info("开始上传...");
            long startTime = System.currentTimeMillis();

            // 使用File直接上传
            PutObjectRequest putObjectRequest = new PutObjectRequest(getBucketName(), objectName, file);
            putObjectRequest.setMetadata(metadata);

            PutObjectResult result = ossClient.putObject(putObjectRequest);
            long endTime = System.currentTimeMillis();
            long costTime = (endTime - startTime) / 1000;

            String url = getFileUrl(objectName);

            log.info("文件上传成功，耗时: {}s, ETag: {}, URL: {}",
                    costTime, result.getETag(), url);

            return url;

        } catch (OSSException oe) {
            log.error("OSS错误: {} (请求ID: {})", oe.getErrorMessage(), oe.getRequestId());
            throw new RuntimeException("OSS上传失败: " + oe.getErrorMessage());
        } catch (Exception e) {
            log.error("上传异常", e);
            throw new RuntimeException("上传失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 上传文件（接收InputStream参数）- 直接流式上传到OSS，不落盘
     */
    public String uploadFile(String objectName, InputStream inputStream) {
        log.info("开始流式上传到OSS，对象名: {}", objectName);
        OSS ossClient = createOssClient();
        try {
            boolean bucketExists = ossClient.doesBucketExist(getBucketName());
            if (!bucketExists) {
                throw new RuntimeException("OSS Bucket不存在: " + getBucketName());
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(objectName));
            metadata.setCacheControl("max-age=31536000");

            long startTime = System.currentTimeMillis();
            PutObjectRequest request = new PutObjectRequest(getBucketName(), objectName, inputStream, metadata);
            PutObjectResult result = ossClient.putObject(request);
            long costTime = (System.currentTimeMillis() - startTime) / 1000;

            String url = getFileUrl(objectName);
            log.info("文件流式上传成功，耗时: {}s, ETag: {}, URL: {}", costTime, result.getETag(), url);
            return url;

        } catch (OSSException oe) {
            log.error("OSS错误: {} (请求ID: {})", oe.getErrorMessage(), oe.getRequestId());
            throw new RuntimeException("OSS上传失败: " + oe.getErrorMessage());
        } catch (Exception e) {
            log.error("上传异常", e);
            throw new RuntimeException("上传失败: " + e.getMessage());
        } finally {
            if (ossClient != null) ossClient.shutdown();
        }
    }

    /**
     * 优化分片上传大文件
     */
    public String uploadLargeFileOptimized(String objectName, File file, long partSize) {
        log.info("开始优化分片上传: {}, 大小: {} MB, 分片大小: {} MB",
                objectName,
                String.format("%.2f", file.length() / (1024.0 * 1024)),
                String.format("%.2f", partSize / (1024.0 * 1024)));

        // 配置更优的OSS客户端
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setConnectionTimeout(100000);
        conf.setSocketTimeout(300000);
        conf.setMaxErrorRetry(3);
        conf.setMaxConnections(200);

        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret(),
                conf);

        String uploadId = null;
        try {
            // 1. 初始化分片上传
            InitiateMultipartUploadRequest initRequest =
                    new InitiateMultipartUploadRequest(getBucketName(), objectName);
            uploadId = ossClient.initiateMultipartUpload(initRequest).getUploadId();

            // 2. 计算分片
            long fileLength = file.length();
            int partCount = (int) (fileLength / partSize);
            if (fileLength % partSize != 0) {
                partCount++;
            }

            log.info("总分片数: {}, 并行上传...", partCount);

            // 3. 并行上传分片（使用线程池）
            List<PartETag> partETags = Collections.synchronizedList(new ArrayList<>());
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(partCount, 5));

            List<CompletableFuture<PartETag>> futures = new ArrayList<>();

            for (int i = 0; i < partCount; i++) {
                final int partNumber = i + 1;
                final long startPos = i * partSize;
                final long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;

                String finalUploadId = uploadId;
                CompletableFuture<PartETag> future = CompletableFuture.supplyAsync(() -> {
                    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                        raf.seek(startPos);
                        byte[] buffer = new byte[(int) curPartSize];
                        raf.readFully(buffer);

                        ByteArrayInputStream partStream = new ByteArrayInputStream(buffer);

                        UploadPartRequest uploadPartRequest = new UploadPartRequest();
                        uploadPartRequest.setBucketName(getBucketName());
                        uploadPartRequest.setKey(objectName);
                        uploadPartRequest.setUploadId(finalUploadId);
                        uploadPartRequest.setInputStream(partStream);
                        uploadPartRequest.setPartSize(curPartSize);
                        uploadPartRequest.setPartNumber(partNumber);

                        UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                        log.debug("分片 {} 上传完成", partNumber);

                        return uploadPartResult.getPartETag();

                    } catch (Exception e) {
                        log.error("分片 {} 上传失败", partNumber, e);
                        throw new CompletionException(e);
                    }
                }, executor);

                futures.add(future);
            }

            // 等待所有分片完成
            for (int i = 0; i < futures.size(); i++) {
                try {
                    int retryCount = 0;
                    while (retryCount < 3) {
                        try {
                            PartETag partETag = futures.get(i).get(600, TimeUnit.SECONDS);
                            partETags.add(partETag);
                            break;
                        } catch (TimeoutException e) {
                            retryCount++;
                            if (retryCount >= 3) {
                                throw e;
                            }
                            log.warn("分片 {} 上传超时，正在进行第 {} 次重试", i + 1, retryCount);
                        }
                    }
                } catch (Exception e) {
                    log.error("获取分片 {} 结果失败", i + 1, e);
                    executor.shutdownNow();
                    throw new RuntimeException("分片上传失败", e);
                }
            }

            executor.shutdown();

            // 4. 完成上传
            partETags.sort(Comparator.comparingInt(PartETag::getPartNumber));

            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                    getBucketName(), objectName, uploadId, partETags);

            ossClient.completeMultipartUpload(completeRequest);

            String url = getFileUrl(objectName);
            log.info("分片上传完成，总分片数: {}", partETags.size());

            return url;

        } catch (Exception e) {
            log.error("分片上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    // ==================== 分片上传 API（供 ChunkUploadController 调用） ====================

    /** 初始化分片上传，返回 uploadId */
    public String initMultipartUpload(String objectName) {
        OSS ossClient = createOssClient();
        try {
            InitiateMultipartUploadRequest request =
                    new InitiateMultipartUploadRequest(getBucketName(), objectName);
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            log.info("初始化分片上传: objectName={}, uploadId={}", objectName, result.getUploadId());
            return result.getUploadId();
        } finally {
            ossClient.shutdown();
        }
    }

    /** 上传单个分片 */
    public PartETag uploadPart(String objectName, String uploadId, int partNumber,
                               byte[] partData, long partSize) {
        OSS ossClient = createOssClient();
        try {
            UploadPartRequest request = new UploadPartRequest();
            request.setBucketName(getBucketName());
            request.setKey(objectName);
            request.setUploadId(uploadId);
            request.setInputStream(new ByteArrayInputStream(partData));
            request.setPartSize(partSize);
            request.setPartNumber(partNumber);

            UploadPartResult result = ossClient.uploadPart(request);
            log.debug("分片上传完成: partNumber={}, eTag={}", partNumber, result.getPartETag().getETag());
            return result.getPartETag();
        } finally {
            ossClient.shutdown();
        }
    }

    /** 完成分片上传 */
    public String completeMultipartUpload(String objectName, String uploadId,
                                          List<PartETag> partETags) {
        OSS ossClient = createOssClient();
        try {
            partETags.sort(Comparator.comparingInt(PartETag::getPartNumber));
            CompleteMultipartUploadRequest request =
                    new CompleteMultipartUploadRequest(getBucketName(), objectName, uploadId, partETags);
            CompleteMultipartUploadResult result = ossClient.completeMultipartUpload(request);
            String url = getFileUrl(objectName);
            log.info("分片上传完成: objectName={}, eTag={}", objectName, result.getETag());
            return url;
        } finally {
            ossClient.shutdown();
        }
    }

    /** 取消分片上传 */
    public void abortMultipartUpload(String objectName, String uploadId) {
        OSS ossClient = createOssClient();
        try {
            AbortMultipartUploadRequest request =
                    new AbortMultipartUploadRequest(getBucketName(), objectName, uploadId);
            ossClient.abortMultipartUpload(request);
            log.info("分片上传已取消: objectName={}, uploadId={}", objectName, uploadId);
        } finally {
            ossClient.shutdown();
        }
    }

    /** 列出已上传的分片（断点续传用） */
    public PartListing listParts(String objectName, String uploadId) {
        OSS ossClient = createOssClient();
        try {
            ListPartsRequest request = new ListPartsRequest(getBucketName(), objectName, uploadId);
            return ossClient.listParts(request);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 带断点续传的分片上传（大文件推荐）
     * <p>
     * 使用 OSS SDK 原生的 UploadFileRequest + Checkpoint 机制，
     * 自动记录上传进度到本地 checkpoint 文件，中断后可以从断点继续。
     * </p>
     *
     * @param file            待上传文件
     * @param objectName      OSS 对象名
     * @param partSize        分片大小（如 10MB）
     * @param progressCallback 进度回调 (percent 0-100)
     * @return 文件URL
     */
    public String uploadWithCheckpoint(File file, String objectName, long partSize,
                                       Consumer<Integer> progressCallback) {
        OSS ossClient = createOssClient();
        try {
            String checkpointFile = file.getParent() + "/" + file.getName() + ".checkpoint";

            UploadFileRequest uploadFileRequest = new UploadFileRequest(
                    getBucketName(), objectName);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setPartSize(partSize);
            uploadFileRequest.setTaskNum(5);
            uploadFileRequest.setEnableCheckpoint(true);
            uploadFileRequest.setCheckpointFile(checkpointFile);
            uploadFileRequest.setProgressListener(event -> {
                if (progressCallback != null) {
                    long total = event.getBytes();
                    // ProgressEvent.getBytes() returns cumulative bytes uploaded
                    if (total > 0) {
                        long fileSize = file.length();
                        int pct = (int) (total * 100 / fileSize);
                        progressCallback.accept(pct);
                    }
                }
            });

            ossClient.uploadFile(uploadFileRequest);

            // 清理 checkpoint 文件
            File cpFile = new File(checkpointFile);
            if (cpFile.exists()) cpFile.delete();

            String url = getFileUrl(objectName);
            log.info("断点续传上传完成: objectName={}, size={}MB",
                    objectName, String.format("%.2f", file.length() / (1024.0 * 1024)));
            return url;

        } catch (Throwable e) {
            log.error("断点续传上传失败: {}", objectName, e);
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }
    }

    // ==================== 签名URL与防盗链 ====================

    /**
     * 生成带签名的临时访问URL（防盗链）
     *
     * @param objectName       OSS对象名
     * @param expirationSeconds 过期时间（秒），建议聊天文件设为3600（1小时）
     * @return 带签名的临时URL
     */
    public String generateSignedUrl(String objectName, long expirationSeconds) {
        return generateSignedUrlWithMethod(objectName, expirationSeconds, com.aliyun.oss.HttpMethod.GET);
    }

    /** 生成 PUT 方法的签名URL（用于前端直传到OSS，需传入 Content-Type 以通过签名校验） */
    public String generatePutSignedUrl(String objectName, long expirationSeconds, String contentType) {
        return generateSignedUrlWithMethod(objectName, expirationSeconds, com.aliyun.oss.HttpMethod.PUT, contentType);
    }

    private String generateSignedUrlWithMethod(String objectName, long expirationSeconds,
                                                com.aliyun.oss.HttpMethod method) {
        return generateSignedUrlWithMethod(objectName, expirationSeconds, method, null);
    }

    private String generateSignedUrlWithMethod(String objectName, long expirationSeconds,
                                                com.aliyun.oss.HttpMethod method, String contentType) {
        OSS ossClient = createOssClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + expirationSeconds * 1000);
            GeneratePresignedUrlRequest request =
                    new GeneratePresignedUrlRequest(getBucketName(), objectName);
            request.setExpiration(expiration);
            request.setMethod(method);
            if (contentType != null && !contentType.isEmpty()) {
                request.setContentType(contentType);
            }

            URL signedUrl = ossClient.generatePresignedUrl(request);
            return signedUrl.toString();
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 设置 Bucket Referer 防盗链白名单
     * <p>
     * 注意：OSS Referer 配置是 Bucket 级别的全局设置，
     * 建议在应用启动时调用一次即可，不要每次上传都调用。
     * </p>
     *
     * @param allowRefererList 允许的 Referer 列表（如 {"https://example.com", "http://localhost:*"}）
     * @param allowEmptyReferer 是否允许空 Referer（如浏览器直接访问、curl 等）
     */
    public void setBucketReferer(List<String> allowRefererList, boolean allowEmptyReferer) {
        OSS ossClient = createOssClient();
        try {
            BucketReferer referer = new BucketReferer(allowEmptyReferer, allowRefererList);
            ossClient.setBucketReferer(getBucketName(), referer);
            log.info("Bucket防盗链已设置: bucket={}, allowList={}, allowEmpty={}",
                    getBucketName(), allowRefererList, allowEmptyReferer);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 根据文件名获取Content-Type
     */
    private String getContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") ||
                fileName.endsWith(".mov") || fileName.endsWith(".mkv")) {
            return "video/mp4";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }
}