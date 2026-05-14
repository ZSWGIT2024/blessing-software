package com.itheima.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.dto.WorldMessageDTO;
import com.itheima.mapper.ChatWorldMessageMapper;
import com.itheima.pojo.ChatWorldMessage;
import com.itheima.pojo.Result;
import com.itheima.service.WorldChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldChatServiceImpl implements WorldChatService {

    private final ChatWorldMessageMapper worldMessageMapper;
    private final RateLimitService rateLimitService;
    private final SensitiveFilterService sensitiveFilterService;
    private final ObjectMapper objectMapper;
    private final WorldChatCacheService worldChatCacheService;

    @Override
    @Transactional
    public Result<Map<String, Object>> sendWorldMessage(Integer senderId, String content, String contentType,
                                                         String messageId, Map<String, Object> extraData) {
        try {
            if (!rateLimitService.checkWorldChatRate(senderId)) {
                return Result.error("世界聊天发送过于频繁，请稍后再试");
            }

            String filteredContent = sensitiveFilterService.filter(content);
            if (filteredContent == null) {
                return Result.error("消息内容包含敏感词，请修改后重新发送");
            }
            List<String> sensitiveWords = sensitiveFilterService.findAllSensitiveWords(content);
            boolean isFiltered = !sensitiveWords.isEmpty();

            if (messageId == null || messageId.isEmpty()) {
                messageId = senderId + "_world_" + System.currentTimeMillis();
            }

            String extraDataJson = null;
            if (extraData != null && !extraData.isEmpty()) {
                try {
                    extraDataJson = objectMapper.writeValueAsString(extraData);
                } catch (Exception ignored) {}
            }

            String fileUrl = null;
            if (extraData != null && extraData.containsKey("fileUrl")) {
                fileUrl = (String) extraData.get("fileUrl");
            }

            ChatWorldMessage message = new ChatWorldMessage();
            message.setMessageId(messageId);
            message.setSenderId(senderId);
            message.setContentType(contentType != null ? contentType : "text");
            message.setContent(filteredContent);
            message.setOriginalContent(content);
            message.setFileUrl(fileUrl);
            message.setExtraData(extraDataJson);
            message.setStatus("sent");
            message.setIsDeleted(false);
            message.setIsFiltered(isFiltered);
            message.setFilteredWords(isFiltered ? String.join(",", sensitiveWords) : null);

            worldMessageMapper.insert(message);
            worldChatCacheService.evictRecentMessages(); // 新消息使缓存失效

            Map<String, Object> result = new HashMap<>();
            result.put("messageId", messageId);
            result.put("senderId", senderId);
            result.put("content", filteredContent);
            result.put("contentType", contentType);
            result.put("status", "sent");
            result.put("timestamp", System.currentTimeMillis());
            if (isFiltered) result.put("filteredWords", sensitiveWords);
            if (fileUrl != null) result.put("fileUrl", fileUrl);

            return Result.success(result);
        } catch (Exception e) {
            log.error("发送世界消息失败", e);
            return Result.error("消息发送失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<WorldMessageDTO>> getWorldMessages(String lastMessageId, int pageSize) {
        if (pageSize <= 0 || pageSize > 100) pageSize = 50;
        // 查缓存（仅首屏，无 lastMessageId 的请求）
        if (lastMessageId == null || lastMessageId.isEmpty()) {
            List<WorldMessageDTO> cached = worldChatCacheService.getCachedRecentMessages();
            if (cached != null) return Result.success(cached);
        }
        int limit = lastMessageId != null && !lastMessageId.isEmpty() ? pageSize : Math.min(pageSize, 100);
        List<WorldMessageDTO> dtos = worldMessageMapper.selectRecentWithUser(limit);
        List<WorldMessageDTO> result = dtos != null ? dtos : Collections.emptyList();
        // 写缓存（仅首屏，防穿透）
        if ((lastMessageId == null || lastMessageId.isEmpty()) && result.isEmpty()) {
            worldChatCacheService.cacheEmptyMessages();
        } else if (lastMessageId == null || lastMessageId.isEmpty()) {
            worldChatCacheService.cacheRecentMessages(result);
        }
        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<?> withdrawWorldMessage(Integer userId, String messageId, String reason) {
        ChatWorldMessage msg = worldMessageMapper.selectByMessageId(messageId);
        if (msg == null) return Result.error("消息不存在");
        if (!msg.getSenderId().equals(userId)) return Result.error("只能撤回自己的消息");
        if ("withdrawn".equals(msg.getStatus())) return Result.error("消息已撤回");

        long diff = System.currentTimeMillis() - (msg.getCreateTime() != null ?
                java.sql.Timestamp.valueOf(msg.getCreateTime()).getTime() : 0);
        if (diff > 2 * 60 * 1000) return Result.error("超过2分钟无法撤回");

        worldMessageMapper.withdrawMessage(messageId, reason);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<?> deleteWorldMessage(Integer userId, String messageId) {
        ChatWorldMessage msg = worldMessageMapper.selectByMessageId(messageId);
        if (msg == null) return Result.error("消息不存在");
        if (!msg.getSenderId().equals(userId)) return Result.error("只能删除自己的消息");

        worldMessageMapper.softDelete(messageId);
        return Result.success();
    }
}
