package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itheima.common.UserConstant;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class User {
    @NotNull
    private Integer id;

    @Pattern(regexp = "^\\S{2,18}$")
    private String username;

    @JsonIgnore
    @Pattern(regexp = "^[a-zA-Z0-9]{5,16}$")
    private String password;

    @NotNull
    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;

    @NotEmpty
    @Pattern(regexp = "^\\S{2,18}$")
    private String nickname;

    @NotEmpty
    @Email
    private String email;

    private String avatar;
    private String bio;
    private String gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    private String hobbies;
    private String favoriteThings;
    private String bloodType;
    private String constellation;

    //是否在线
    @Pattern(regexp = "^(online|offline)$")
    private String isOnline = UserConstant.OFFLINE;

    // === 权限相关 ===

    @Min(0)
    @Max(1)
    private Integer userType = UserConstant.USER_TYPE_NORMAL;


    @Pattern(regexp = "^(active|banned)$")
    private String status = UserConstant.STATUS_ACTIVE;

    // === 等级经验相关 ===
    @Min(1)
    private Integer level = UserConstant.DEFAULT_LEVEL;

    @Min(0)
    private Long exp = UserConstant.DEFAULT_EXP;

    @Min(0)
    private Long nextLevelExp = UserConstant.getNextLevelRequiredExp(level);

    // === VIP相关 ===
    @Min(0)
    @Max(4)
    private Integer vipType = UserConstant.VIP_TYPE_NONE;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime vipExpireTime;

    // === 统计相关 ===
    @Min(0)
    private Integer coinBalance = 0;           // 积分/金币余额

    @Min(0)
    private Integer contribution = 0;          // 贡献值

    @Min(0)
    private Integer uploadCount = 0;           // 上传作品数量

    @Min(0)
    private Integer likedCount = 0;            // 获赞数量

    @Min(0)
    private Integer favoriteCount = 0;         // 收藏数量

    @Min(0)
    private Integer commentCount = 0;          // 评论数量

    @Min(0)
    private Integer viewCount = 0;             // 作品被浏览总数

    @Min(0)
    private Integer followCount = 0;           // 关注数

    @Min(0)
    private Integer followerCount = 0;         // 粉丝数

    // === 登录相关 ===
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    @Min(0)
    private Integer loginDays = 0;             // 连续登录天数

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastLoginDate;       // 最后登录日期（用于计算连续登录）

    @Min(0)
    private Integer totalLoginDays = 0;        // 累计登录天数

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActiveTime;      // 最后活跃时间

    // === 安全相关 ===
    private String registerIp;                 // 注册IP
    private String registerLocation;           // 注册地
    private String lastLoginIp;                // 最后登录IP
    private String lastLoginLocation;          // 最后登录地
    private String registerSource;             // 注册来源：web, app, wechat等
    private Long tokenVersion;                 // Token版本号（用于强制登出）
    private Integer loginFailCount;            // 登录失败次数
    private LocalDateTime lockedUntil;         // 锁定截止时间

    // === 时间戳 ===
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // === 工具方法 ===

    /**
     * 判断是否是管理员
     */
    @JsonIgnore
    public boolean isAdmin() {
        return this.userType != null &&
                UserConstant.USER_TYPE_ADMIN.equals(this.userType);
    }


    // === 上传相关工具方法 ===

    /**
     * 获取用户每日上传限制
     */
    @JsonIgnore
    public Integer getDailyUploadLimit() {
        return UserConstant.getDailyUploadLimit(this.vipType);
    }

    /**
     * 是否无限上传
     */
    @JsonIgnore
    public boolean isUnlimitedUpload() {
        return UserConstant.isUnlimitedUpload(this.vipType);
    }

    /**
     * 检查是否可以上传（基于今日已上传数量）
     * @param todayUploadCount 今日已上传数量
     * @return 是否可以上传
     */
    @JsonIgnore
    public boolean canUpload(int todayUploadCount) {
        if (!isActive()) {
            return false;
        }

        int limit = getDailyUploadLimit();
        if (limit == -1) {  // 无限
            return true;
        }

        return todayUploadCount < limit;
    }

    /**
     * 获取剩余可上传数量
     * @param todayUploadCount 今日已上传数量
     * @return 剩余可上传数量（-1表示无限）
     */
    @JsonIgnore
    public int getRemainingUploadCount(int todayUploadCount) {
        int limit = getDailyUploadLimit();
        if (limit == -1) {
            return -1;  // 无限
        }
        return Math.max(0, limit - todayUploadCount);
    }

    /**
     * 判断账号是否正常（未封禁）
     */
    @JsonIgnore
    public boolean isActive() {
        return UserConstant.STATUS_ACTIVE.equals(this.status);
    }

    /**
     * 判断是否是VIP（在有效期内）
     */
    @JsonIgnore
    public boolean isVip() {
        if (this.vipType == null ||
                UserConstant.VIP_TYPE_NONE.equals(this.vipType)) {
            return false;
        }
        // 检查VIP是否过期
        if (this.vipExpireTime != null) {
            return LocalDateTime.now().isBefore(this.vipExpireTime);
        }
        // 终身VIP
        return UserConstant.VIP_TYPE_LIFETIME.equals(this.vipType);
    }

    /**
     * 获取VIP剩余天数
     */
    @JsonIgnore
    public Long getVipRemainingDays() {
        if (!isVip() || this.vipExpireTime == null) {
            return 0L;
        }
        return Duration.between(LocalDateTime.now(), this.vipExpireTime).toDays();
    }
}