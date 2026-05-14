package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统消息实体类
 * <p>
 * 表示由系统或管理员发送的通知/公告类消息，可以面向全体用户、
 * 指定用户、指定群组等不同目标范围。支持消息类型分类、
 * 生效状态控制和过期时间设置，用于系统通知、运营公告、
 * 活动推送等场景。
 * </p>
 *
 * @author blessing-software
 */
@Data
public class SystemMessage {

    /** 主键ID，自增 */
    private Long id;

    /** 消息业务唯一标识（雪花ID或UUID），用于对外暴露和消息去重 */
    private String messageId;

    /** 系统消息标题，简要概括消息主题 */
    private String title;

    /** 系统消息正文内容，支持纯文本或简单的富文本格式 */
    private String content;

    /**
     * 系统消息类型
     * <ul>
     *   <li>system_notice - 系统通知（如维护公告、版本更新）</li>
     *   <li>activity - 活动推送（如节日活动、限时福利）</li>
     *   <li>admin_notice - 管理员公告（如违规警告、规则更新）</li>
     *   <li>vip_notice - VIP相关通知（如会员到期提醒、权益变更）</li>
     * </ul>
     */
    private String messageType;

    /**
     * 目标类型，定义消息发送的范围
     * <ul>
     *   <li>all - 全体用户</li>
     *   <li>user - 指定用户（配合 targetId 使用）</li>
     *   <li>group - 指定群组（配合 targetId 使用）</li>
     *   <li>vip - 仅VIP用户可见</li>
     *   <li>level - 指定等级以上用户可见</li>
     * </ul>
     */
    private String targetType;

    /**
     * 目标ID
     * <ul>
     *   <li>targetType=user 时存放用户ID</li>
     *   <li>targetType=group 时存放群组ID</li>
     *   <li>targetType=level 时存放等级数值</li>
     *   <li>targetType=all/vip 时为空</li>
     * </ul>
     */
    private String targetId;

    /** 发送者ID：系统消息时可以为系统用户ID（如0或-1），管理员发送时为管理员用户ID */
    private Integer senderId;

    /** 消息是否生效：true-生效中（前端展示），false-已停用（不展示） */
    private Boolean isActive;

    /** 消息过期时间，超过此时间消息不再展示，null表示永不过期 */
    private LocalDateTime expireTime;

    /** 消息创建时间 */
    private LocalDateTime createTime;
}
