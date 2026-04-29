// com.itheima.config.JwtProperties.java
package com.itheima.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * JWT签名秘钥
     */
    @Value("${security.jwt.secret}")
    private String secret;

    /**
     * JWT过期时间（毫秒）
     * 默认7天 = 7 * 24 * 60 * 60 * 1000 = 604800000
     */
    @Value("${security.jwt.expire-time}")
    private long expireTime;

    /**
     * token在请求头中的名称
     */
    @Value("${security.jwt.token-header}")
    private String tokenHeader;

    /**
     * token前缀
     */
    @Value("${security.jwt.token-prefix}")
    private String tokenPrefix;

    /**
     * 获取过期时间（毫秒）
     */
    public Date getExpireDate() {
        return new Date(System.currentTimeMillis() + expireTime * 24 * 60 * 60 * 1000);
    }
}
