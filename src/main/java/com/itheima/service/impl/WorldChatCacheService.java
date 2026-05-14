package com.itheima.service.impl;

import com.itheima.common.RedisConstants;
import com.itheima.dto.WorldMessageDTO;
import com.itheima.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 世界聊天缓存服务
 * <p>
 * 缓存最近的世界聊天消息列表，有效减少高频轮询对数据库的压力。
 * 世界聊天为实时场景，TTL 较短以保证消息时效性。
 * 防穿透：空列表缓存。
 * 防雪崩：TTL 加随机抖动。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorldChatCacheService {

    private final RedisUtil redisUtil;

    private long jitter(long baseSeconds) {
        double factor = 0.7 + ThreadLocalRandom.current().nextDouble() * 0.6;
        return (long) (baseSeconds * factor);
    }

    // ==================== 最近消息列表缓存 ====================

    @SuppressWarnings("unchecked")
    public void cacheRecentMessages(List<WorldMessageDTO> messages) {
        try {
            long ttl = jitter(TimeUnit.MINUTES.toSeconds(RedisConstants.WORLD_MSG_TTL));
            redisUtil.set(RedisConstants.WORLD_RECENT_MSGS_KEY, messages, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("缓存世界消息列表失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<WorldMessageDTO> getCachedRecentMessages() {
        try {
            Object obj = redisUtil.get(RedisConstants.WORLD_RECENT_MSGS_KEY);
            if (obj instanceof List) return (List<WorldMessageDTO>) obj;
        } catch (Exception e) {
            log.error("读取世界消息缓存失败", e);
        }
        return null;
    }

    /** 空列表也缓存防止穿透 */
    public void cacheEmptyMessages() {
        try {
            redisUtil.set(RedisConstants.WORLD_RECENT_MSGS_KEY, "EMPTY",
                    RedisConstants.NULL_VALUE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存世界消息空值失败", e);
        }
    }

    /** 新消息发送后清除缓存，下次查询重新加载 */
    public void evictRecentMessages() {
        redisUtil.delete(RedisConstants.WORLD_RECENT_MSGS_KEY);
    }

    // ==================== 单条消息详情缓存 ====================

    public void cacheMessageDetail(String messageId, WorldMessageDTO dto) {
        String key = String.format(RedisConstants.WORLD_MSG_DETAIL_KEY, messageId);
        try {
            redisUtil.set(key, dto, jitter(RedisConstants.WORLD_MSG_DETAIL_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存世界消息详情失败 messageId:{}", messageId, e);
        }
    }

    public WorldMessageDTO getCachedMessageDetail(String messageId) {
        String key = String.format(RedisConstants.WORLD_MSG_DETAIL_KEY, messageId);
        try {
            Object obj = redisUtil.get(key);
            if (obj instanceof WorldMessageDTO) return (WorldMessageDTO) obj;
        } catch (Exception e) {
            log.error("读取世界消息详情失败 messageId:{}", messageId, e);
        }
        return null;
    }

    /** 消息撤回/删除时驱逐 */
    public void evictMessageDetail(String messageId) {
        String key = String.format(RedisConstants.WORLD_MSG_DETAIL_KEY, messageId);
        redisUtil.delete(key);
    }
}
