package com.itheima.dto;

import com.itheima.common.UserConstant;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
public class UserSimpleDTO {
    private Integer id;
    @Pattern(regexp = "^\\S{2,18}$")
    private String username;
    @Pattern(regexp = "^\\S{2,18}$")
    private String nickname;
    private String email;
    private String avatar;
    private String avatarFrame;
    private String bio;
    private Integer level;
    private Long exp;
    private Long nextLevelExp;
    private Integer vipType;
    private String gender;
    private LocalDate birthday;
    private String hobbies;
    private String favoriteThings;
    private String bloodType;
    private String constellation;
    private Integer coinBalance;
    private Integer totalLoginDays;


    // 社交相关
    private Integer uploadCount;
    private Integer likedCount;
    private Integer followCount;
    private Integer followerCount;
    private Boolean isFollowed; // 是否已关注
    private Boolean isFriend;   // 是否好友

    // 在线状态
    @Pattern(regexp = "^(online|offline)$")
    private String isOnline = UserConstant.OFFLINE;

    // 用户状态
    @Pattern(regexp = "^(active|banned|muted)$")
    private String status = UserConstant.STATUS_ACTIVE;

    // 时间戳
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}