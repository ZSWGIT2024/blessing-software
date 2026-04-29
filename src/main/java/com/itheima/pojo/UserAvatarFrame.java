package com.itheima.pojo;

import lombok.Data;

import java.util.Date;


@Data
public class UserAvatarFrame {
    private Integer id;
    private Integer userId;
    private Integer frameId;
    private Boolean isUsing;
    private Date unlockTime;
    private AvatarFrame frameInfo; // 关联对象

}

