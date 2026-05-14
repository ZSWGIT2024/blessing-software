package com.itheima.dto;

import lombok.Data;

@Data
public class CreateGroupDTO {
    private String name;
    private String avatar;
    private String description;
    private Integer maxMembers;
    private String joinPermission;
}
