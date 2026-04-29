package com.itheima.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    private String endpoint;
    private String accessKeyId;      // 配置文件写 access-key-id 即可
    private String accessKeySecret;  // 配置文件写 access-key-secret 即可
    private String bucketName;       // 配置文件写 bucket-name 即可

    /**
     * 获取OSS文件访问URL
     */
    public String getFileUrl(String filePath) {
        return String.format("https://%s.%s/%s",
                bucketName, endpoint, filePath);
    }
}
