package com.itheima.annotation;

import java.lang.annotation.*;

/**
 * 管理员权限注解
 * 标注在Controller方法上，要求管理员权限才能访问
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAdmin {

    /**
     * 权限描述
     */
    String description() default "需要管理员权限";
}
