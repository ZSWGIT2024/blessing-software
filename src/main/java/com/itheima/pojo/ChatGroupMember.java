package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊成员实体类
 * <p>
 * 表示用户与群聊之间的成员关系，记录用户在群内的角色、
 * 禁言状态、昵称、未读消息数、最后阅读时间等信息。
 * 一个用户可以在多个群中，每个群内该用户有一条成员记录。
 * </p>
 *
 * @author blessing-software
 */
@Data
public class ChatGroupMember {

    /** 主键ID，自增 */
    private Long id;

    /** 所属群聊的业务唯一标识，关联 chat_group 表 */
    private String groupId;

    /** 用户ID，关联用户表 */
    private Integer userId;

    /**
     * 用户在群内的角色
     * <ul>
     *   <li>owner - 群主，拥有最高权限</li>
     *   <li>admin - 管理员，由群主任命</li>
     *   <li>member - 普通成员</li>
     * </ul>
     */
    private String role;

    /** 用户在群内自定义的昵称，为空则显示用户原始昵称 */
    private String nicknameInGroup;

    /** 该成员是否被单独禁言：true-禁言中，false-正常状态 */
    private Boolean isMuted;

    /** 禁言截止时间，为null或过去时间表示未被禁言或禁言已过期 */
    private LocalDateTime mutedUntil;

    /** 该用户在此群中的未读消息数量，用于红点/角标展示 */
    private Integer unreadCount;

    /** 该用户在此群中最后一次阅读消息的时间 */
    private LocalDateTime lastReadTime;

    /** 该用户在此群中最后一条已读消息的业务ID，用于精确定位已读位置 */
    private String lastReadMessageId;

    /** 用户加入群聊的时间 */
    private LocalDateTime joinTime;

    /** 用户离开群聊的时间，为null表示当前仍在群内 */
    private LocalDateTime leaveTime;

    /** 邀请人用户ID，记录是谁邀请该用户入群的，为null表示自主加入 */
    private Integer inviterId;
}
