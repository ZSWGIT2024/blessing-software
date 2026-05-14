package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊事件日志实体类
 * <p>
 * 记录群聊内发生的各类管理操作事件，如成员加入/退出、
 * 角色变更、群信息修改、禁言/解除禁言、消息撤回等。
 * 用于审计追溯和群聊管理日志展示。
 * </p>
 *
 * @author blessing-software
 */
@Data
public class ChatGroupEvent {

    /** 主键ID，自增 */
    private Long id;

    /** 所属群聊的业务唯一标识，关联 chat_group 表 */
    private String groupId;

    /**
     * 事件类型
     * <ul>
     *   <li>member_join - 成员加入</li>
     *   <li>member_leave - 成员退出</li>
     *   <li>member_kick - 成员被踢出</li>
     *   <li>role_change - 角色变更</li>
     *   <li>group_info_update - 群信息修改</li>
     *   <li>member_mute - 成员被禁言</li>
     *   <li>member_unmute - 成员解除禁言</li>
     *   <li>all_mute - 全员禁言开关</li>
     *   <li>message_withdraw - 消息撤回</li>
     *   <li>group_dissolve - 群解散</li>
     * </ul>
     */
    private String eventType;

    /** 操作人用户ID，即触发该事件的用户（群主/管理员/系统） */
    private Integer operatorId;

    /** 被操作的目标用户ID，当事件有明确目标用户时填写 */
    private Integer targetUserId;

    /** 变更前的旧值（如旧角色、旧群名等），用于审计对比 */
    private String oldValue;

    /** 变更后的新值（如新角色、新群名等），用于审计对比 */
    private String newValue;

    /** 事件发生时间 */
    private LocalDateTime createTime;
}
