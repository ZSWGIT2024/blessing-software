package com.itheima.config;

import com.itheima.interceptors.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录和注册不需要拦截
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns("/user/register", "/user/login", "/user/sendSMSCode", "/user/loginByCode");
    }
}
