package com.itheima.service.impl;

import com.itheima.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WebSocket
 * 离线消息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineMessageService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis key 前缀
    private static final String OFFLINE_KEY_PREFIX = "offline:msg:";
    // 离线消息保留 7 天
    private static final Duration MESSAGE_TTL = Duration.ofDays(7);

    /**
     * 保存离线消息（存到 Redis List 中）
     */
    public void saveOfflineMessage(Integer userId, ChatMessageDTO message) {
        message.setOffline(true);
        String key = OFFLINE_KEY_PREFIX + userId;
        // 右侧推入
        redisTemplate.opsForList().rightPush(key, message);
        // 设置过期时间（如果 key 已存在会刷新 TTL）
        redisTemplate.expire(key, MESSAGE_TTL);
        log.info("已保存用户 {} 的离线消息: {}", userId, message.getContent());
    }

    /**
     * 获取并删除该用户的所有离线消息（原子操作）
     * 注意：range + delete 并非原子，但能满足大多数场景。如需严格原子可改用 Lua 脚本。
     */
    @SuppressWarnings("unchecked")
    public List<ChatMessageDTO> getAndClearOfflineMessages(Integer userId) {
        String key = OFFLINE_KEY_PREFIX + userId;
        List<ChatMessageDTO> messages = new ArrayList<>();

        // 获取所有消息
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList != null && !rawList.isEmpty()) {
            for (Object obj : rawList) {
                if (obj instanceof ChatMessageDTO) {
                    messages.add((ChatMessageDTO) obj);
                }
            }
        }

        // 删除 key
        if (!messages.isEmpty()) {
            redisTemplate.delete(key);
            log.info("已拉取并删除用户 {} 的 {} 条离线消息", userId, messages.size());
        }
        return messages;
    }

    // 离线群消息 key 前缀
    private static final String OFFLINE_GROUP_KEY_PREFIX = "offline:group:";

    /**
     * 保存离线群消息（存到 Redis List 中）
     */
    public void saveOfflineGroupMessage(Integer userId, String groupId, Object message) {
        String key = OFFLINE_GROUP_KEY_PREFIX + groupId + ":" + userId;
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, MESSAGE_TTL);
        log.info("已保存用户 {} 在群 {} 的离线消息", userId, groupId);
    }

    /**
     * 获取并删除该用户在指定群的离线消息
     */
    @SuppressWarnings("unchecked")
    public List<Object> getAndClearOfflineGroupMessages(Integer userId, String groupId) {
        String key = OFFLINE_GROUP_KEY_PREFIX + groupId + ":" + userId;
        List<Object> messages = new ArrayList<>();

        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList != null && !rawList.isEmpty()) {
            messages.addAll(rawList);
        }

        if (!messages.isEmpty()) {
            redisTemplate.delete(key);
            log.info("已拉取并删除用户 {} 在群 {} 的 {} 条离线消息", userId, groupId, messages.size());
        }
        return messages;
    }

    /**
     * 获取并删除用户所有群的离线消息
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<Object>> getAllOfflineGroupMessages(Integer userId) {
        String pattern = OFFLINE_GROUP_KEY_PREFIX + "*:" + userId;
        Map<String, List<Object>> result = new java.util.LinkedHashMap<>();

        java.util.Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null) {
            for (String key : keys) {
                String groupId = key.substring(OFFLINE_GROUP_KEY_PREFIX.length(), key.lastIndexOf(":"));
                List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
                if (rawList != null && !rawList.isEmpty()) {
                    result.put(groupId, new ArrayList<>(rawList));
                }
                redisTemplate.delete(key);
            }
        }
        log.info("已拉取并删除用户 {} 的 {} 个群的离线消息", userId, result.size());
        return result;
    }

    /**
     * 查询离线消息数量（可选，用于前端小红点）
     */
    public Long getOfflineMessageCount(Integer userId) {
        String key = OFFLINE_KEY_PREFIX + userId;
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size : 0L;
    }
}