package com.itheima.vo;

import com.itheima.vo.FollowUserVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 关注关系VO
 */
@Data
public class FollowRelationVO {
    private Integer userId;          // 用户ID
    private Integer targetUserId;    // 目标用户ID
    private Boolean isFollowing;     // 是否已关注
    private Boolean isFollowed;      // 是否被关注
    private Boolean isMutual;        // 是否互相关注
    private Boolean isFriend;        // 是否是好友
    private String relationType;     // 关注类型
    private String remark;           // 备注
}

