package com.itheima.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户相关常量（简化版）
 */
public class UserConstant {

    // 用户类型
    public static final Integer USER_TYPE_NORMAL = 0;
    public static final Integer USER_TYPE_ADMIN = 1;

    // 账号状态
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_BANNED = "banned";
    //是否在线
    public static final String ONLINE = "online";
    public static final String OFFLINE = "offline";

    // VIP类型
    public static final Integer VIP_TYPE_NONE = 0;
    public static final Integer VIP_TYPE_MONTHLY = 1;
    public static final Integer VIP_TYPE_QUARTERLY = 2;
    public static final Integer VIP_TYPE_YEARLY = 3;
    public static final Integer VIP_TYPE_LIFETIME = 4;

    // 默认值
    public static final Integer DEFAULT_LEVEL = 1;
    public static final Long DEFAULT_EXP = 0L;

    // 最大等级
    public static final Integer MAX_LEVEL = 7;

    // 经验加成系数
    public static final double VIP_EXP_MULTIPLIER = 1.5;  // VIP经验加成50%

    // ===== 等级经验配置 =====
    /**
     * 等级所需经验表（从1级升到对应等级所需的总经验）
     * 升级规则：
     * 1-2级: 1000经验
     * 2-3级: 2000经验
     * 3-4级: 4000经验
     * 4-5级: 8000经验
     * 5-6级: 16000经验
     * 6-7级: 50000经验
     */
    private static final Map<Integer, Long> LEVEL_EXP_MAP = new HashMap<>();

    static {
        // 初始化等级经验表（累计经验值）
        LEVEL_EXP_MAP.put(1, 0L);        // 1级：0经验（起始等级）
        LEVEL_EXP_MAP.put(2, 1000L);     // 2级：1000经验
        LEVEL_EXP_MAP.put(3, 3000L);     // 3级：1000+2000=3000经验
        LEVEL_EXP_MAP.put(4, 7000L);     // 4级：3000+4000=7000经验
        LEVEL_EXP_MAP.put(5, 15000L);    // 5级：7000+8000=15000经验
        LEVEL_EXP_MAP.put(6, 31000L);    // 6级：15000+16000=31000经验
        LEVEL_EXP_MAP.put(7, 81000L);    // 7级：31000+50000=81000经验（满级）
    }

    // ===== 等级特权配置 =====
    private static final Map<Integer, String[]> LEVEL_PRIVILEGES_MAP = new HashMap<>();

    static {
        LEVEL_PRIVILEGES_MAP.put(1, new String[]{"基础功能", "观看作品", "点赞评论"});
        LEVEL_PRIVILEGES_MAP.put(2, new String[]{"基础功能", "观看作品", "点赞评论",
                "上传图片"});
        LEVEL_PRIVILEGES_MAP.put(3, new String[]{"基础功能", "观看作品", "点赞评论",
                "上传图片", "创建合集"});
        LEVEL_PRIVILEGES_MAP.put(4, new String[]{"基础功能", "观看作品", "点赞评论",
                "上传图片",  "创建合集", "专属标识"});
        LEVEL_PRIVILEGES_MAP.put(5, new String[]{"基础功能", "观看作品", "点赞评论",
                "上传图片",  "创建合集",
                "专属标识", "视频上传"});
        LEVEL_PRIVILEGES_MAP.put(6, new String[]{"基础功能", "观看作品", "点赞评论",
                "上传图片",  "创建合集",
                 "专属标识", "视频上传",
                "直播权限", "优先审核"});
        LEVEL_PRIVILEGES_MAP.put(7, new String[]{"基础功能", "观看作品", "点赞评论",
                "上传图片",  "创建合集",
                 "专属标识", "视频上传",
                "直播权限", "优先审核",
                "彩虹头像框", "无限上传"});
    }

    // ===== 经验动作配置（简化，直接在代码中定义）=====
    public static class ExpAction {
        // 动作类型
        public static final String UPLOAD = "upload";          // 上传作品
        public static final String LIKE_RECEIVED = "like_received";  // 收到点赞
        public static final String COMMENT = "comment";        // 发表评论
        public static final String DAILY_LOGIN = "daily_login"; // 每日登录
        public static final String SHARE = "share";            // 分享作品
        public static final String COMPLETE_PROFILE = "complete_profile"; // 完善资料
        public static final String WATCH_VIDEO = "watch_video"; // 观看视频
        public static final String INVITE = "invite";          // 邀请好友
        public static final String VIP_PURCHASE = "vip_purchase"; // 购买VIP

        // 基础经验值配置
        private static final Map<String, Integer> BASE_EXP_VALUES = new HashMap<>();

        static {
            BASE_EXP_VALUES.put(UPLOAD, 5);           // 上传作品 +5经验
            BASE_EXP_VALUES.put(LIKE_RECEIVED, 1);     // 收到点赞 +1经验
            BASE_EXP_VALUES.put(COMMENT, 2);          // 发表评论 +2经验
            BASE_EXP_VALUES.put(DAILY_LOGIN, 10);      // 每日登录 +10经验
            BASE_EXP_VALUES.put(SHARE, 5);            // 分享作品 +5经验
            BASE_EXP_VALUES.put(COMPLETE_PROFILE, 100); // 完善资料 +100经验
            BASE_EXP_VALUES.put(WATCH_VIDEO, 1);       // 观看视频 +1经验
            BASE_EXP_VALUES.put(INVITE, 20);          // 邀请好友 +20经验
            BASE_EXP_VALUES.put(VIP_PURCHASE, 200);    // 购买VIP +200经验
        }

        // 每日上限配置（0表示无限制）
        private static final Map<String, Integer> DAILY_LIMITS = new HashMap<>();

        static {
            DAILY_LIMITS.put(UPLOAD, 10);           // 每天最多10次上传奖励
            DAILY_LIMITS.put(LIKE_RECEIVED, 50);    // 每天最多50次点赞奖励
            DAILY_LIMITS.put(COMMENT, 20);          // 每天最多20次评论奖励
            DAILY_LIMITS.put(DAILY_LOGIN, 1);       // 每天1次登录奖励
            DAILY_LIMITS.put(SHARE, 5);             // 每天最多5次分享奖励
            DAILY_LIMITS.put(WATCH_VIDEO, 30);      // 每天最多30次观看奖励
            DAILY_LIMITS.put(COMPLETE_PROFILE, 1);  // 只有1次完善资料奖励
            DAILY_LIMITS.put(INVITE, 0);            // 无限制邀请奖励
            DAILY_LIMITS.put(VIP_PURCHASE, 0);      // 无限制VIP购买奖励
        }

        /**
         * 获取动作的基础经验值
         */
        public static Integer getBaseExp(String actionType) {
            return BASE_EXP_VALUES.getOrDefault(actionType, 0);
        }

        /**
         * 获取动作的每日上限
         */
        public static Integer getDailyLimit(String actionType) {
            return DAILY_LIMITS.getOrDefault(actionType, 0);
        }

        /**
         * 获取所有动作类型
         */
        public static Map<String, String> getAllActions() {
            Map<String, String> actions = new HashMap<>();
            actions.put(UPLOAD, "上传作品");
            actions.put(LIKE_RECEIVED, "收到点赞");
            actions.put(COMMENT, "发表评论");
            actions.put(DAILY_LOGIN, "每日登录");
            actions.put(SHARE, "分享作品");
            actions.put(COMPLETE_PROFILE, "完善资料");
            actions.put(WATCH_VIDEO, "观看视频");
            actions.put(INVITE, "邀请好友");
            actions.put(VIP_PURCHASE, "购买VIP");
            return actions;
        }
    }

    // ===== 上传限制配置 =====
    public static int DAILY_UPLOAD_COUNT_NORMAL = 20;
    public static int DAILY_UPLOAD_COUNT_VIP_MONTHLY = 50;
    public static int DAILY_UPLOAD_COUNT_VIP_QUARTERLY = 100;
    public static int DAILY_UPLOAD_COUNT_VIP_YEARLY = 200;
    public static int DAILY_UPLOAD_COUNT_VIP_LIFETIME = -1;

    // ===== 彩虹头像框URL（7级专属）=====
    public static final String RAINBOW_FRAME_URL = "/static/frames/rainbow-frame.png";

    // ===== 核心等级计算方法 =====

    /**
     * 获取当前等级所需经验（累计值）
     */
    public static long getRequiredExp(int level) {
        if (level < 1) return 0L;
        if (level > MAX_LEVEL) return LEVEL_EXP_MAP.get(MAX_LEVEL);
        return LEVEL_EXP_MAP.getOrDefault(level, 0L);
    }

    /**
     * 获取下一等级所需经验
     */
    public static long getNextLevelRequiredExp(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0L; // 已是最高级
        return getRequiredExp(currentLevel + 1);
    }

    /**
     * 获取升级到下一级还需要多少经验
     */
    public static long getRemainingExp(long currentExp, int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0L;
        long nextLevelExp = getRequiredExp(currentLevel + 1);
        return Math.max(0, nextLevelExp - currentExp);
    }

    /**
     * 根据经验值计算当前等级
     */
    public static int calculateLevel(long exp) {
        int level = DEFAULT_LEVEL;

        // 遍历所有等级，找到小于等于当前经验的最高等级
        for (int i = DEFAULT_LEVEL + 1; i <= MAX_LEVEL; i++) {
            if (exp >= getRequiredExp(i)) {
                level = i;
            } else {
                break;
            }
        }

        return level;
    }

    /**
     * 获取等级特权
     */
    public static String[] getLevelPrivileges(int level) {
        if (level < 1) return new String[]{"基础功能"};
        if (level > MAX_LEVEL) level = MAX_LEVEL;

        // 获取小于等于当前等级的最高特权配置
        for (int i = level; i >= 1; i--) {
            String[] privileges = LEVEL_PRIVILEGES_MAP.get(i);
            if (privileges != null) {
                return privileges;
            }
        }
        return new String[]{"基础功能"};
    }

    /**
     * 获取经验进度百分比（0-100）
     */
    public static double getExpProgress(long currentExp, int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 100.0;

        long currentLevelExp = getRequiredExp(currentLevel);
        long nextLevelExp = getRequiredExp(currentLevel + 1);

        if (nextLevelExp <= currentLevelExp) {
            return 100.0;
        }

        double progress = (double) (currentExp - currentLevelExp) /
                (nextLevelExp - currentLevelExp) * 100;
        return Math.min(100.0, Math.max(0.0, progress));
    }

    /**
     * 获取头像框URL（7级有彩虹头像框）
     */
    public static String getFrameUrl(int level) {
        if (level == MAX_LEVEL) {
            return RAINBOW_FRAME_URL;
        }
        return null; // 其他等级没有特殊头像框
    }

    /**
     * 是否可以直播（6级以上）
     */
    public static boolean canLiveStream(int level) {
        return level >= 6;
    }

    /**
     * 获取完整的等级信息
     */
    public static Map<String, Object> getLevelInfo(long exp) {
        int level = calculateLevel(exp);

        Map<String, Object> info = new HashMap<>();
        info.put("level", level);
        info.put("maxLevel", MAX_LEVEL);
        info.put("exp", exp);
        info.put("currentLevelExp", getRequiredExp(level));
        info.put("nextLevelExp", getRequiredExp(level + 1));
        info.put("remainingExp", getRemainingExp(exp, level));
        info.put("expProgress", getExpProgress(exp, level));
        info.put("frameUrl", getFrameUrl(level));
        info.put("canLiveStream", canLiveStream(level));
        info.put("privileges", getLevelPrivileges(level));

        // 如果是满级，添加满级标志
        if (level == MAX_LEVEL) {
            info.put("isMaxLevel", true);
            info.put("totalExp", getRequiredExp(MAX_LEVEL));
        } else {
            info.put("isMaxLevel", false);
        }

        return info;
    }

    /**
     * 获取升级所需经验详情（用于显示升级进度）
     */
    public static Map<String, Long> getUpgradeDetails(long currentExp, int currentLevel) {
        Map<String, Long> details = new HashMap<>();

        if (currentLevel >= MAX_LEVEL) {
            details.put("current", 0L);
            details.put("next", 0L);
            details.put("need", 0L);
            return details;
        }

        long currentLevelExp = getRequiredExp(currentLevel);
        long nextLevelExp = getRequiredExp(currentLevel + 1);

        details.put("current", currentLevelExp);
        details.put("next", nextLevelExp);
        details.put("need", nextLevelExp - currentExp);

        return details;
    }

    /**
     * 获取所有等级经验要求（用于前端显示等级进度条）
     */
    public static Map<Integer, Long> getAllLevelExpRequirements() {
        return new HashMap<>(LEVEL_EXP_MAP);
    }

    /**
     * 根据VIP类型获取每日上传限制
     */
    public static int getDailyUploadLimit(Integer vipType) {
        if (vipType == null || VIP_TYPE_NONE.equals(vipType)) {
            return DAILY_UPLOAD_COUNT_NORMAL;
        }
        if (VIP_TYPE_MONTHLY.equals(vipType)) {
            return DAILY_UPLOAD_COUNT_VIP_MONTHLY;
        } else if (VIP_TYPE_QUARTERLY.equals(vipType)) {
            return DAILY_UPLOAD_COUNT_VIP_QUARTERLY;
        } else if (VIP_TYPE_YEARLY.equals(vipType)) {
            return DAILY_UPLOAD_COUNT_VIP_YEARLY;
        } else if (VIP_TYPE_LIFETIME.equals(vipType)) {
            return DAILY_UPLOAD_COUNT_VIP_LIFETIME;
        } else {
            return DAILY_UPLOAD_COUNT_NORMAL;
        }
    }

    /**
     * 是否无限上传
     */
    public static boolean isUnlimitedUpload(Integer vipType) {
        return getDailyUploadLimit(vipType) == -1;
    }
}