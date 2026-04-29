package com.itheima.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class AvatarFrame {
    private Integer id;
    private String name;
    private String url;
    private String previewUrl;
    private Integer requiredLevel;
    private Integer requiredVipType;
    private Integer requiredDays;
    private Boolean isDefault;
    private String description;
    private Date createTime;
    private Date updateTime;

}

