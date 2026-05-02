package com.itheima.exception;

/**
 * 权限异常
 * 当用户权限不足时抛出
 */
public class PermissionDeniedException extends RuntimeException {

    private String permission;

    public PermissionDeniedException(String message) {
        super(message);
    }

    public PermissionDeniedException(String permission, String message) {
        super(message);
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}
