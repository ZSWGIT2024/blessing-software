package com.itheima.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 短信发送服务（阿里云 SMS 接入点）
 * <p>
 * 当前为开发测试模式，仅日志打印验证码。
 * 正式上线前需在阿里云控制台申请签名和模板，取消下方注释并填入真实配置。
 * </p>
 *
 * <pre>
 * 阿里云 SMS 接入步骤：
 * 1. pom.xml 添加依赖：
 *    &lt;dependency&gt;
 *        &lt;groupId&gt;com.aliyun&lt;/groupId&gt;
 *        &lt;artifactId&gt;dysmsapi20170525&lt;/artifactId&gt;
 *        &lt;version&gt;2.0.24&lt;/version&gt;
 *    &lt;/dependency&gt;
 * 2. application.yml 配置：
 *    aliyun:
 *      sms:
 *        access-key-id: your_key
 *        access-key-secret: your_secret
 *        sign-name: 惠天下
 *        template-code: SMS_XXXXXXXXX
 * 3. 取消下方 send 方法中的注释，注入配置并调用阿里云 SDK。
 * </pre>
 */
@Slf4j
@Service
public class SmsService {

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param code  验证码
     * @param type  类型：login/register/reset
     */
    public boolean send(String phone, String code, String type) {
        String content;
        switch (type) {
            case "login":
                content = String.format("【惠天下】您的登录验证码是：%s，5分钟内有效。请勿泄露给他人。", code);
                break;
            case "register":
                content = String.format("【惠天下】您的注册验证码是：%s，10分钟内有效。请勿泄露给他人。", code);
                break;
            case "reset":
                content = String.format("【惠天下】您正在重置密码，验证码是：%s，10分钟内有效。", code);
                break;
            default:
                content = String.format("【惠天下】您的验证码是：%s，请勿泄露给他人。", code);
        }

        // 开发测试阶段，打印到控制台
        log.info("【短信】发送至 {}: {}", phone, content);
        return true;

        // ========== 阿里云短信 SDK 接入代码（正式环境取消注释） ==========
        // try {
        //     com.aliyun.dysmsapi20170525.Client client = createClient();
        //     com.aliyun.dysmsapi20170525.models.SendSmsRequest request =
        //         new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
        //             .setPhoneNumbers(phone)
        //             .setSignName(signName)
        //             .setTemplateCode(templateCode)
        //             .setTemplateParam("{\"code\":\"" + code + "\"}");
        //     client.sendSms(request);
        //     return true;
        // } catch (Exception e) {
        //     log.error("阿里云短信发送失败: phone={}", phone, e);
        //     return false;
        // }
    }

    // private com.aliyun.dysmsapi20170525.Client createClient() throws Exception {
    //     com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
    //         .setAccessKeyId(accessKeyId)
    //         .setAccessKeySecret(accessKeySecret);
    //     config.endpoint = "dysmsapi.aliyuncs.com";
    //     return new com.aliyun.dysmsapi20170525.Client(config);
    // }
}
