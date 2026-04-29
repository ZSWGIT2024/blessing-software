package com.itheima.service.impl;

import com.itheima.common.RedisConstants;
import com.itheima.dto.*;
import com.itheima.pojo.Notification;
import com.itheima.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialCacheService {

    private final RedisUtil redisUtil;

    // ==================== 好友列表缓存 ====================
    public void cacheFriends(Integer userId, List<FriendRelationDTO> friends) {
        String key = String.format(RedisConstants.USER_FRIENDS_KEY, userId);
        try {
            redisUtil.set(key, friends, RedisConstants.USER_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存好友列表失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<FriendRelationDTO> getCachedFriends(Integer userId) {
        String key = String.format(RedisConstants.USER_FRIENDS_KEY, userId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<FriendRelationDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存好友列表失败 userId:{}", userId, e);
        }
        return null;
    }

    public void evictFriendsCache(Integer userId) {
        String key = String.format(RedisConstants.USER_FRIENDS_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 关注列表缓存 ====================
    public void cacheFollowing(Integer userId, List<FollowRelationDTO> following) {
        String key = String.format(RedisConstants.USER_FOLLOWING_KEY, userId);
        try {
            redisUtil.set(key, following, RedisConstants.USER_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存关注列表失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<FollowRelationDTO> getCachedFollowing(Integer userId) {
        String key = String.format(RedisConstants.USER_FOLLOWING_KEY, userId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<FollowRelationDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存关注列表失败 userId:{}", userId, e);
        }
        return null;
    }

    public void evictFollowingCache(Integer userId) {
        String key = String.format(RedisConstants.USER_FOLLOWING_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 粉丝列表缓存 ====================
public void cacheFollowers(Integer userId, List<FollowRelationDTO> followers) {
        String key = String.format(RedisConstants.USER_FOLLOWERS_KEY, userId);
        try {
            redisUtil.set(key, followers, RedisConstants.USER_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存粉丝列表失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<FollowRelationDTO> getCachedFollowers(Integer userId) {
        String key = String.format(RedisConstants.USER_FOLLOWERS_KEY, userId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<FollowRelationDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存粉丝列表失败 userId:{}", userId, e);
        }
        return null;
    }

    public void evictFollowerCache(Integer userId) {
        String key = String.format(RedisConstants.USER_FOLLOWERS_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 好友申请缓存 ====================
    public void cacheFriendApplies(Integer userId, List<FriendApplyDTO> applies) {
        String key = String.format(RedisConstants.USER_FRIEND_APPLIES_KEY, userId);
        try {
            redisUtil.set(key, applies, RedisConstants.USER_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存好友申请失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<FriendApplyDTO> getCachedFriendApplies(Integer userId) {
        String key = String.format(RedisConstants.USER_FRIEND_APPLIES_KEY, userId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<FriendApplyDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存好友申请失败 userId:{}", userId, e);
        }
        return null;
    }

    public void evictFriendAppliesCache(Integer userId) {
        String key = String.format(RedisConstants.USER_FRIEND_APPLIES_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 用户信息缓存 ====================
    public void cacheUserInfo(Integer userId, Object userInfo) {
        String key = String.format(RedisConstants.USER_INFO_KEY, userId);
        try {
            redisUtil.set(key, userInfo, RedisConstants.USER_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存用户信息失败 userId:{}", userId, e);
        }
    }

    public Object getCachedUserInfo(Integer userId) {
        String key = String.format(RedisConstants.USER_INFO_KEY, userId);
        return redisUtil.get(key);
    }

    public void evictUserInfoCache(Integer userId) {
        String key = String.format(RedisConstants.USER_INFO_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 未读消息数缓存 ====================
    public void cacheUnreadCount(Integer userId, Integer friendId, Integer count) {
        String key = String.format(RedisConstants.CHAT_UNREAD_COUNT_KEY, userId, friendId);
        try {
            redisUtil.set(key, count, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("缓存未读消息数失败 userId:{}, friendId:{}", userId, friendId, e);
        }
    }

    public Integer getCachedUnreadCount(Integer userId, Integer friendId) {
        String key = String.format(RedisConstants.CHAT_UNREAD_COUNT_KEY, userId, friendId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof Integer) {
                return (Integer) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存未读消息数失败 userId:{}, friendId:{}", userId, friendId, e);
        }
        return null;
    }

    public void incrementUnreadCount(Integer userId, Integer friendId) {
        String key = String.format(RedisConstants.CHAT_UNREAD_COUNT_KEY, userId, friendId);
        redisUtil.increment(key, 1);
    }

    public void resetUnreadCount(Integer userId, Integer friendId) {
        String key = String.format(RedisConstants.CHAT_UNREAD_COUNT_KEY, userId, friendId);
        redisUtil.delete(key);
    }


    public void updateFriendStarStatus(Integer userId, Integer friendId, Boolean isStarred) {
        List<FriendRelationDTO> friends = getCachedFriends(userId);
        if (friends != null) {
            for (FriendRelationDTO friend : friends) {
                if (friend.getFriendId().equals(friendId)) {
                    friend.setIsStarred(isStarred);
                    break;
                }
            }
            cacheFriends(userId, friends);
        }
    }

    public void updateFriendRemark(Integer userId, Integer friendId, String remark) {
        List<FriendRelationDTO> friends = getCachedFriends(userId);
        if (friends != null) {
            for (FriendRelationDTO friend : friends) {
                if (friend.getFriendId().equals(friendId)) {
                    friend.setRemark(remark);
                    break;
                }
            }
            cacheFriends(userId, friends);
        }
    }

    public void updateFriendBlockStatus(Integer userId, Integer friendId, Boolean isBlocked) {
        List<FriendRelationDTO> friends = getCachedFriends(userId);
        if (friends != null) {
            for (FriendRelationDTO friend : friends) {
                if (friend.getFriendId().equals(friendId)) {
                    friend.setIsBlocked(isBlocked);
                    break;
                }
            }
            cacheFriends(userId, friends);
        }
    }

    // ==================== 消息状态缓存 ====================
    public enum MessageStatus {
        SENDING,    // 发送中
        SENT,       // 已发送
        FAILED,     // 发送失败
        READ,       // 已读
        WITHDRAWN   // 已撤回
    }

    public void cacheMessageStatus(String messageId, MessageStatus status) {
        String key = String.format(RedisConstants.MESSAGE_STATUS_KEY, messageId);
        try {
            redisUtil.set(key, status.name(), 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("缓存消息状态失败 messageId:{}", messageId, e);
        }
    }

    public MessageStatus getMessageStatus(String messageId) {
        String key = String.format(RedisConstants.MESSAGE_STATUS_KEY, messageId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof String) {
                return MessageStatus.valueOf((String) cached);
            }
        } catch (Exception e) {
            log.error("获取消息状态失败 messageId:{}", messageId, e);
        }
        return null;
    }

    // ==================== 批量清除社交信息缓存 ====================
    public void evictAllSocialCache(Integer userId) {
        evictFriendsCache(userId);
        evictFollowingCache(userId);
        evictFollowerCache(userId);
        evictFriendAppliesCache(userId);
        evictUserInfoCache(userId);
    }

    // 在SocialCacheService中添加以下方法

    // ==================== 最近聊天缓存 ====================
    public void cacheRecentChats(Integer userId, List<RecentChatDTO> recentChats) {
        String key = String.format("social:recent_chats:user:%d", userId);
        try {
            redisUtil.set(key, recentChats, 5, TimeUnit.MINUTES); // 最近聊天缓存5分钟
        } catch (Exception e) {
            log.error("缓存最近聊天失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<RecentChatDTO> getCachedRecentChats(Integer userId) {
        String key = String.format("social:recent_chats:user:%d", userId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<RecentChatDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存最近聊天失败 userId:{}", userId, e);
        }
        return null;
    }

    public void evictRecentChatsCache(Integer userId) {
        String key = String.format("social:recent_chats:user:%d", userId);
        redisUtil.delete(key);
    }

    // ==================== 聊天历史缓存 ====================
    public void cacheChatHistory(Integer userId, Integer relatedUserId, List<ChatMessageDTO> messages) {
        String key = String.format("social:chat_history:%d:%d", userId, relatedUserId);
        try {
            redisUtil.set(key, messages, 10, TimeUnit.MINUTES); // 聊天历史缓存10分钟
        } catch (Exception e) {
            log.error("缓存聊天历史失败 userId:{}, relatedUserId:{}", userId, relatedUserId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<ChatMessageDTO> getCachedChatHistory(Integer userId, Integer relatedUserId) {
        String key = String.format("social:chat_history:%d:%d", userId, relatedUserId);
        try {
            Object cached = redisUtil.get(key);
            if (cached instanceof List) {
                return (List<ChatMessageDTO>) cached;
            }
        } catch (Exception e) {
            log.error("获取缓存聊天历史失败 userId:{}, relatedUserId:{}", userId, relatedUserId, e);
        }
        return null;
    }

    public void evictChatHistoryCache(Integer userId, Integer relatedUserId) {
        String key = String.format("social:chat_history:%d:%d", userId, relatedUserId);
        redisUtil.delete(key);
    }

    // ==================== 批量清除聊天信息信息缓存 ====================
    public void evictAllChatMessagesCache(Integer userId, Integer relatedUserId) {
        evictRecentChatsCache(userId);
        evictChatHistoryCache(userId,relatedUserId);
    }
}