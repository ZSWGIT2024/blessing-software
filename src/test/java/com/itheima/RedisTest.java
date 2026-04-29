package com.itheima;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;


@SpringBootTest(classes = BlessingSoftwareApplication.class)//如果在测试类上添加这个注解，那么将来单元测试方法执行之前会初始化Spring容器
public class RedisTest {


    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Test
    public void testRedis() {
        //往Redis中存储一个键值对，StringRedisTemplate或redisTemplate(推荐)
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        operations.set("userName", "zhangsan");
        operations.set("age", "18", 15, TimeUnit.SECONDS);//设置过期时间
        String name = operations.get("userName");
        String age = operations.get("age");
        System.out.println(name);
        System.out.println(age);
    }
}
