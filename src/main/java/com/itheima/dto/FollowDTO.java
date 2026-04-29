package com.itheima.dto;

import lombok.Data;

/**
 * 关注相关DTO
 */
@Data
public class FollowDTO {
    private Integer followerId;      // 关注者ID
    private Integer followingId;     // 被关注者ID
    private String remark;           // 备注
    private String relationType;     // 关注类型：normal, special
    private Boolean muteNotify;      // 是否静默通知
    private Boolean isHidden;        // 是否隐藏
}