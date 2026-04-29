package com.itheima.dto;

import lombok.Data;

/**
 * 关注状态DTO
 */
@Data
public class FollowStatusDTO {
    private Integer targetUserId;    // 目标用户ID
    private Boolean isFollowing;     // 是否已关注
    private Boolean isFollowed;      // 是否被关注
}
