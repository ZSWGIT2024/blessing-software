package com.itheima.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务（基于 Hutool + Redis）
 * <p>
 * 生成线段干扰验证码，存入 Redis（key=captcha:uuid），返回 uuid + base64 图片。
 * 验证码有效期 5 分钟，一次性消费。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CAPTCHA_KEY = "captcha:";
    private static final long CAPTCHA_TTL = 5; // 5分钟

    /** 生成验证码，返回 {uuid, base64Image} */
    public Map<String, String> generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 80, 4, 50);
        String code = captcha.getCode();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // hutool getImageBase64Data() 已自带 "data:image/png;base64," 前缀
        String imageData = captcha.getImageBase64Data();

        redisTemplate.opsForValue().set(CAPTCHA_KEY + uuid, code, CAPTCHA_TTL, TimeUnit.MINUTES);
        log.debug("生成图形验证码: uuid={}", uuid);

        Map<String, String> result = new HashMap<>();
        result.put("uuid", uuid);
        result.put("image", imageData);
        return result;
    }

    /** 校验验证码，校验通过后删除 */
    public boolean verify(String uuid, String code) {
        if (uuid == null || code == null) return false;
        String key = CAPTCHA_KEY + uuid;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) return false;
        boolean match = code.equalsIgnoreCase(cached.toString().trim());
        if (match) redisTemplate.delete(key);
        return match;
    }
}
