package com.itheima.dto;

import lombok.Data;

@Data
public class AvatarFrameWithStatusDTO {
    private Integer id;
    private String name;
    private String url;
    private String previewUrl;
    private Integer requiredLevel;
    private Integer requiredVipType;
    private Integer requiredDays;
    private String description;
    private Boolean unlocked; // 是否已解锁
    private Boolean isUsing;  // 是否正在使用

    // 可以根据需要添加其他字段
}

