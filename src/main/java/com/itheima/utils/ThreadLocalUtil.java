package com.itheima.utils;

import java.util.Map;

public class ThreadLocalUtil {

    // 提供一个threadLocal对象
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 根据键获取值
     */
    public static <T> T get(String key) {
        Map<String, Object> map = THREAD_LOCAL.get();
        return map != null ? (T) map.get(key) : null;
    }

    /**
     * 获取所有数据
     */
    public static Map<String, Object> get() {
        return THREAD_LOCAL.get();
    }

    /**
     * 设置键值对
     */
    public static void set(String key, Object value) {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new java.util.HashMap<>();
            THREAD_LOCAL.set(map);
        }
        map.put(key, value);
    }

    /**
     * 设置整个Map
     */
    public static void set(Map<String, Object> value) {
        THREAD_LOCAL.set(value);
    }

    /**
     * 获取用户ID
     */
    public static Integer getUserId() {
        return get("id");
    }

    /**
     * 获取用户手机号
     */
    public static String getPhone() { return get("phone"); }

    /**
     * 获取用户名
     */
    public static String getUsername() {
        return get("username");
    }

    /**
     * 获取用户邮箱
     */
    public static String getEmail() { return get("email"); }

    /**
     * 获取用户类型
     */
    public static Integer getUserType() {
        return get("userType");
    }

    /**
     * 获取用户昵称
     */
    public static String getNickname() {
        return get("nickname");
    }

    /**
     * 获取用户状态
     */
    public static String getStatus() {
        return get("status");
    }

    /**
     * 检查是否为管理员
     */
    public static boolean isAdmin() {
        Integer userType = getUserType();
        return userType != null && userType.equals(com.itheima.common.UserConstant.USER_TYPE_ADMIN);
    }

    /**
     * 检查是否已登录
     */
    public static boolean isLoggedIn() {
        return getUserId() != null;
    }

    /**
     * 清除ThreadLocal中的数据，防止内存泄漏
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}