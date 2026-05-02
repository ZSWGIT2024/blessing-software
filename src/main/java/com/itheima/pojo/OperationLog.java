package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
public class OperationLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 模块名称
     */
    private String module;

    /**
     * 操作描述
     */
    private String operation;

    /**
     * 操作类型：LOGIN, LOGOUT, INSERT, UPDATE, DELETE, SELECT等
     */
    private String type;

    /**
     * 请求方法：GET, POST, PUT, DELETE
     */
    private String method;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * 请求参数（JSON格式，已脱敏）
     */
    private String requestParams;

    /**
     * 响应结果
     */
    private String responseResult;

    /**
     * 操作用户ID
     */
    private Integer userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 执行时长（毫秒）
     */
    private Integer duration;

    /**
     * 操作状态：success, fail
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
