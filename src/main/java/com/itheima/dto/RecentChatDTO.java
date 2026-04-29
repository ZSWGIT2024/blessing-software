package com.itheima.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecentChatDTO {
    private Integer relatedUserId;
    private Integer userId;
    private String username;
    private String nickname;
    private String avatar;
    private String avatarFrame;
    private String lastMessage;
    private LocalDateTime lastTime;
    private Integer unreadCount;
    private Boolean isFriend;
    private String remark;
    private Boolean isStarred;
    private Boolean isFollowing;
    private String isOnline;
}
