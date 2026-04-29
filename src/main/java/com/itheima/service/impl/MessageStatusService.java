package com.itheima.service.impl;

import com.itheima.dto.ChatMessageDTO;
import com.itheima.mapper.SocialMapper;
import com.itheima.pojo.ChatMessage;
import com.itheima.pojo.Notification;
import com.itheima.pojo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageStatusService {

    private final SocialMapper socialMapper;
    private final SocialCacheService cacheService;

    /**
     * 更新消息状态
     */
    @Transactional
    public Result updateMessageStatus(String messageId, String status, Integer userId) {
        try {
            // 1. 获取消息
            //如果是通知消息
            Notification notification = socialMapper.selectNotificationByMessageId(messageId);
            //如果是聊天消息
            ChatMessage chatMessage = socialMapper.selectChatMessageByMessageId(messageId);
            if (notification == null && chatMessage == null) {
                return Result.error("消息不存在");
            }
            if (notification != null) {
                // 2. 验证权限
                if (!notification.getRelatedUserId().equals(userId) &&
                        !notification.getUserId().equals(userId)) {
                    return Result.error("无权操作此消息");
                }

                // 3. 更新数据库状态
                notification.setStatus(status);
                socialMapper.updateNotification(notification);

                // 4. 更新缓存状态
                SocialCacheService.MessageStatus cacheStatus =
                        SocialCacheService.MessageStatus.valueOf(status.toUpperCase());
                cacheService.cacheMessageStatus(messageId, cacheStatus);

                // 5. 如果是标记为已读，更新未读计数
                if ("read".equals(status)) {
                    cacheService.resetUnreadCount(notification.getUserId(),
                            notification.getRelatedUserId());
                }
            }else {
                // 2. 验证权限
                if (!chatMessage.getSenderId().equals(userId) &&
                        !chatMessage.getReceiverId().equals(userId)) {
                    return Result.error("无权操作此消息");
                }

                // 3. 更新数据库状态
                chatMessage.setStatus(status);
                socialMapper.updateChatMessage(chatMessage);

                // 4. 更新缓存状态
                SocialCacheService.MessageStatus cacheStatus =
                        SocialCacheService.MessageStatus.valueOf(status.toUpperCase());
                cacheService.cacheMessageStatus(messageId, cacheStatus);

                // 5. 如果是标记为已读，更新未读计数
                if ("read".equals(status)) {
                    cacheService.resetUnreadCount(chatMessage.getSenderId(),
                            chatMessage.getReceiverId());
                }
            }

            return Result.success("消息状态更新成功");

        } catch (Exception e) {
            log.error("更新消息状态失败 messageId:{}", messageId, e);
            return Result.error("更新消息状态失败");
        }
    }

    /**
     * 获取消息状态
     */
    public Result<String> getMessageStatus(String messageId, Integer userId) {
        try {
            // 1. 先尝试从缓存获取
            SocialCacheService.MessageStatus cachedStatus =
                    cacheService.getMessageStatus(messageId);
            if (cachedStatus != null) {
                return Result.success(cachedStatus.name().toLowerCase());
            }

            // 2. 从数据库获取
            //如果是通知消息
            Notification notification = socialMapper.selectNotificationByMessageId(messageId);
            //如果是聊天消息
            ChatMessage chatMessage = socialMapper.selectChatMessageByMessageId(messageId);
            if (notification == null && chatMessage == null) {
                return Result.error("消息不存在");
            }
            if (notification != null) {
                // 3. 验证权限
                if (!notification.getRelatedUserId().equals(userId) &&
                        !notification.getUserId().equals(userId)) {
                    return Result.error("无权查看此消息");
                }

                String status = notification.getStatus();
                if (status == null) {
                    status = "sent"; // 默认状态
                }
                return Result.success(status);
            }else {
               // 3. 验证权限
                if (!chatMessage.getSenderId().equals(userId) &&
                        !chatMessage.getReceiverId().equals(userId)) {
                    return Result.error("无权查看此消息");
                }

                String status = chatMessage.getStatus();
                if (status == null) {
                    status = "sent"; // 默认状态
                }
                return Result.success(status);
            }


        } catch (Exception e) {
            log.error("获取消息状态失败 messageId:{}", messageId, e);
            return Result.error("获取消息状态失败");
        }
    }

    /**
     * 批量标记消息为已读
     */
    @Transactional
    public Result markMessagesAsRead(Integer userId, Integer friendId) {
        try {
            // 1. 更新数据库
            socialMapper.markChatNotificationsAsRead(userId, friendId);
            socialMapper.markMessagesAsRead(userId, friendId);

            // 2. 更新缓存
            cacheService.resetUnreadCount(userId, friendId);

            // 3. 批量更新消息状态
            // 这里可以添加批量更新逻辑

            return Result.success("消息已标记为已读");

        } catch (Exception e) {
            log.error("标记消息为已读失败 userId:{}, friendId:{}", userId, friendId, e);
            return Result.error("标记消息为已读失败");
        }
    }
}