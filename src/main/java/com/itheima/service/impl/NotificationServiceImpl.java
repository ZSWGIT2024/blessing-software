package com.itheima.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.mapper.NotificationMapper;
import com.itheima.pojo.Notification;
import com.itheima.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public boolean sendNotification(Notification notification) {
        notification.setCreateTime(LocalDateTime.now());
        int result = notificationMapper.insert(notification);
        return result > 0;
    }

    @Override
    @Transactional
    public boolean sendFriendApplyNotification(Integer fromUserId, Integer toUserId, String message) {
        try {
            Notification notification = new Notification();
            notification.setUserId(toUserId);
            notification.setType("friend_apply");
            notification.setTitle("好友申请");
            notification.setContent(message != null ? message : "您收到了一个好友申请");

            // 设置附加数据
            Map<String, Object> data = new HashMap<>();
            data.put("fromUserId", fromUserId);
            data.put("applyTime", LocalDateTime.now());
            notification.setData(objectMapper.writeValueAsString(data));

            notification.setRelatedUserId(fromUserId);
            notification.setRelatedType("user");

            return sendNotification(notification);
        } catch (JsonProcessingException e) {
            log.error("序列化通知数据失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean sendFriendApplyResultNotification(Integer fromUserId, Integer toUserId, Boolean accepted, String replyMsg) {
        try {
            Notification notification = new Notification();
            notification.setUserId(toUserId);
            notification.setType("friend_apply_result");
            notification.setTitle(accepted ? "好友申请已通过" : "好友申请被拒绝");
            notification.setContent(replyMsg != null ? replyMsg :
                    (accepted ? "对方已通过您的好友申请" : "对方拒绝了您的好友申请"));

            // 设置附加数据
            Map<String, Object> data = new HashMap<>();
            data.put("fromUserId", fromUserId);
            data.put("accepted", accepted);
            data.put("replyMsg", replyMsg);
            data.put("processTime", LocalDateTime.now());
            notification.setData(objectMapper.writeValueAsString(data));

            notification.setRelatedUserId(fromUserId);
            notification.setRelatedType("user");

            return sendNotification(notification);
        } catch (JsonProcessingException e) {
            log.error("序列化通知数据失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean markAsRead(Long notificationId) {
        return notificationMapper.markAsRead(notificationId) > 0;
    }

    @Override
    @Transactional
    public boolean markAllAsRead(Integer userId) {
        return notificationMapper.markAllAsRead(userId) > 0;
    }

    @Override
    @Transactional
    public boolean deleteNotification(Long notificationId) {
        return notificationMapper.delete(notificationId) > 0;
    }

    @Override
    public List<Notification> getUserNotifications(Integer userId, String type, Boolean unreadOnly, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return notificationMapper.selectByUser(userId, type, unreadOnly, offset, pageSize);
    }

    @Override
    public int getUnreadCount(Integer userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public List<Notification> getLatestNotifications(Integer userId, Integer limit) {
        return notificationMapper.selectLatest(userId, limit);
    }
}