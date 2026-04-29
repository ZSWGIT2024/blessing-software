package com.itheima.service;

import com.itheima.pojo.Notification;

import java.util.List;

public interface NotificationService {

    boolean sendNotification(Notification notification);

    boolean sendFriendApplyNotification(Integer fromUserId, Integer toUserId, String message);

    boolean sendFriendApplyResultNotification(Integer fromUserId, Integer toUserId, Boolean accepted, String replyMsg);

    boolean markAsRead(Long notificationId);

    boolean markAllAsRead(Integer userId);

    boolean deleteNotification(Long notificationId);

    List<Notification> getUserNotifications(Integer userId, String type, Boolean unreadOnly, Integer page, Integer pageSize);

    int getUnreadCount(Integer userId);

    List<Notification> getLatestNotifications(Integer userId, Integer limit);
}