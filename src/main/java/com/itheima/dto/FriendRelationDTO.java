package com.itheima.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRelationDTO {
    private Long id;
    private Integer userId;
    private Integer friendId;
    private String friendUsername;
    private String friendNickname;
    private String friendAvatar;
    private String friendAvatarFrame;
    private String groupName;
    private String remark;// 备注
    private String relationType;// 关系类型  friend-好友，stranger-陌生人
    private Boolean isStarred;// 是否置顶
    private Boolean isBlocked;// 是否被拉黑
    private Boolean isFollowing;// 是否已关注
    private String isOnline;// 是否在线
    private String lastMessage;// 最后一条消息
    private Integer unreadCount;// 未读消息数
    private LocalDateTime becomeFriendTime;
    private LocalDateTime lastInteractionTime;
}
