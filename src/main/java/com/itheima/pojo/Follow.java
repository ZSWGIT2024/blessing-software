package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Follow {
    private Long id;
    private Integer followerId;      // 关注者ID
    private Integer followingId;     // 被关注者ID
    private String relationType;     // 关注类型：normal-普通关注，special-特别关注
    private String remark;           // 备注
    private Boolean muteNotify;      // 是否静默通知（0-通知，1-不通知）
    private Boolean isHidden;        // 是否隐藏关注（0-公开，1-隐藏）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // 关联信息
    private User follower;           // 关注者信息
    private User following;          // 被关注者信息
}