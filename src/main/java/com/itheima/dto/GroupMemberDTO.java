package com.itheima.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMemberDTO {
    private Integer userId;
    private String username;
    private String nickname;
    private String avatar;
    private String avatarFrame;
    private String role;
    private String nicknameInGroup;
    private Boolean isMuted;
    private LocalDateTime mutedUntil;
    private String isOnline;
    private LocalDateTime joinTime;
    private Integer level;
    private Integer vipType;
}
