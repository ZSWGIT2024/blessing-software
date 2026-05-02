package com.itheima.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt密码加密工具类
 * 提供更安全的密码加密和验证功能
 * 相比MD5，BCrypt具有以下优势：
 * 1. 内置盐值，每次加密结果不同
 * 2. 可调节计算成本，对抗暴力破解
 * 3. 慢速哈希，增加破解难度
 */
@Slf4j
public class BCryptUtil {

    /**
     * BCrypt加密器实例
     * 强度默认为10，可在配置中调整
     * 强度越高越安全，但耗时越长
     */
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    /**
     * 加密密码
     * @param rawPassword 明文密码
     * @return 加密后的密码（包含盐值）
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        String encoded = encoder.encode(rawPassword);
        log.debug("密码加密成功");
        return encoded;
    }

    /**
     * 验证密码
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库存储的加密密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 判断密码是否需要升级
     * 当BCrypt强度调整后，可用来判断是否需要重新加密
     * @param encodedPassword 已加密的密码
     * @param strength 目标强度
     * @return 是否需要升级
     */
    public static boolean upgradeEncoding(String encodedPassword, int strength) {
        return encoder.upgradeEncoding(encodedPassword);
    }

    /**
     * 判断是否是BCrypt加密的密码
     * BCrypt密码格式：$2a$10$...
     * @param password 密码字符串
     * @return 是否是BCrypt格式
     */
    public static boolean isBCryptPassword(String password) {
        if (password == null) {
            return false;
        }
        // BCrypt密码以$2a$或$2b$或$2y$开头
        return password.startsWith("$2a$") ||
               password.startsWith("$2b$") ||
               password.startsWith("$2y$");
    }

    /**
     * 生成随机密码
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        if (length < 6) {
            length = 6;
        }
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.security.SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
