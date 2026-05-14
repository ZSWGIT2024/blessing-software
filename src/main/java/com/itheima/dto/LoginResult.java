package com.itheima.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoginResult {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int accessTokenExpireIn;
    private Integer userId;
    private String phone;
    private String username;
    private String nickname;
    private String avatar;
    private Integer userType;
    private String status;
    private String location;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginDays;
    private Integer totalLoginDays;
    private Integer level;
    private Long exp;
    private String isOnline;

    // VIP fields (nullable)
    private Integer vipType;
    private LocalDateTime vipExpireTime;
    private Long vipRemainingDays;
}
