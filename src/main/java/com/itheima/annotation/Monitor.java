package com.itheima.annotation;

import java.lang.annotation.*;

/**
 * 接口性能监控注解
 * 标注在方法上，用于监控接口执行性能
 *
 * 使用示例：
 * @Monitor(threshold = 1000) // 超过1秒记录慢日志
 * public List<User> getUserList() { ... }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Monitor {

    /**
     * 接口名称（默认使用方法名）
     */
    String name() default "";

    /**
     * 慢接口阈值（毫秒）
     * 超过此时间会记录慢日志
     */
    long threshold() default 1000;

    /**
     * 是否记录详细性能信息
     */
    boolean detailed() default false;
}
