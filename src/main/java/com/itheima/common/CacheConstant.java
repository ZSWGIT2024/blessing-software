package com.itheima.common;

import java.time.Duration;

public class CacheConstant {

    // 缓存键前缀
    public static final String PREFIX = "heima:";

    // 关注相关缓存
    public static final String CACHE_FOLLOW = PREFIX + "follow";
    public static final String CACHE_FOLLOWING_COUNT = PREFIX + "following:count";
    public static final String CACHE_FOLLOWER_COUNT = PREFIX + "follower:count";

    // 好友相关缓存
    public static final String CACHE_FRIEND = PREFIX + "friend";
    public static final String CACHE_FRIEND_COUNT = PREFIX + "friend:count";
    public static final String CACHE_PENDING_APPLY_COUNT = PREFIX + "pending:apply:count";

    // 用户相关缓存
    public static final String CACHE_USER = PREFIX + "user";

    // 通知相关缓存
    public static final String CACHE_NOTIFICATION = PREFIX + "notification";

    // 缓存过期时间
    public static final Duration EXPIRE_FOLLOW = Duration.ofHours(1);
    public static final Duration EXPIRE_FRIEND = Duration.ofHours(2);
    public static final Duration EXPIRE_USER = Duration.ofHours(3);
    public static final Duration EXPIRE_NOTIFICATION = Duration.ofMinutes(30);

    // 缓存键生成工具方法
    public static String getFollowKey(Integer userId, Integer targetUserId) {
        return String.format("%s:%d:%d", CACHE_FOLLOW, userId, targetUserId);
    }

    public static String getFollowingCountKey(Integer userId) {
        return String.format("%s:%d", CACHE_FOLLOWING_COUNT, userId);
    }

    public static String getFollowerCountKey(Integer userId) {
        return String.format("%s:%d", CACHE_FOLLOWER_COUNT, userId);
    }

    public static String getFriendKey(Integer userId, Integer friendId) {
        return String.format("%s:%d:%d", CACHE_FRIEND, userId, friendId);
    }

    public static String getFriendCountKey(Integer userId) {
        return String.format("%s:%d", CACHE_FRIEND_COUNT, userId);
    }

    public static String getPendingApplyCountKey(Integer userId) {
        return String.format("%s:%d", CACHE_PENDING_APPLY_COUNT, userId);
    }

    public static String getUserKey(Integer userId) {
        return String.format("%s:%d", CACHE_USER, userId);
    }
}