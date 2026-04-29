package com.itheima.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * MD5密码加密工具类
 * 提供密码加密和验证功能
 */
public class Md5Util {

    /**
     * 生成随机的盐值
     * @return 16字节的随机盐值（Base64编码）
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * MD5加密（不带盐值）
     * @param password 明文密码
     * @return 加密后的密码
     */
    public static String md5Encrypt(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(password.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不存在", e);
        }
    }

    /**
     * MD5加密（带盐值）
     * @param password 明文密码
     * @param salt 盐值
     * @return 加密后的密码
     */
    public static String md5EncryptWithSalt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 将密码和盐值组合
            String passwordWithSalt = password + salt;
            byte[] digest = md.digest(passwordWithSalt.getBytes());
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不存在", e);
        }
    }

    /**
     * 验证密码（不带盐值）
     * @param inputPassword 用户输入的密码
     * @param storedPassword 存储的加密密码
     * @return 验证结果
     */
    public static boolean verify(String inputPassword, String storedPassword) {
        String encryptedInput = md5Encrypt(inputPassword);
        return encryptedInput.equals(storedPassword);
    }

    /**
     * 验证密码（带盐值）
     * @param inputPassword 用户输入的密码
     * @param storedPassword 存储的加密密码
     * @param salt 盐值
     * @return 验证结果
     */
    public static boolean verifyWithSalt(String inputPassword, String storedPassword, String salt) {
        String encryptedInput = md5EncryptWithSalt(inputPassword, salt);
        return encryptedInput.equals(storedPassword);
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 生成加盐的密码（用于新用户注册或密码修改）
     * @param password 明文密码
     * @return 包含加密密码和盐值的对象
     */
    public static PasswordSalt generateSaltedPassword(String password) {
        String salt = generateSalt();
        String encryptedPassword = md5EncryptWithSalt(password, salt);
        return new PasswordSalt(encryptedPassword, salt);
    }

    /**
     * 内部类：用于返回加密密码和盐值
     */
    public static class PasswordSalt {
        private final String password;
        private final String salt;

        public PasswordSalt(String password, String salt) {
            this.password = password;
            this.salt = salt;
        }

        public String getPassword() {
            return password;
        }

        public String getSalt() {
            return salt;
        }
    }

    // 测试示例
    public static void main(String[] args) {
        // 测试不带盐值的MD5加密
        String password = "123456";
        String encrypted = md5Encrypt(password);
        System.out.println("原始密码: " + password);
        System.out.println("MD5加密: " + encrypted);
        System.out.println("验证结果: " + verify(password, encrypted));

        System.out.println("====================================");

        // 测试带盐值的MD5加密
        PasswordSalt passwordSalt = generateSaltedPassword(password);
        System.out.println("加盐加密密码: " + passwordSalt.getPassword());
        System.out.println("盐值: " + passwordSalt.getSalt());
        System.out.println("加盐验证结果: " +
                verifyWithSalt(password, passwordSalt.getPassword(), passwordSalt.getSalt()));
    }
}