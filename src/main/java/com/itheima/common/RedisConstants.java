package com.itheima.common;

/**
 * Redis缓存键常量
 */
public class RedisConstants {

    // 用户社交信息缓存
    public static final String USER_FRIENDS_KEY = "social:friends:user:%d"; // %d=userId
    public static final String USER_FOLLOWING_KEY = "social:following:user:%d";
    public static final String USER_FOLLOWERS_KEY = "social:followers:user:%d";
    public static final String USER_FRIEND_APPLIES_KEY = "social:friend_applies:user:%d";

    // 用户信息缓存
    public static final String USER_INFO_KEY = "user:info:%d"; // %d=userId

    // 聊天消息缓存
    public static final String CHAT_MESSAGES_KEY = "chat:messages:%d:%d"; // %d:senderId:receiverId
    public static final String CHAT_UNREAD_COUNT_KEY = "chat:unread:%d:%d"; // %d:userId:friendId

    // 消息发送状态
    public static final String MESSAGE_STATUS_KEY = "message:status:%s"; // %s=messageId

    // 频率限制
    public static final String RATE_LIMIT_KEY = "rate:limit:%s:%d"; // %s=action:%d=userId

    // 敏感词缓存
    public static final String SENSITIVE_WORDS_KEY = "system:sensitive_words";

    // 缓存过期时间
    public static final long USER_CACHE_TTL = 30; // 30分钟
    public static final long MESSAGE_CACHE_TTL = 7; // 7天
    public static final long RATE_LIMIT_TTL = 60; // 60秒
    public static final long SENSITIVE_WORDS_TTL = 24 * 60; // 24小时

    private RedisConstants() {}
}