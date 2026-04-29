package com.itheima.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendApplyDTO {
    private Long id;
    private Integer applicantId;
    private String applicantUsername;
    private String applicantNickname;
    private String applicantAvatar;
    private String applicantAvatarFrame;
    private Integer receiverId;
    private String receiverUsername;
    private String receiverNickname;
    private String receiverAvatar;
    private String receiverAvatarFrame;
    private String applyMsg;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
