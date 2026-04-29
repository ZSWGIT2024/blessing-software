package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendRelation {
    private Long id;
    private Integer userId;          // 用户ID
    private Integer friendId;        // 好友ID
    private String groupName;        // 分组名称
    private String remark;           // 备注
    @Pattern(regexp = "^(friend|stranger)$")
    private String relationType = "stranger";// 关系类型  friend-好友，stranger-陌生人
    private Boolean isStarred;       // 是否星标
    private Boolean isBlocked;       // 是否屏蔽

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime becomeFriendTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastInteractionTime;

    private Integer interactionCount; // 互动次数

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}