package com.itheima;


import com.itheima.config.JwtProperties;
import com.itheima.config.MediaProperties;
import com.itheima.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Arrays;


@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({
        MediaProperties.class,
        OssProperties.class,
        JwtProperties.class
})
public class BlessingSoftwareApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BlessingSoftwareApplication.class, args);
        Environment env = context.getEnvironment();
        String[] activeProfiles = env.getActiveProfiles();
        System.out.println("==================== 环境信息 ====================");
        System.out.println("激活的 Profile: " + Arrays.toString(activeProfiles));
        System.out.println("数据库 URL: " + env.getProperty("spring.datasource.url"));
        System.out.println("OSS 是否启用: " + env.getProperty("oss.endpoint"));
        System.out.println("敏感词过滤: " + env.getProperty("security.sensitive.enabled"));
        System.out.println("==================================================");
        log.info("BlessingSoftwareApplication  启动成功");
    }
}
