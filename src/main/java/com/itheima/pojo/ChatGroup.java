package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊实体类
 * <p>
 * 表示一个群聊的基本信息，包括群名称、头像、描述、群主、
 * 成员数量、加入权限、邀请码、禁言状态、解散状态等。
 * 每个群聊有唯一的 groupId 作为业务标识。
 * </p>
 *
 * @author blessing-software
 */
@Data
public class ChatGroup {

    /** 主键ID，自增 */
    private Long id;

    /** 群聊业务唯一标识（UUID或自定义生成），用于对外暴露，避免暴露自增ID */
    private String groupId;

    /** 群聊名称，由群主或管理员设定 */
    private String name;

    /** 群聊头像，存储对象存储（OSS） */
    private String avatar;

    /** 群聊简介/描述信息，用于展示群聊的基本介绍 */
    private String description;

    /** 群主用户ID，关联用户表，拥有群聊最高管理权限 */
    private Integer ownerId;

    /** 群聊最大成员数上限，受VIP等级或系统配置限制 */
    private Integer maxMembers;

    /** 当前群聊实际成员数量，用于快速展示，避免每次关联查询 */
    private Integer currentMembers;

    /**
     * 加入群聊的权限控制方式
     * <ul>
     *   <li>public - 任何人可自由加入</li>
     *   <li>approval - 需要群主/管理员审批</li>
     *   <li>invite_only - 仅可通过邀请码加入</li>
     * </ul>
     */
    private String joinPermission;

    /** 是否全员禁言：true-全员禁言（仅管理员可发言），false-正常发言模式 */
    private Boolean isMutedAll;

    /** 邀请码，用于 invite_only 模式下邀请新成员加入 */
    private String inviteCode;

    /** 邀请码过期时间，过期后邀请码自动失效 */
    private LocalDateTime inviteExpireTime;

    /** 群聊是否已解散：true-已解散，false-正常运营中 */
    private Boolean isDissolved;

    /** 群聊创建时间 */
    private LocalDateTime createTime;

    /** 群聊信息最后更新时间 */
    private LocalDateTime updateTime;
}
