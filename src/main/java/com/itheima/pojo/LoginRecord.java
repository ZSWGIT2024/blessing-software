package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录记录实体
 */
@Data
public class LoginRecord {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 登录类型：password, code, token
     */
    private String loginType;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 设备信息（User-Agent）
     */
    private String deviceInfo;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 登录状态：success, fail
     */
    private String status;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 登录时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /**
     * 登出时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime logoutTime;

    /**
     * 在线时长（秒）
     */
    private Long onlineDuration;
}
