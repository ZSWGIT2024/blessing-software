package com.itheima.common;

/**
 * Redis 缓存键常量与 TTL 配置
 * <p>
 * 集中管理所有缓存键格式和过期时间，避免散落魔术字符串。
 * 所有写操作后必须调用对应 CacheService 的 evict 方法清理缓存。
 * </p>
 */
public class RedisConstants {

    // ==================== 用户社交信息 ====================
    public static final String USER_FRIENDS_KEY = "social:friends:user:%d";
    public static final String USER_FOLLOWING_KEY = "social:following:user:%d";
    public static final String USER_FOLLOWERS_KEY = "social:followers:user:%d";
    public static final String USER_FRIEND_APPLIES_KEY = "social:friend_applies:user:%d";
    public static final String USER_RECENT_CHATS_KEY = "social:recent_chats:user:%d";
    public static final String USER_CHAT_HISTORY_KEY = "social:chat_history:%d:%d";

    // ==================== 用户信息 ====================
    public static final String USER_INFO_KEY = "user:info:%d";

    // ==================== 聊天消息 ====================
    public static final String CHAT_UNREAD_COUNT_KEY = "chat:unread:%d:%d";
    public static final String MESSAGE_STATUS_KEY = "message:status:%s";

    // ==================== 群聊 ====================
    public static final String GROUP_INFO_KEY = "group:info:%s";
    public static final String GROUP_MEMBERS_KEY = "group:members:%s";
    public static final String GROUP_LIST_KEY = "group:list:%d";
    public static final String GROUP_UNREAD_KEY = "group:unread:%s:%d";
    public static final String GROUP_ONLINE_KEY = "group:online:%s";
    public static final String GROUP_MESSAGES_KEY = "group:messages:%s";

    // ==================== 世界聊天 ====================
    public static final String WORLD_RECENT_MSGS_KEY = "world:recent_msgs";
    public static final String WORLD_MSG_DETAIL_KEY = "world:msg:%s";

    // ==================== 媒体资源 (user_media) ====================
    public static final String MEDIA_LIST_KEY = "media:list:user:%d:status:%s:type:%s:p%d:s%d";
    public static final String MEDIA_DETAIL_KEY = "media:detail:%d";
    public static final String MEDIA_RECENT_KEY = "media:recent:user:%d";
    public static final String MEDIA_STATS_KEY = "media:stats:user:%d";

    // ==================== 收藏 ====================
    public static final String FAVORITE_FOLDERS_KEY = "favorite:folders:user:%d";
    public static final String FAVORITE_LIST_KEY = "favorite:list:user:%d:folder:%s:p%d:s%d";
    public static final String FAVORITE_MEDIA_IDS_KEY = "favorite:media_ids:user:%d";

    // ==================== 表情包 ====================
    public static final String EMOJI_CATEGORIES_KEY = "emoji:categories";
    public static final String EMOJI_SYSTEM_LIST_KEY = "emoji:system:category:%s";
    public static final String EMOJI_PACKS_KEY = "emoji:packs";
    public static final String EMOJI_PACK_ITEMS_KEY = "emoji:pack_items:%d";

    // ==================== 频率限制 / 系统 ====================
    public static final String RATE_LIMIT_KEY = "rate:limit:%s:%d";
    public static final String SENSITIVE_WORDS_KEY = "system:sensitive_words";

    // ============================================================
    //  TTL 配置（分钟）
    // ============================================================
    public static final long USER_CACHE_TTL = 30;          // 用户信息
    public static final long SOCIAL_LIST_TTL = 15;         // 好友/关注/粉丝列表
    public static final long FRIEND_APPLY_TTL = 5;         // 好友申请列表（变更频繁）
    public static final long CHAT_HISTORY_TTL = 10;        // 聊天历史
    public static final long RECENT_CHATS_TTL = 5;         // 最近聊天列表
    public static final long UNREAD_COUNT_TTL = 60;        // 未读计数（与旧行为一致）
    public static final long MESSAGE_CACHE_TTL = 10080;    // 消息状态 7天
    public static final long RATE_LIMIT_TTL = 1;           // 频率限制 1分钟
    public static final long SENSITIVE_WORDS_TTL = 1440;   // 敏感词 24小时

    public static final long GROUP_INFO_TTL = 30;          // 群信息
    public static final long GROUP_LIST_TTL = 15;          // 用户群列表
    public static final long GROUP_MEMBERS_TTL = 10;       // 群成员列表
    public static final long GROUP_MESSAGES_TTL = 5;       // 群消息缓存
    public static final long GROUP_ONLINE_TTL = 2;         // 在线成员

    public static final long WORLD_MSG_TTL = 2;            // 世界消息（热数据，短 TTL）
    public static final long WORLD_MSG_DETAIL_TTL = 10;    // 单条消息详情

    public static final long MEDIA_LIST_TTL = 5;           // 媒体列表（变更较频繁）
    public static final long MEDIA_DETAIL_TTL = 30;        // 媒体详情
    public static final long MEDIA_STATS_TTL = 10;         // 媒体统计
    public static final long MEDIA_RECENT_TTL = 5;         // 最近上传

    public static final long FAVORITE_LIST_TTL = 5;        // 收藏列表
    public static final long FAVORITE_FOLDER_TTL = 10;     // 收藏夹
    public static final long FAVORITE_IDS_TTL = 5;         // 收藏媒体ID列表

    public static final long EMOJI_PACK_TTL = 60;          // 表情包列表（静态数据）
    public static final long EMOJI_ITEM_TTL = 30;          // 表情包内容
    public static final long EMOJI_SYSTEM_TTL = 120;       // 系统emoji（几乎不变）

    /** 空值缓存 TTL（防止缓存穿透），单位分钟 */
    public static final long NULL_VALUE_TTL = 2;

    private RedisConstants() {}
}
