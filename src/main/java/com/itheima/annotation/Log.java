package com.itheima.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在Controller方法上，用于记录操作日志
 *
 * 使用示例：
 * @Log(module = "用户管理", operation = "删除用户", type = LogType.DELETE)
 * public void deleteUser(Integer userId) { ... }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作描述
     */
    String operation() default "";

    /**
     * 操作类型
     */
    LogType type() default LogType.OTHER;

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean recordResult() default false;

    /**
     * 是否脱敏处理
     */
    boolean desensitize() default true;

    /**
     * 排除的参数名（不记录）
     */
    String[] excludeParams() default {};
}
