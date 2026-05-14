package com.itheima.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemMessageDTO {
    private String messageId;
    private String title;
    private String content;
    private String messageType;
    private String targetType;
    private String targetId;
    private Integer senderId;
    private Boolean isActive;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
