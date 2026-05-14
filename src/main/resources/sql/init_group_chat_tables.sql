-- =============================================================
-- 群聊功能模块 - 数据库初始化脚本
-- 包含：群聊、群成员、群消息、消息已读、群事件、世界消息、
--        系统消息、消息状态历史 共8张表
-- 适用数据库：MySQL 5.7+ / 8.0+
-- =============================================================

-- =============================================================
-- 1. 群聊主表：存储群聊的基本信息
-- =============================================================
CREATE TABLE IF NOT EXISTS `chat_group` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT    COMMENT '主键ID，自增',
    `group_id`        VARCHAR(64)   NOT NULL                   COMMENT '群聊业务唯一标识（UUID）',
    `name`            VARCHAR(100)  NOT NULL                   COMMENT '群聊名称',
    `avatar`          VARCHAR(500)  DEFAULT NULL               COMMENT '群聊头像URL',
    `description`     VARCHAR(500)  DEFAULT ''                 COMMENT '群聊简介/描述',
    `owner_id`        INT           NOT NULL                   COMMENT '群主用户ID',
    `max_members`     INT           NOT NULL DEFAULT 200       COMMENT '群聊最大成员数上限',
    `current_members` INT           NOT NULL DEFAULT 0         COMMENT '当前成员数',
    `join_permission` VARCHAR(32)   NOT NULL DEFAULT 'approval' COMMENT '加入权限：public-自由加入, approval-需审批, invite_only-仅邀请',
    `is_muted_all`    TINYINT(1)    NOT NULL DEFAULT 0        COMMENT '是否全员禁言：0-否, 1-是',
    `invite_code`     VARCHAR(32)   DEFAULT NULL               COMMENT '邀请码',
    `invite_expire_time` DATETIME   DEFAULT NULL               COMMENT '邀请码过期时间',
    `is_dissolved`    TINYINT(1)    NOT NULL DEFAULT 0        COMMENT '是否已解散：0-否, 1-是',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_id` (`group_id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_is_dissolved` (`is_dissolved`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊主表';

-- =============================================================
-- 2. 群聊成员表：存储用户与群聊的成员关系
-- =============================================================
CREATE TABLE IF NOT EXISTS `chat_group_member` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '主键ID，自增',
    `group_id`            VARCHAR(64)  NOT NULL                   COMMENT '群聊业务唯一标识',
    `user_id`             INT          NOT NULL                   COMMENT '用户ID',
    `role`                VARCHAR(16)  NOT NULL DEFAULT 'member'  COMMENT '角色：owner-群主, admin-管理员, member-普通成员',
    `nickname_in_group`   VARCHAR(50)  DEFAULT NULL               COMMENT '用户在群内的自定义昵称',
    `is_muted`            TINYINT(1)   NOT NULL DEFAULT 0        COMMENT '是否被禁言：0-否, 1-是',
    `muted_until`         DATETIME     DEFAULT NULL               COMMENT '禁言截止时间，NULL表示未禁言',
    `unread_count`        INT          NOT NULL DEFAULT 0         COMMENT '未读消息数量',
    `last_read_time`      DATETIME     DEFAULT NULL               COMMENT '最后阅读时间',
    `last_read_message_id` VARCHAR(64) DEFAULT NULL               COMMENT '最后已读消息的业务ID',
    `join_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `leave_time`          DATETIME     DEFAULT NULL               COMMENT '离开时间，NULL表示仍在群内',
    `inviter_id`          INT          DEFAULT NULL               COMMENT '邀请人用户ID，NULL表示自主加入',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_role` (`role`),
    KEY `idx_join_time` (`join_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊成员表';

-- =============================================================
-- 3. 群聊消息表：存储群聊中的每条消息
-- =============================================================
CREATE TABLE IF NOT EXISTS `chat_group_message` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT    COMMENT '主键ID，自增',
    `message_id`       VARCHAR(64)   NOT NULL                   COMMENT '消息业务唯一标识（雪花ID/UUID）',
    `group_id`         VARCHAR(64)   NOT NULL                   COMMENT '所属群聊业务唯一标识',
    `sender_id`        INT           NOT NULL                   COMMENT '发送者用户ID',
    `content_type`     VARCHAR(16)   NOT NULL DEFAULT 'text'    COMMENT '消息类型：text-文本, image-图片, file-文件, video-视频, audio-语音, system-系统通知',
    `content`          TEXT          DEFAULT NULL               COMMENT '消息正文内容（过滤后的安全内容）',
    `original_content` TEXT          DEFAULT NULL               COMMENT '消息原始内容（未经敏感词过滤）',
    `file_id`          BIGINT        DEFAULT NULL               COMMENT '关联的文件ID，多媒体消息时关联文件表',
    `extra_data`       JSON          DEFAULT NULL               COMMENT '扩展数据（JSON格式），存储图片尺寸/文件名/系统事件等',
    `status`           VARCHAR(16)   NOT NULL DEFAULT 'sent'    COMMENT '消息状态：sent-已发送, delivered-已投递, withdrawn-已撤回, deleted-已删除',
    `is_deleted`       TINYINT(1)    NOT NULL DEFAULT 0        COMMENT '是否已软删除：0-否, 1-是',
    `is_filtered`      TINYINT(1)    NOT NULL DEFAULT 0        COMMENT '是否触发了敏感词过滤：0-否, 1-是',
    `filtered_words`   VARCHAR(500)  DEFAULT NULL               COMMENT '命中的敏感词列表（逗号分隔）',
    `mention_all`      TINYINT(1)    NOT NULL DEFAULT 0        COMMENT '是否@全体成员：0-否, 1-是',
    `withdraw_time`    DATETIME      DEFAULT NULL               COMMENT '撤回时间，NULL表示未撤回',
    `withdraw_reason`  VARCHAR(200)  DEFAULT NULL               COMMENT '撤回原因',
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_group_create_time` (`group_id`, `create_time`),
    KEY `idx_content_type` (`content_type`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊消息表';

-- =============================================================
-- 4. 群聊消息已读状态表：记录每条消息的已读情况
-- =============================================================
CREATE TABLE IF NOT EXISTS `chat_group_message_read` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '主键ID，自增',
    `message_id`  VARCHAR(64)  NOT NULL                   COMMENT '消息业务唯一标识',
    `group_id`    VARCHAR(64)  NOT NULL                   COMMENT '所属群聊业务唯一标识（冗余，方便按群统计）',
    `user_id`     INT          NOT NULL                   COMMENT '用户ID',
    `is_read`     TINYINT(1)   NOT NULL DEFAULT 0        COMMENT '是否已读：0-未读, 1-已读',
    `read_time`   DATETIME     DEFAULT NULL               COMMENT '阅读时间，未读时为NULL',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_user` (`message_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_group_id_read` (`group_id`, `is_read`),
    KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊消息已读状态表';

-- =============================================================
-- 5. 群聊事件日志表：记录群内的管理操作事件
-- =============================================================
CREATE TABLE IF NOT EXISTS `chat_group_event` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '主键ID，自增',
    `group_id`       VARCHAR(64)  NOT NULL                   COMMENT '所属群聊业务唯一标识',
    `event_type`     VARCHAR(32)  NOT NULL                   COMMENT '事件类型：member_join, member_leave, member_kick, role_change, group_info_update, member_mute, member_unmute, all_mute, message_withdraw, group_dissolve',
    `operator_id`    INT          NOT NULL                   COMMENT '操作人用户ID',
    `target_user_id` INT          DEFAULT NULL               COMMENT '目标用户ID',
    `old_value`      VARCHAR(500) DEFAULT NULL               COMMENT '变更前的旧值',
    `new_value`      VARCHAR(500) DEFAULT NULL               COMMENT '变更后的新值',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件发生时间',
    PRIMARY KEY (`id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_target_user_id` (`target_user_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_group_event_time` (`group_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊事件日志表';

-- =============================================================
-- 6. 群聊消息状态历史表：记录消息状态变更的完整链路
-- =============================================================
CREATE TABLE IF NOT EXISTS `chat_group_message_status_history` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '主键ID，自增',
    `message_id`   VARCHAR(64)  NOT NULL                   COMMENT '消息业务唯一标识',
    `group_id`     VARCHAR(64)  NOT NULL                   COMMENT '所属群聊业务唯一标识',
    `from_status`  VARCHAR(16)  DEFAULT NULL               COMMENT '变更前的状态',
    `to_status`    VARCHAR(16)  NOT NULL                   COMMENT '变更后的状态',
    `operator_id`  INT          NOT NULL                   COMMENT '操作人用户ID',
    `reason`       VARCHAR(200) DEFAULT NULL               COMMENT '变更原因/备注',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '状态变更时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_id` (`message_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群聊消息状态变更历史表';

-- =============================================================
-- 7. 世界聊天消息表：存储世界频道的公共消息
-- =============================================================
CREATE TABLE IF NOT EXISTS `chat_world_message` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '主键ID，自增',
    `message_id`     VARCHAR(64)  NOT NULL                   COMMENT '消息业务唯一标识（雪花ID/UUID）',
    `sender_id`      INT          NOT NULL                   COMMENT '发送者用户ID',
    `content_type`   VARCHAR(16)  NOT NULL DEFAULT 'text'    COMMENT '消息类型：text-文本, image-图片, system-系统公告',
    `content`        TEXT         DEFAULT NULL               COMMENT '消息正文内容',
    `is_filtered`    TINYINT(1)   NOT NULL DEFAULT 0        COMMENT '是否触发了敏感词过滤：0-否, 1-是',
    `filtered_words` VARCHAR(500) DEFAULT NULL               COMMENT '命中的敏感词列表（逗号分隔）',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_sender_create` (`sender_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界聊天消息表';

-- =============================================================
-- 8. 系统消息表：存储系统/管理员发送的通知公告
-- =============================================================
CREATE TABLE IF NOT EXISTS `system_message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '主键ID，自增',
    `message_id`   VARCHAR(64)  NOT NULL                   COMMENT '消息业务唯一标识（雪花ID/UUID）',
    `title`        VARCHAR(200) NOT NULL                   COMMENT '系统消息标题',
    `content`      TEXT         DEFAULT NULL               COMMENT '系统消息正文内容',
    `message_type` VARCHAR(32)  NOT NULL DEFAULT 'system_notice' COMMENT '消息类型：system_notice-系统通知, activity-活动推送, admin_notice-管理员公告, vip_notice-VIP通知',
    `target_type`  VARCHAR(16)  NOT NULL DEFAULT 'all'     COMMENT '目标类型：all-全体, user-指定用户, group-指定群组, vip-仅VIP, level-指定等级以上',
    `target_id`    VARCHAR(64)  DEFAULT NULL               COMMENT '目标ID：user时为用户ID, group时为群组ID, level时为等级数值',
    `sender_id`    INT          DEFAULT NULL               COMMENT '发送者ID：系统为0/-1，管理员为管理员用户ID',
    `is_active`    TINYINT(1)   NOT NULL DEFAULT 1        COMMENT '是否生效：0-已停用, 1-生效中',
    `expire_time`  DATETIME     DEFAULT NULL               COMMENT '过期时间，NULL表示永不过期',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_id` (`message_id`),
    KEY `idx_message_type` (`message_type`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_active_expire` (`is_active`, `expire_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息表';
