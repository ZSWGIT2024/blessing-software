package com.itheima.dto;

import lombok.Data;

@Data
public class UpdateGroupDTO {
    private String groupId;
    private String name;
    private String avatar;
    private String description;
    private Integer maxMembers;
    private Boolean isMutedAll;
    private String joinPermission;
}
