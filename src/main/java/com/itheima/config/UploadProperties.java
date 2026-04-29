// com.itheima.config.UploadProperties.java
package com.itheima.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "upload.daily-limit")
public class UploadProperties {

    // 普通用户每日上传限制
    private Integer normal = 20;

    // VIP用户每日上传限制
    private Integer vipMonthly = 50;
    private Integer vipQuarterly = 100;
    private Integer vipYearly = 200;
    private Integer vipLifetime = -1;
}
