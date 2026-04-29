package com.itheima.service;

import com.itheima.dto.*;
import com.itheima.pojo.Notification;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import java.util.List;
import java.util.Map;

public interface SocialService {

    // ==================== 好友申请相关 ====================
    Result sendFriendApply(Integer applicantId, Integer receiverId, String applyMsg);
    Result<List<FriendApplyDTO>> getPendingApplies(Integer userId);
    Result<List<FriendApplyDTO>> getMyAllApplies(Integer userId);
    Result acceptFriendApply(Long applyId);
    Result rejectFriendApply(Long applyId, Integer userId);
    Result cancelFriendApply(Long applyId, Integer userId);

    // ==================== 好友管理相关 ====================
    Result<List<FriendRelationDTO>> getFriendList(Integer userId);

    Result<List<FriendRelationDTO>> getBlacklist(Integer userId);

    /**
     * 获取好友分组列表
     */
    Result<List<FriendGroupDTO>> getFriendGroups(Integer userId);

    /**
     * 创建好友分组
     */
    Result<Void> createFriendGroup(FriendGroupDTO groupDTO);

    /**
     * 更新好友分组
     */
    Result<Void> updateFriendGroup(FriendGroupDTO groupDTO);

    /**
     * 删除好友分组
     */
    Result<Void> deleteFriendGroup(Long groupId, Integer userId);

    /**
     * 获取分组详情
     */
    Result<FriendGroupDTO> getFriendGroup(Long groupId, Integer userId);

    /**
     * 添加好友到分组
     */
    Result<Void> addFriendToGroup(Integer userId, Integer friendId, String groupName);

    /**
     * 从分组移除好友
     */
    Result<Void> removeFriendFromGroup(Integer userId, Integer friendId);

    /**
     * 获取用户详情
     */
    Result<UserSimpleDTO> getUserDetail(Integer userId, Integer currentUserId);
    Result updateFriendRemark(Integer userId, Integer friendId, String remark);
    Result updateFriendGroup(Integer userId, Integer friendId, String groupName);
    Result starFriend(Integer userId, Integer friendId, Boolean isStarred);
    Result blockFriend(Integer userId, Integer friendId, Boolean isBlocked);
    Result deleteFriend(Integer userId, Integer friendId);

    // ==================== 关注管理相关 ====================
    Result followUser(Integer followerId, Integer followingId);
    Result<List<FollowRelationDTO>> getFollowingList(Integer userId);
    Result<List<FollowRelationDTO>> getFollowerList(Integer userId);
    Result unfollowUser(Integer followerId, Integer followingId);
    Result updateFollowRemark(Integer followerId, Integer followingId, String remark);

    // ==================== 消息通知相关 ====================
    Result sendMessage(Integer senderId, Integer receiverId, String content, String chatType, String contentType, String messageId, Map<String, Object> extraData);
    Result deleteMessage(Integer userId,String messageId);
    Result<List<Notification>> getNotifications(Integer userId, Integer pageNum, Integer pageSize);
    Result<Integer> getUnreadCount(Integer userId);
    Result markAsRead(Integer notificationId, Integer userId);
    Result markAllAsRead(Integer userId);

    // ==================== 聊天相关 ====================
    Result<List<RecentChatDTO>> getRecentChats(Integer userId);
    Result<List<ChatMessageDTO>> getChatHistory(Integer userId, Integer relatedUserId,String lastMessageId, Integer pageSize);
    Result clearChatHistory(Integer userId, Integer friendId);

    // ==================== 搜索相关 ====================
    Result<PageBean<UserSimpleDTO>> searchUsers(String keyword, Integer pageNum, Integer pageSize);

    // ==================== 社交信息汇总 ====================
    Result<Map<String, Object>> getSocialInfo(Integer userId);

    Result withdrawMessage(String messageId, Integer userId, String reason);

}