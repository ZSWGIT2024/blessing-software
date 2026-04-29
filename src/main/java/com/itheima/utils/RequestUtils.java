package com.itheima.utils;

import com.itheima.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

public class RequestUtils {

    /**
     * 获取当前登录用户ID
     */
    public static Integer getCurrentUserId() {
        return ThreadLocalUtil.getUserId();
    }

    public static String getCurrentPhone() { return ThreadLocalUtil.getPhone(); }

    /**
     * 获取当前登录用户的用户名
     */
    public static String getCurrentUsername() {
        return ThreadLocalUtil.getUsername();
    }

    /**
     * 获取当前登录用户的邮箱
     */
    public static String getCurrentEmail() { return ThreadLocalUtil.getEmail(); }

    /**
     * 获取当前登录用户昵称
     */
    public static String getCurrentNickname() {
        return ThreadLocalUtil.getNickname();
    }

    /**
     * 获取当前登录用户类型
     */
    public static Integer getCurrentUserType() {
        return ThreadLocalUtil.getUserType();
    }

    /**
     * 获取当前登录用户状态
     */
    public static String getCurrentUserStatus() {
        return ThreadLocalUtil.getStatus();
    }

    /**
     * 获取当前登录用户信息（完整对象）
     */
    public static User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }

        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            return null;
        }

        User user = new User();
        user.setId(getCurrentUserId());
        user.setPhone(getCurrentPhone());
        user.setUsername(getCurrentUsername());
        user.setNickname(getCurrentNickname());
        user.setUserType(getCurrentUserType());
        user.setStatus(getCurrentUserStatus());

        // 根据需要设置其他字段
        user.setAvatar(ThreadLocalUtil.get("avatar"));
        user.setEmail(ThreadLocalUtil.get("email"));

        return user;
    }

    /**
     * 检查当前用户是否是管理员
     */
    public static boolean isAdmin() {
        return ThreadLocalUtil.isAdmin();
    }

    /**
     * 检查当前用户是否已登录
     */
    public static boolean isLoggedIn() {
        return ThreadLocalUtil.isLoggedIn();
    }

    /**
     * 检查当前用户状态是否正常
     */
    public static boolean isUserActive() {
        String status = getCurrentUserStatus();
        return com.itheima.common.UserConstant.STATUS_ACTIVE.equals(status);
    }

    /**
     * 获取HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP
     */
    public static String getClientIp() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return getClientIp(request);
    }

    /**
     * 获取客户端IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 获取User-Agent
     */
    public static String getUserAgent() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    /**
     * 获取请求路径
     */
    public static String getRequestURI() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getRequestURI() : null;
    }

    /**
     * 获取请求方法
     */
    public static String getMethod() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getMethod() : null;
    }
}