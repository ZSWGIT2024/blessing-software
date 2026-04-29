package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendApply {
    private Long id;
    private Integer applicantId;     // 申请者ID
    private Integer receiverId;      // 接收者ID
    private String applyMsg;         // 申请消息
    private String status;           // 状态：pending, accepted, rejected, expired
    private User applicant;          // 申请者信息
    private User receiver;           // 接收者信息

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}