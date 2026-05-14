package com.itheima.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupInfoDTO {
    private String groupId;
    private String name;
    private String avatar;
    private String description;
    private Integer ownerId;
    private String ownerName;
    private String ownerAvatar;
    private Integer maxMembers;
    private Integer currentMembers;
    private String joinPermission;
    private Boolean isMutedAll;
    private String inviteCode;
    private LocalDateTime inviteExpireTime;
    private Boolean isDissolved;
    private String myRole;
    private Boolean isMuted;
    private Integer unreadCount;
    private LocalDateTime createTime;
    private GroupMessageDTO lastMessage;
}
