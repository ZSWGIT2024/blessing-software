package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊消息已读状态实体类
 * <p>
 * 记录群聊中每条消息被每个群成员的阅读状态。
 * 用于实现消息已读/未读功能，支持已读回执的展示
 * （如"n人已读"）、未读计数等群聊读状态管理。
 * 一条消息对应多条已读记录（每个群成员一条）。
 * </p>
 *
 * @author blessing-software
 */
@Data
public class ChatGroupMessageRead {

    /** 主键ID，自增 */
    private Long id;

    /** 消息业务唯一标识，关联 chat_group_message 表 */
    private String messageId;

    /** 所属群聊的业务唯一标识，关联 chat_group 表（冗余字段，方便按群查询） */
    private String groupId;

    /** 用户ID，表示该已读记录对应哪个群成员 */
    private Integer userId;

    /** 该用户是否已读此消息：true-已读，false-未读（默认） */
    private Boolean isRead;

    /** 用户阅读此消息的具体时间，未读时为null */
    private LocalDateTime readTime;
}
