package com.itheima.annotation;

/**
 * 日志类型枚举
 */
public enum LogType {
    LOGIN("登录"),
    LOGOUT("登出"),
    INSERT("新增"),
    UPDATE("更新"),
    DELETE("删除"),
    SELECT("查询"),
    UPLOAD("上传"),
    DOWNLOAD("下载"),
    EXPORT("导出"),
    IMPORT("导入"),
    OTHER("其他");

    private final String description;

    LogType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
