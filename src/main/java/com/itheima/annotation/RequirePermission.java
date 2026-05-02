package com.itheima.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 标注在Controller方法或Service方法上，用于权限控制
 *
 * 使用示例：
 * @RequirePermission("user:delete")
 * public void deleteUser(Integer userId) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限标识
     * 例如：user:list, user:create, user:delete
     */
    String value();

    /**
     * 权限描述（用于日志和提示）
     */
    String description() default "";

    /**
     * 是否需要资源所有权校验
     * 为true时，会额外检查用户是否有权操作该资源
     */
    boolean checkOwnership() default false;

    /**
     * 资源类型（配合checkOwnership使用）
     */
    String resourceType() default "";
}
