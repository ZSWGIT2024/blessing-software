package com.itheima.service.impl;

import com.itheima.common.RedisConstants;
import com.itheima.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisUtil redisUtil;

    // 各种操作的频率限制配置
    private static final int MESSAGE_RATE_LIMIT = 30; // 每分钟最多30条消息
    private static final int FRIEND_APPLY_RATE_LIMIT = 5; // 每小时最多5个好友申请
    private static final int FOLLOW_RATE_LIMIT = 50; // 每小时最多关注50人

    /**
     * 检查消息发送频率
     */
    public boolean checkMessageRate(Integer userId) {
        String key = String.format(RedisConstants.RATE_LIMIT_KEY, "message", userId);
        return checkRateLimit(key, MESSAGE_RATE_LIMIT, 60);
    }

    /**
     * 检查好友申请频率
     */
    public boolean checkFriendApplyRate(Integer userId) {
        String key = String.format(RedisConstants.RATE_LIMIT_KEY, "friend_apply", userId);
        return checkRateLimit(key, FRIEND_APPLY_RATE_LIMIT, 3600);
    }

    /**
     * 检查关注频率
     */
    public boolean checkFollowRate(Integer userId) {
        String key = String.format(RedisConstants.RATE_LIMIT_KEY, "follow", userId);
        return checkRateLimit(key, FOLLOW_RATE_LIMIT, 3600);
    }

    /**
     * 通用频率限制检查
     */
    private boolean checkRateLimit(String key, int limit, int seconds) {
        try {
            // 获取当前计数
            Long count = (Long) redisUtil.get(key);
            if (count == null) {
                count = 0L;
            }

            // 检查是否超过限制
            if (count >= limit) {
                log.warn("频率限制触发 key:{} count:{} limit:{}", key, count, limit);
                return false;
            }

            // 增加计数
            redisUtil.increment(key, 1);

            // 如果是第一次设置，设置过期时间
            if (count == 0) {
                redisUtil.expire(key, seconds, TimeUnit.SECONDS);
            }

            return true;

        } catch (Exception e) {
            log.error("检查频率限制失败 key:{}", key, e);
            return true; // 出现异常时放行，避免影响正常业务
        }
    }

    /**
     * 清除频率限制计数
     */
    public void resetRateLimit(String key) {
        redisUtil.delete(key);
    }
}