package com.itheima.anno;

import com.itheima.validation.StateValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotEmpty;

import java.lang.annotation.*;


@Documented//元注解 表示该注解应该被包含在javadoc中
@Target({ElementType.FIELD})//元注解 表示该注解只能用于字段上
@Retention(RetentionPolicy.RUNTIME)//元注解 表示该注解在运行时可用
@Constraint(validatedBy = {StateValidation.class})//指定提供校验规则的类
public @interface State {

    //提供校验失败时的提示信息
    String message() default "state参数的值只能是“已发布”或者“草稿”！";
    //指定分组
    Class<?>[] groups() default {};
    //指定负载  获取到state注解的附加信息
    Class<? extends Payload>[] payload() default {};
}
