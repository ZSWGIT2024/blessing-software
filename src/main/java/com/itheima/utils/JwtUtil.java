
package com.itheima.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.itheima.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 负责JWT令牌的生成、解析和验证
 *
 */
@Slf4j
@Component
public class JwtUtil {

    // JWT配置属性（通过构造器注入）
    private final JwtProperties jwtProperties;

    /**
     * 构造器注入
     * @param jwtProperties JWT配置属性
     */
    @Autowired
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        log.info("JwtUtil初始化完成，秘钥长度：{}，过期时间：{}ms",
                jwtProperties.getSecret().length(),
                jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 生成JWT令牌
     *
     * @param claims 要存储的业务数据（用户信息）
     * @return JWT令牌字符串
     */
    public String generateToken(Map<String, Object> claims) {
        try {
            // 配置的是毫秒数
            long expireMillis = jwtProperties.getAccessTokenExpiration();
            String token = JWT.create()
                    .withClaim("claims", claims)
                    .withExpiresAt(new Date(System.currentTimeMillis() + expireMillis))
                    .sign(Algorithm.HMAC256(jwtProperties.getSecret()));

            log.debug("生成JWT令牌成功，用户ID：{}", claims.get("id"));
            return token;
        } catch (Exception e) {
            log.error("生成JWT令牌失败", e);
            throw new RuntimeException("生成JWT令牌失败", e);
        }
    }

    /**
     * 解析JWT令牌
     *
     * @param token JWT令牌字符串
     * @return 令牌中的业务数据
     * @throws JWTVerificationException 验证失败时抛出
     * @throws TokenExpiredException 令牌过期时抛出
     */
    public Map<String, Object> parseToken(String token) throws JWTVerificationException, TokenExpiredException {
        try {
            // 移除Bearer前缀（兼容带/不带前缀的token）
            String jwt = removeBearerPrefix(token);

            Map<String, Object> claims = JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                    .build()
                    .verify(jwt)
                    .getClaim("claims")
                    .asMap();

            log.debug("解析JWT令牌成功");
            return claims;
        } catch (TokenExpiredException e) {
            log.warn("JWT令牌已过期：{}", e.getMessage());
            throw e;
        } catch (JWTVerificationException e) {
            log.warn("JWT令牌验证失败：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("解析JWT令牌时发生未知错误", e);
            throw new JWTVerificationException("令牌解析失败");
        }
    }

    /**
     * 从token中获取用户ID
     * 支持 Bearer 前缀格式
     *
     * @param token 可能包含Bearer前缀的令牌字符串
     * @return 用户ID，获取失败返回null
     */
    public Integer getUserIdFromToken(String token) {
        try {
            // 1. 参数校验
            if (token == null || token.trim().isEmpty()) {
                log.warn("获取用户ID失败：token为空");
                return null;
            }

            // 2. 移除 Bearer 前缀（如果存在）
            String jwt = removeBearerPrefix(token);

            // 3. 解析令牌获取claims
            Map<String, Object> claims = parseToken(jwt);

            // 4. 从claims中获取用户ID
            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                userIdObj = claims.get("id");  // 兼容不同字段名
            }

            if (userIdObj == null) {
                log.warn("获取用户ID失败：claims中不存在userId或id字段");
                return null;
            }

            // 5. 转换为Integer类型
            Integer userId;
            if (userIdObj instanceof Integer) {
                userId = (Integer) userIdObj;
            } else if (userIdObj instanceof String) {
                userId = Integer.parseInt((String) userIdObj);
            } else if (userIdObj instanceof Long) {
                userId = ((Long) userIdObj).intValue();
            } else {
                log.warn("获取用户ID失败：userId字段类型错误，实际类型：{}", userIdObj.getClass());
                return null;
            }

            log.debug("从token中获取用户ID成功：{}", userId);
            return userId;

        } catch (TokenExpiredException e) {
            log.warn("获取用户ID失败：令牌已过期");
            return null;
        } catch (JWTVerificationException e) {
            log.warn("获取用户ID失败：令牌验证失败 - {}", e.getMessage());
            return null;
        } catch (NumberFormatException e) {
            log.warn("获取用户ID失败：用户ID格式转换错误 - {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("获取用户ID时发生未知错误", e);
            return null;
        }
    }

    /**
     * 验证token是否有效
     *
     * @param token 可能包含Bearer前缀的令牌字符串
     * @return true=有效，false=无效
     */
    public boolean validateToken(String token) {
        try {
            // 1. 参数校验
            if (token == null || token.trim().isEmpty()) {
                log.warn("验证token失败：token为空");
                return false;
            }

            // 2. 移除Bearer前缀
            String jwt = removeBearerPrefix(token);

            // 3. 解析验证
            parseToken(jwt);

            log.debug("token验证成功");
            return true;

        } catch (TokenExpiredException e) {
            log.warn("token验证失败：令牌已过期");
            return false;
        } catch (JWTVerificationException e) {
            log.warn("token验证失败：{}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("token验证时发生未知错误", e);
            return false;
        }
    }

    /**
     * 从完整token中解析出JWT部分
     * 支持格式：
     * - Bearer xxxxx.yyyyy.zzzzz
     * - xxxxx.yyyyy.zzzzz
     *
     * @param token 原始token字符串
     * @return 纯净的JWT字符串
     */
    private String removeBearerPrefix(String token) {
        if (token == null) return null;

        String trimmed = token.trim();

        // 如果以Bearer开头，移除前缀
        if (trimmed.startsWith("Bearer ")) {
            return trimmed.substring(7);
        }

        return trimmed;
    }

    /**
     * 获取token的剩余有效时间
     *
     * @param token 令牌
     * @return 剩余有效时间（毫秒），-1表示无效或已过期
     */
    public long getRemainingTime(String token) {
        try {
            String jwt = removeBearerPrefix(token);

            Date expiresAt = JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                    .build()
                    .verify(jwt)
                    .getExpiresAt();

            if (expiresAt == null) return -1;

            long remaining = expiresAt.getTime() - System.currentTimeMillis();
            return Math.max(remaining, -1);

        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 刷新token（生成新的）
     *
     * @param oldToken 旧token
     * @return 新的token，如果旧token无效则返回null
     */
    public String refreshToken(String oldToken) {
        try {
            String jwt = removeBearerPrefix(oldToken);
            Map<String, Object> claims = parseToken(jwt);

            // 生成新token
            return generateToken(claims);

        } catch (Exception e) {
            log.warn("刷新token失败：{}", e.getMessage());
            return null;
        }
    }
}











//// com.itheima.utils.JwtUtil.java
//package com.itheima.utils;
//
//import com.auth0.jwt.JWT;
//import com.auth0.jwt.algorithms.Algorithm;
//import com.itheima.config.JwtProperties;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import java.util.Date;
//import java.util.Map;
//
//@Component
//public class JwtUtil {
//
//    // 改为实例字段
//    private final JwtProperties jwtProperties;
//
//    // 通过构造器注入
//    @Autowired
//    public JwtUtil(JwtProperties jwtProperties) {
//        this.jwtProperties = jwtProperties;
//    }
//
//    // 接收业务数据，生成token并返回（非静态方法）
//    public String generateToken(Map<String, Object> claims) {
//        return JWT.create()
//                .withClaim("claims", claims)
//                .withExpiresAt(new Date(System.currentTimeMillis() + jwtProperties.getExpireTime()))
//                .sign(Algorithm.HMAC256(jwtProperties.getSecret()));
//    }
//
//    // 接收token，验证token，并返回业务数据（非静态方法）
//    public Map<String, Object> parseToken(String token) {
//        return JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
//                .build()
//                .verify(token)
//                .getClaim("claims")
//                .asMap();
//    }
//
//    /**
//     * 从token中获取用户ID
//     */
//    public Integer getUserIdFromToken(String token) {
//        if (token == null || !token.startsWith("Bearer ")) {
//            return null;
//        }
//
//        try {
//            String jwt = token.substring(7); // 去掉 "Bearer "
//            Map<String, Object> claims = parseToken(jwt);
//            return (Integer) claims.get("userId");
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    /**
//     * 验证token是否有效
//     */
//    public boolean validateToken(String token) {
//        try {
//            if (token == null || !token.startsWith("Bearer ")) {
//                return false;
//            }
//
//            String jwt = token.substring(7);
//            parseToken(jwt);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//}