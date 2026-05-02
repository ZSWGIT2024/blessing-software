package com.itheima.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录安全服务
 * 提供登录失败限制、账户锁定等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis Key前缀
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String ACCOUNT_LOCK_PREFIX = "login:lock:";
    private static final String IP_FAIL_PREFIX = "login:ip_fail:";
    private static final String IP_LOCK_PREFIX = "login:ip_lock:";

    // 配置参数
    public static final int MAX_FAIL_COUNT = 5;           // 最大失败次数
    public static final long LOCK_DURATION_MINUTES = 30;  // 锁定时长（分钟）
    private static final int MAX_IP_FAIL_COUNT = 20;       // 单IP最大失败次数
    private static final long IP_LOCK_DURATION_MINUTES = 60; // IP锁定时长（分钟）

    /**
     * 记录登录失败
     * @param identifier 用户标识（手机号/用户名）
     * @param ip 客户端IP
     * @return 当前失败次数
     */
    public int recordLoginFailure(String identifier, String ip) {
        // 1. 记录账户失败次数
        String failKey = LOGIN_FAIL_PREFIX + identifier;
        Long failCount = redisTemplate.opsForValue().increment(failKey);

        if (failCount == 1) {
            // 首次失败，设置过期时间（24小时）
            redisTemplate.expire(failKey, 24, TimeUnit.HOURS);
        }

        // 2. 记录IP失败次数
        String ipFailKey = IP_FAIL_PREFIX + ip;
        Long ipFailCount = redisTemplate.opsForValue().increment(ipFailKey);
        if (ipFailCount == 1) {
            redisTemplate.expire(ipFailKey, 1, TimeUnit.HOURS);
        }

        // 3. 检查是否需要锁定
        if (failCount != null && failCount >= MAX_FAIL_COUNT) {
            lockAccount(identifier);
            log.warn("账户已被锁定：identifier={}, failCount={}", identifier, failCount);
        }

        // 4. 检查是否需要锁定IP
        if (ipFailCount != null && ipFailCount >= MAX_IP_FAIL_COUNT) {
            lockIp(ip);
            log.warn("IP已被锁定：ip={}, failCount={}", ip, ipFailCount);
        }

        log.info("登录失败记录：identifier={}, ip={}, failCount={}", identifier, ip, failCount);
        return failCount != null ? failCount.intValue() : 0;
    }

    /**
     * 记录登录成功，清除失败记录
     * @param identifier 用户标识
     * @param ip 客户端IP
     */
    public void recordLoginSuccess(String identifier, String ip) {
        // 清除账户失败记录
        String failKey = LOGIN_FAIL_PREFIX + identifier;
        redisTemplate.delete(failKey);

        // 清除账户锁定
        String lockKey = ACCOUNT_LOCK_PREFIX + identifier;
        redisTemplate.delete(lockKey);

        log.info("登录成功，已清除失败记录：identifier={}", identifier);
    }

    /**
     * 检查账户是否被锁定
     * @param identifier 用户标识
     * @return 是否被锁定
     */
    public boolean isAccountLocked(String identifier) {
        String lockKey = ACCOUNT_LOCK_PREFIX + identifier;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * 检查IP是否被锁定
     * @param ip 客户端IP
     * @return 是否被锁定
     */
    public boolean isIpLocked(String ip) {
        String lockKey = IP_LOCK_PREFIX + ip;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * 获取账户剩余锁定时间（分钟）
     * @param identifier 用户标识
     * @return 剩余锁定时间，未锁定返回0
     */
    public long getRemainingLockTime(String identifier) {
        String lockKey = ACCOUNT_LOCK_PREFIX + identifier;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * 获取登录失败次数
     * @param identifier 用户标识
     * @return 失败次数
     */
    public int getFailedAttempts(String identifier) {
        String failKey = LOGIN_FAIL_PREFIX + identifier;
        Object count = redisTemplate.opsForValue().get(failKey);
        return count != null ? Integer.parseInt(count.toString()) : 0;
    }

    /**
     * 手动解锁账户
     * @param identifier 用户标识
     */
    public void unlockAccount(String identifier) {
        String lockKey = ACCOUNT_LOCK_PREFIX + identifier;
        String failKey = LOGIN_FAIL_PREFIX + identifier;
        redisTemplate.delete(lockKey);
        redisTemplate.delete(failKey);
        log.info("账户已解锁：identifier={}", identifier);
    }

    /**
     * 手动锁定账户
     * @param identifier 用户标识
     */
    public void lockAccount(String identifier) {
        String lockKey = ACCOUNT_LOCK_PREFIX + identifier;
        redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 锁定IP
     * @param ip 客户端IP
     */
    private void lockIp(String ip) {
        String lockKey = IP_LOCK_PREFIX + ip;
        redisTemplate.opsForValue().set(lockKey, "1", IP_LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 检查是否可以尝试登录
     * @param identifier 用户标识
     * @param ip 客户端IP
     * @return 是否可以尝试
     */
    public LoginCheckResult checkLoginAttempt(String identifier, String ip) {
        // 1. 检查IP是否被锁定
        if (isIpLocked(ip)) {
            return LoginCheckResult.ipLocked();
        }

        // 2. 检查账户是否被锁定
        if (isAccountLocked(identifier)) {
            long remainingTime = getRemainingLockTime(identifier);
            return LoginCheckResult.accountLocked(remainingTime);
        }

        // 3. 检查失败次数，给出警告
        int failedAttempts = getFailedAttempts(identifier);
        if (failedAttempts >= 3) {
            return LoginCheckResult.warning(failedAttempts);
        }

        return LoginCheckResult.allowed();
    }

    /**
     * 登录检查结果
     */
    public static class LoginCheckResult {
        private final boolean allowed;
        private final String message;
        private final int remainingAttempts;
        private final long lockRemainingMinutes;

        private LoginCheckResult(boolean allowed, String message, int remainingAttempts, long lockRemainingMinutes) {
            this.allowed = allowed;
            this.message = message;
            this.remainingAttempts = remainingAttempts;
            this.lockRemainingMinutes = lockRemainingMinutes;
        }

        public static LoginCheckResult allowed() {
            return new LoginCheckResult(true, "允许登录", 0, 0);
        }

        public static LoginCheckResult warning(int failedAttempts) {
            int remaining = MAX_FAIL_COUNT - failedAttempts;
            return new LoginCheckResult(true,
                    String.format("警告：还有%d次尝试机会", remaining), remaining, 0);
        }

        public static LoginCheckResult accountLocked(long remainingMinutes) {
            return new LoginCheckResult(false,
                    String.format("账户已锁定，请%d分钟后再试", remainingMinutes), 0, remainingMinutes);
        }

        public static LoginCheckResult ipLocked() {
            return new LoginCheckResult(false, "IP已被锁定，请稍后再试", 0, 0);
        }

        // Getters
        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
        public int getRemainingAttempts() { return remainingAttempts; }
        public long getLockRemainingMinutes() { return lockRemainingMinutes; }
    }
}
