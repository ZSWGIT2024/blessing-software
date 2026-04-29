package com.itheima.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注用户信息VO
 */
@Data
public class FollowUserVO {
    private Integer id;
    private String username;
    private String nickname;
    private String avatar;
    private String gender;
    private String bio;
    private Integer level;
    private Boolean isVip;
    private String remark;           // 备注名
    private LocalDateTime followTime; // 关注时间
}