package com.itheima.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.model.*;
import com.itheima.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class AliOssUtil {

    private final OssProperties ossProperties;

    @Autowired
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
     * 上传文件（接收InputStream参数）- 兼容原有调用
     */
    public String uploadFile(String objectName, InputStream inputStream) {
        log.info("开始上传流到OSS，对象名: {}", objectName);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("oss_temp_", ".tmp");

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                log.info("输入流已保存到临时文件: {} (大小: {} bytes)",
                        tempFile.getAbsolutePath(), totalBytes);
            }

            return uploadFile(objectName, tempFile);

        } catch (Exception e) {
            log.error("处理输入流失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                    log.debug("临时文件已清理");
                } catch (IOException e) {
                    log.warn("清理临时文件失败", e);
                }
            }
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