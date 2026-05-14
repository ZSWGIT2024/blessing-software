package com.itheima.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupListDTO {
    private String groupId;
    private String name;
    private String avatar;
    private Integer ownerId;
    private String ownerName;
    private Integer currentMembers;
    private Integer maxMembers;
    private String myRole;
    private Integer unreadCount;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
}
