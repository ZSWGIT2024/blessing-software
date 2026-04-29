package com.itheima.utils;

import com.itheima.common.UserConstant;
import com.itheima.pojo.User;

// 权限检查工具类
public class PermissionUtil {

    /**
     * 检查用户是否有管理员权限
     */
    public static boolean hasAdminPermission(User user) {
        return user != null &&
                UserConstant.USER_TYPE_ADMIN.equals(user.getUserType()) &&
                UserConstant.STATUS_ACTIVE.equals(user.getStatus());
    }

    /**
     * 检查用户是否有上传权限
     */
    public static boolean hasUploadPermission(User user) {
        if (user == null || UserConstant.STATUS_BANNED.equals(user.getStatus())) {
            return false;
        }
        return true;
    }

    /**
     * 检查用户是否有VIP特权
     */
    public static boolean hasVipPrivilege(User user, String privilege) {
        if (user == null || !user.isVip()) {
            return false;
        }
        // 可以根据不同的VIP类型返回不同的特权
        return true;
    }
}