package com.itheima.common;

/**
 * 关注与好友相关常量
 */
public class RelationConstant {

    // 关注类型
    public static final String FOLLOW_TYPE_NORMAL = "normal";// 普通关注
    public static final String FOLLOW_TYPE_SPECIAL = "special";// 特别关注

    // 好友申请状态
    public static final String APPLY_STATUS_PENDING = "pending";
    public static final String APPLY_STATUS_ACCEPTED = "accepted";
    public static final String APPLY_STATUS_REJECTED = "rejected";

    // 好友申请过期时间（天）
    public static final int APPLY_EXPIRE_DAYS = 7;


    // 最大好友数量限制
    public static final int MAX_FRIENDS_NORMAL = 1000;      // 普通用户
    public static final int MAX_FRIENDS_VIP = -1;        // VIP用户无限制

    // 最大关注数量限制
    public static final int MAX_FOLLOWING_NORMAL = 1000;   // 普通用户
    public static final int MAX_FOLLOWING_VIP = -1;      // VIP用户无限制
}