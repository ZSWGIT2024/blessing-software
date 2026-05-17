package com.itheima.service.impl;

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
    private static final String BAN_LEVEL_PREFIX = "login:ban_level:";
    private static final String CAPTCHA_REQUIRED_PREFIX = "login:captcha:";

    // 配置参数
    public static final int MAX_FAIL_COUNT = 5;
    private static final int MAX_IP_FAIL_COUNT = 20;
    private static final long IP_LOCK_DURATION_MINUTES = 60;

    /** 递增封禁时长（分钟）：1h, 1d, 7d, 30d, 永久 */
    private static final long[] BAN_ESCALATION_MINUTES = {60, 1440, 10080, 43200, 999999};
    /** 重置封禁等级所需的冷静期倍数（2倍于上次封禁时长） */
    private static final double RESET_COOLDOWN_FACTOR = 2.0;

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
     * 获取账户剩余锁定时间
     * @param identifier 用户标识
     * @return [剩余分钟数, 封禁等级, 封禁等级描述]
     */
    public Object[] getLockInfo(String identifier) {
        String lockKey = ACCOUNT_LOCK_PREFIX + identifier;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
        long remaining = (ttl != null && ttl > 0) ? ttl : 0;
        int level = getBanLevel(identifier);
        if (remaining <= 0) return new Object[]{0L, 0, ""};
        String desc;
        if (level <= 1) desc = "1小时";
        else if (level == 2) desc = "1天";
        else if (level == 3) desc = "7天";
        else if (level == 4) desc = "30天";
        else desc = "永久";
        return new Object[]{remaining, level, desc};
    }

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
     * 锁定账户（递增封禁时长）
     * @param identifier 用户标识
     */
    public void lockAccount(String identifier) {
        int currentLevel = getBanLevel(identifier);
        // 检查是否需要重置等级（冷静期内没有再次被封）
        if (shouldResetBanLevel(identifier, currentLevel)) {
            currentLevel = 0;
        }
        int nextLevel = Math.min(currentLevel + 1, BAN_ESCALATION_MINUTES.length);
        long lockMinutes = BAN_ESCALATION_MINUTES[nextLevel - 1];

        String lockKey = ACCOUNT_LOCK_PREFIX + identifier;
        redisTemplate.opsForValue().set(lockKey, String.valueOf(nextLevel), lockMinutes, TimeUnit.MINUTES);

        // 记录封禁等级和时间，用于后续递增判断
        String levelKey = BAN_LEVEL_PREFIX + identifier;
        redisTemplate.opsForValue().set(levelKey, String.valueOf(nextLevel), (long)(lockMinutes * RESET_COOLDOWN_FACTOR), TimeUnit.MINUTES);

        log.warn("账户已锁定：identifier={}, level={}, duration={}min", identifier, nextLevel, lockMinutes);
    }

    /** 获取当前封禁等级 */
    private int getBanLevel(String identifier) {
        String levelKey = BAN_LEVEL_PREFIX + identifier;
        Object val = redisTemplate.opsForValue().get(levelKey);
        return val != null ? Integer.parseInt(val.toString()) : 0;
    }

    /** 判断是否应该重置封禁等级（冷静期内未再次被封则重置） */
    private boolean shouldResetBanLevel(String identifier, int currentLevel) {
        if (currentLevel <= 0) return true;
        String lockKey = ACCOUNT_LOCK_PREFIX + identifier;
        String levelKey = BAN_LEVEL_PREFIX + identifier;
        // 如果锁已过期且封禁等级记录也已过期，说明冷静期已过
        boolean lockExpired = !Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        boolean levelExpired = !Boolean.TRUE.equals(redisTemplate.hasKey(levelKey));
        return lockExpired && levelExpired;
    }

    /** 获取当前锁定时长（分钟），未锁定返回0 */
    public long getLockDurationMinutes(String identifier) {
        int level = getBanLevel(identifier);
        if (level <= 0) return 0;
        return BAN_ESCALATION_MINUTES[level - 1];
    }

    /** 手动解锁账户 */
    public void unlockAccount(String identifier) {
        redisTemplate.delete(ACCOUNT_LOCK_PREFIX + identifier);
        redisTemplate.delete(LOGIN_FAIL_PREFIX + identifier);
        redisTemplate.delete(BAN_LEVEL_PREFIX + identifier);
        redisTemplate.delete(CAPTCHA_REQUIRED_PREFIX + identifier);
        log.info("账户已解锁：identifier={}", identifier);
    }

    // ==================== 图形验证码 ====================

    /** 标记该标识需要验证码 */
    public void requireCaptcha(String identifier) {
        redisTemplate.opsForValue().set(CAPTCHA_REQUIRED_PREFIX + identifier, "1", 30, TimeUnit.MINUTES);
    }

    /** 检查是否需要验证码 */
    public boolean isCaptchaRequired(String identifier) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CAPTCHA_REQUIRED_PREFIX + identifier));
    }

    /** 清除验证码要求（登录成功后） */
    public void clearCaptchaRequired(String identifier) {
        redisTemplate.delete(CAPTCHA_REQUIRED_PREFIX + identifier);
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
