package com.itheima.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.itheima.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT双Token服务
 * 实现Access Token + Refresh Token机制
 *
 * Access Token：短期有效（30分钟），用于API调用
 * Refresh Token：长期有效（7天），用于刷新Access Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key前缀
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "token:refresh:";
    private static final String TOKEN_VERSION_PREFIX = "token:version:";

    /**
     * 生成Access Token
     * @param claims 业务数据
     * @return Access Token
     */
    public String generateAccessToken(Map<String, Object> claims) {
        return generateToken(claims, jwtProperties.getAccessTokenExpiration(), "access");
    }

    /**
     * 生成Refresh Token
     * @param claims 业务数据
     * @return Refresh Token
     */
    public String generateRefreshToken(Map<String, Object> claims) {
        String refreshToken = generateToken(claims, jwtProperties.getRefreshTokenExpiration(), "refresh");

        // 将Refresh Token存储到Redis
        Integer userId = (Integer) claims.get("id");
        if (userId != null) {
            String key = REFRESH_TOKEN_PREFIX + userId;
            redisTemplate.opsForValue().set(key, refreshToken,
                    jwtProperties.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);
            log.debug("Refresh Token已存储到Redis: userId={}", userId);
        }

        return refreshToken;
    }

    /**
     * 生成Token
     * @param claims 业务数据
     * @param expireMillis 过期时间（毫秒）
     * @param tokenType token类型（access/refresh）
     * @return Token字符串
     */
    private String generateToken(Map<String, Object> claims, long expireMillis, String tokenType) {
        try {
            Date now = new Date();
            Date expireDate = new Date(now.getTime() + expireMillis);

            String token = JWT.create()
                    .withClaim("claims", claims)
                    .withClaim("type", tokenType)
                    .withIssuedAt(now)
                    .withExpiresAt(expireDate)
                    .sign(Algorithm.HMAC256(jwtProperties.getSecret()));

            log.debug("生成{} Token成功，过期时间：{}", tokenType, expireDate);
            return token;

        } catch (Exception e) {
            log.error("生成Token失败", e);
            throw new RuntimeException("生成Token失败", e);
        }
    }

    /**
     * 解析Token
     * @param token JWT Token
     * @return 业务数据
     */
    public Map<String, Object> parseToken(String token) {
        try {
            // 移除Bearer前缀
            String jwt = removeBearerPrefix(token);

            return JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                    .build()
                    .verify(jwt)
                    .getClaim("claims")
                    .asMap();

        } catch (TokenExpiredException e) {
            log.warn("Token已过期");
            throw e;
        } catch (JWTVerificationException e) {
            log.warn("Token验证失败：{}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证Token有效性
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            String jwt = removeBearerPrefix(token);

            // 1. 检查是否在黑名单中
            if (isBlacklisted(jwt)) {
                log.warn("Token在黑名单中");
                return false;
            }

            // 2. 验证Token签名和过期时间
            Map<String, Object> claims = parseToken(jwt);

            // 3. 检查Token版本（用于强制登出）
            Integer userId = (Integer) claims.get("id");
            Integer tokenVersion = (Integer) claims.get("version");
            if (userId != null && tokenVersion != null) {
                if (!validateTokenVersion(userId, tokenVersion)) {
                    log.warn("Token版本不匹配，可能已被强制登出");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.debug("Token验证失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 刷新Access Token
     * @param refreshToken Refresh Token
     * @return 新的Access Token，失败返回null
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            String jwt = removeBearerPrefix(refreshToken);

            // 1. 验证Refresh Token
            if (!validateRefreshToken(jwt)) {
                log.warn("Refresh Token无效");
                return null;
            }

            // 2. 解析Refresh Token获取用户信息
            Map<String, Object> claims = parseToken(jwt);
            Integer userId = (Integer) claims.get("id");

            if (userId == null) {
                log.warn("Refresh Token中缺少用户ID");
                return null;
            }

            // 3. 检查Redis中的Refresh Token是否匹配
            String storedRefreshToken = (String) redisTemplate.opsForValue()
                    .get(REFRESH_TOKEN_PREFIX + userId);

            if (storedRefreshToken == null || !storedRefreshToken.equals(jwt)) {
                log.warn("Refresh Token不匹配或已过期");
                return null;
            }

            // 4. 生成新的Access Token
            String newAccessToken = generateAccessToken(claims);
            log.info("刷新Access Token成功：userId={}", userId);

            return newAccessToken;

        } catch (Exception e) {
            log.error("刷新Access Token失败", e);
            return null;
        }
    }

    /**
     * 验证Refresh Token
     */
    private boolean validateRefreshToken(String refreshToken) {
        try {
            Map<String, Object> claims = JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                    .build()
                    .verify(refreshToken)
                    .getClaim("claims")
                    .asMap();

            String type = (String) JWT.decode(refreshToken).getClaim("type").asString();
            return "refresh".equals(type);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将Token加入黑名单（登出时使用）
     * @param token JWT Token
     */
    public void addToBlacklist(String token) {
        try {
            String jwt = removeBearerPrefix(token);

            // 计算Token剩余有效时间
            Date expiresAt = JWT.decode(jwt).getExpiresAt();
            if (expiresAt != null) {
                long ttl = expiresAt.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    String key = TOKEN_BLACKLIST_PREFIX + jwt;
                    redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.MILLISECONDS);
                    log.info("Token已加入黑名单，TTL：{}ms", ttl);
                }
            }

        } catch (Exception e) {
            log.error("添加Token到黑名单失败", e);
        }
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String jwt = removeBearerPrefix(token);
        String key = TOKEN_BLACKLIST_PREFIX + jwt;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 更新用户Token版本（用于强制登出所有设备）
     * @param userId 用户ID
     * @return 新的版本号
     */
    public Long incrementTokenVersion(Integer userId) {
        String key = TOKEN_VERSION_PREFIX + userId;
        Long version = redisTemplate.opsForValue().increment(key);
        log.info("更新用户Token版本：userId={}, version={}", userId, version);
        return version;
    }

    /**
     * 获取用户当前Token版本
     */
    public Long getTokenVersion(Integer userId) {
        String key = TOKEN_VERSION_PREFIX + userId;
        Object version = redisTemplate.opsForValue().get(key);
        return version != null ? Long.parseLong(version.toString()) : 0L;
    }

    /**
     * 验证Token版本
     */
    private boolean validateTokenVersion(Integer userId, Integer tokenVersion) {
        Long currentVersion = getTokenVersion(userId);
        return currentVersion.equals(tokenVersion.longValue());
    }

    /**
     * 撤销Refresh Token（登出时使用）
     * @param userId 用户ID
     */
    public void revokeRefreshToken(Integer userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("已撤销用户的Refresh Token：userId={}", userId);
    }

    /**
     * 移除Bearer前缀
     */
    private String removeBearerPrefix(String token) {
        if (token == null) return null;
        String trimmed = token.trim();
        if (trimmed.startsWith("Bearer ")) {
            return trimmed.substring(7);
        }
        return trimmed;
    }

    /**
     * 获取Token剩余有效时间
     */
    public long getRemainingTime(String token) {
        try {
            String jwt = removeBearerPrefix(token);
            Date expiresAt = JWT.decode(jwt).getExpiresAt();
            if (expiresAt == null) return -1;
            long remaining = expiresAt.getTime() - System.currentTimeMillis();
            return Math.max(remaining, -1);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 判断Token是否即将过期（5分钟内）
     */
    public boolean isTokenExpiringSoon(String token) {
        long remaining = getRemainingTime(token);
        return remaining > 0 && remaining < 5 * 60 * 1000;
    }
}
