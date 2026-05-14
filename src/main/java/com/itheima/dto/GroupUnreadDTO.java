package com.itheima.dto;

import lombok.Data;

@Data
public class GroupUnreadDTO {
    private String groupId;
    private Integer unreadCount;
}
