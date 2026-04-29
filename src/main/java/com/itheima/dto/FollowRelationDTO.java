package com.itheima.dto;

import com.itheima.pojo.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowRelationDTO {
    private Long id;
    private Integer followerId;
    private String followerUsername;
    private String followerNickname;
    private String followerAvatar;
    private String followerAvatarFrame;
    private Integer followingId;
    private String followingUsername;
    private String followingNickname;
    private String followingAvatar;
    private String followingAvatarFrame;
    private String remark;
    private String relationType;
    private LocalDateTime createTime;

    // 关联信息
    private Boolean isFriend;
    private Boolean isFollowing;
    private User follower;           // 关注者信息
    private User following;          // 被关注者信息
}