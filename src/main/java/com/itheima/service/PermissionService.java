package com.itheima.service;

import com.itheima.common.UserConstant;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.User;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 权限服务
 * 提供权限校验、资源所有权校验等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserMapper userMapper;

    /**
     * 权限定义
     * Key: 权限标识
     * Value: 允许访问的用户类型列表
     */
    private static final Map<String, List<Integer>> PERMISSION_DEFINITION = new HashMap<>();

    static {
        // 用户管理权限 - 仅管理员
        PERMISSION_DEFINITION.put("user:list", List.of(UserConstant.USER_TYPE_ADMIN));
        PERMISSION_DEFINITION.put("user:ban", List.of(UserConstant.USER_TYPE_ADMIN));
        PERMISSION_DEFINITION.put("user:delete", List.of(UserConstant.USER_TYPE_ADMIN));
        PERMISSION_DEFINITION.put("user:edit", List.of(UserConstant.USER_TYPE_ADMIN));
        PERMISSION_DEFINITION.put("user:vip", List.of(UserConstant.USER_TYPE_ADMIN));
        PERMISSION_DEFINITION.put("user:statistics", List.of(UserConstant.USER_TYPE_ADMIN));

        // 内容管理权限
        PERMISSION_DEFINITION.put("content:audit", List.of(UserConstant.USER_TYPE_ADMIN));
        PERMISSION_DEFINITION.put("content:delete", List.of(UserConstant.USER_TYPE_ADMIN, UserConstant.USER_TYPE_NORMAL));

        // 系统管理权限 - 仅管理员
        PERMISSION_DEFINITION.put("system:config", List.of(UserConstant.USER_TYPE_ADMIN));
        PERMISSION_DEFINITION.put("system:log", List.of(UserConstant.USER_TYPE_ADMIN));
        PERMISSION_DEFINITION.put("system:monitor", List.of(UserConstant.USER_TYPE_ADMIN));

        // 普通用户权限
        PERMISSION_DEFINITION.put("profile:view", List.of(UserConstant.USER_TYPE_ADMIN, UserConstant.USER_TYPE_NORMAL));
        PERMISSION_DEFINITION.put("profile:edit", List.of(UserConstant.USER_TYPE_ADMIN, UserConstant.USER_TYPE_NORMAL));
        PERMISSION_DEFINITION.put("upload:create", List.of(UserConstant.USER_TYPE_ADMIN, UserConstant.USER_TYPE_NORMAL));
    }

    /**
     * 检查当前用户是否有指定权限
     * @param permission 权限标识
     * @return 是否有权限
     */
    public boolean hasPermission(String permission) {
        try {
            // 获取当前用户信息
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims == null) {
                log.warn("无法获取当前用户信息");
                return false;
            }

            Integer userType = (Integer) claims.get("userType");
            if (userType == null) {
                log.warn("无法获取用户类型");
                return false;
            }

            // 管理员拥有所有权限
            if (UserConstant.USER_TYPE_ADMIN.equals(userType)) {
                return true;
            }

            // 检查权限定义
            List<Integer> allowedTypes = PERMISSION_DEFINITION.get(permission);
            if (allowedTypes == null) {
                // 未定义的权限，默认需要管理员
                log.warn("未定义的权限：{}", permission);
                return false;
            }

            return allowedTypes.contains(userType);

        } catch (Exception e) {
            log.error("权限检查失败", e);
            return false;
        }
    }

    /**
     * 检查是否是管理员
     * @return 是否是管理员
     */
    public boolean isAdmin() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims == null) {
                return false;
            }
            Integer userType = (Integer) claims.get("userType");
            return UserConstant.USER_TYPE_ADMIN.equals(userType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public Integer getCurrentUserId() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims == null) {
                return null;
            }
            return (Integer) claims.get("id");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查资源所有权
     * @param resourceUserId 资源所属用户ID
     * @return 是否有权限
     */
    public boolean checkOwnership(Integer resourceUserId) {
        // 管理员可以操作所有资源
        if (isAdmin()) {
            return true;
        }

        // 检查是否是资源所有者
        Integer currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(resourceUserId);
    }

    /**
     * 检查用户状态
     * @param userId 用户ID
     * @return 用户是否正常
     */
    public boolean isUserActive(Integer userId) {
        try {
            User user = userMapper.findUserById(userId);
            return user != null && UserConstant.STATUS_ACTIVE.equals(user.getStatus());
        } catch (Exception e) {
            log.error("检查用户状态失败", e);
            return false;
        }
    }

    /**
     * 获取当前用户信息
     * @return 用户信息Map
     */
    public Map<String, Object> getCurrentUserInfo() {
        return ThreadLocalUtil.get();
    }

    /**
     * 获取用户所有权限列表
     * @param userType 用户类型
     * @return 权限列表
     */
    public List<String> getUserPermissions(Integer userType) {
        List<String> permissions = new ArrayList<>();

        for (Map.Entry<String, List<Integer>> entry : PERMISSION_DEFINITION.entrySet()) {
            if (entry.getValue().contains(userType)) {
                permissions.add(entry.getKey());
            }
        }

        return permissions;
    }

    /**
     * 检查是否是VIP用户
     * @param userId 用户ID
     * @return 是否是VIP
     */
    public boolean isVip(Integer userId) {
        try {
            User user = userMapper.findUserById(userId);
            return user != null && user.isVip();
        } catch (Exception e) {
            return false;
        }
    }
}
