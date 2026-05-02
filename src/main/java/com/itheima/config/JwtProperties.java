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
     *  30分钟（毫秒）
     */
    @Value("${security.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    /**
     * JWT刷新过期时间（毫秒）
     *  7天（毫秒）
     */
    @Value("${security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

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

}
