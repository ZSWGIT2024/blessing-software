package com.itheima.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务（QQ 邮箱 SMTP 接入点）
 * <p>
 * 当前为开发测试模式，仅日志打印验证码。
 * 正式上线前需配置 QQ 邮箱 SMTP 授权码，取消下方注释并填入真实配置。
 * </p>
 *
 * <pre>
 * 配置步骤：
 * 1. pom.xml 添加依赖：
 *    &lt;dependency&gt;
 *        &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
 *        &lt;artifactId&gt;spring-boot-starter-mail&lt;/artifactId&gt;
 *    &lt;/dependency&gt;
 * 2. application.yml 配置：
 *    spring:
 *      mail:
 *        host: smtp.qq.com
 *        port: 587
 *        username: your_qq_email@qq.com
 *        password: your_qq_smtp_authorization_code
 *        properties:
 *          mail.smtp.auth: true
 *          mail.smtp.starttls.enable: true
 * 3. QQ邮箱 → 设置 → 账户 → POP3/SMTP服务 → 开启 → 获取授权码
 * 4. 取消下方 send 方法中的注释，注入 JavaMailSender 即可使用。
 * </pre>
 */
@Slf4j
@Service
public class EmailService {

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @param code  验证码
     * @param type  类型：login/register/reset
     */
    public boolean send(String email, String code, String type) {
        String subject;
        String content;
        switch (type) {
            case "login":
                subject = "惠天下 - 登录验证码";
                content = buildHtml("登录验证码", code, "5分钟");
                break;
            case "register":
                subject = "惠天下 - 注册验证码";
                content = buildHtml("注册验证码", code, "10分钟");
                break;
            case "reset":
                subject = "惠天下 - 密码重置验证码";
                content = buildHtml("密码重置验证码", code, "10分钟");
                break;
            default:
                subject = "惠天下 - 验证码";
                content = buildHtml("验证码", code, "5分钟");
        }

        // 开发测试阶段，打印到控制台
        log.info("【邮件】发送至 {}: subject={}, code={}", email, subject, code);
        return true;

        // ========== JavaMailSender 接入代码（正式环境取消注释） ==========
        // try {
        //     org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
        //     message.setFrom(username);
        //     message.setTo(email);
        //     message.setSubject(subject);
        //     message.setText(content.replaceAll("<[^>]+>", ""));
        //     javaMailSender.send(message);
        //     return true;
        // } catch (Exception e) {
        //     log.error("邮件发送失败: email={}", email, e);
        //     return false;
        // }
    }

    /** 生成 HTML 格式的验证码邮件内容 */
    private String buildHtml(String type, String code, String validity) {
        return "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;padding:20px;border:1px solid #eee;border-radius:8px;'>"
                + "<h2 style='color:#ff69b4;text-align:center;'>惠天下</h2>"
                + "<p style='font-size:16px;'>您的" + type + "是：</p>"
                + "<div style='font-size:28px;font-weight:bold;text-align:center;letter-spacing:8px;padding:16px;background:#fff0f5;border-radius:8px;margin:16px 0;'>" + code + "</div>"
                + "<p style='color:#999;'>请在" + validity + "内使用，请勿泄露给他人。</p>"
                + "<hr style='border:none;border-top:1px solid #eee;'/>"
                + "<p style='color:#bbb;font-size:12px;'>如非本人操作，请忽略此邮件。</p>"
                + "</div>";
    }
}
