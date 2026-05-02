-- =============================================
-- 安全优化相关数据库表
-- 创建日期：2026-04-30
-- 说明：登录记录表 + 操作日志表
-- =============================================

-- 1. 登录记录表
CREATE TABLE IF NOT EXISTS `login_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` INT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `login_type` VARCHAR(20) NOT NULL COMMENT '登录类型：password/code/token',
    `login_ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
    `login_location` VARCHAR(100) DEFAULT NULL COMMENT '登录地点',
    `device_info` VARCHAR(500) DEFAULT NULL COMMENT '设备信息(User-Agent)',
    `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器',
    `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
    `status` VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '登录状态：success/fail',
    `fail_reason` VARCHAR(200) DEFAULT NULL COMMENT '失败原因',
    `login_time` DATETIME NOT NULL COMMENT '登录时间',
    `logout_time` DATETIME DEFAULT NULL COMMENT '登出时间',
    `online_duration` BIGINT DEFAULT NULL COMMENT '在线时长（秒）',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_login_time` (`login_time`),
    INDEX `idx_status` (`status`),
    INDEX `idx_login_ip` (`login_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录记录表';

-- 2. 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `module` VARCHAR(50) DEFAULT NULL COMMENT '模块名称',
    `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
    `type` VARCHAR(20) DEFAULT NULL COMMENT '操作类型：LOGIN/LOGOUT/INSERT/UPDATE/DELETE等',
    `method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法：GET/POST/PUT/DELETE',
    `request_url` VARCHAR(200) DEFAULT NULL COMMENT '请求URL',
    `request_ip` VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
    `request_params` TEXT DEFAULT NULL COMMENT '请求参数（已脱敏JSON）',
    `response_result` TEXT DEFAULT NULL COMMENT '响应结果',
    `user_id` INT DEFAULT NULL COMMENT '操作用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    `duration` INT DEFAULT NULL COMMENT '执行时长（毫秒）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '操作状态：success/fail',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_module` (`module`),
    INDEX `idx_type` (`type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 3. User表扩展字段（如不存在则添加，MySQL 5.7+ 请逐条执行或使用存储过程）
-- 添加Token版本字段
ALTER TABLE `user` ADD COLUMN `token_version` BIGINT DEFAULT 0 COMMENT 'Token版本号（用于强制登出）';

-- 添加登录失败相关字段
ALTER TABLE `user` ADD COLUMN `login_fail_count` INT DEFAULT 0 COMMENT '登录失败次数';

ALTER TABLE `user` ADD COLUMN `locked_until` DATETIME DEFAULT NULL COMMENT '锁定截止时间';

-- 为User表新增字段添加索引
CREATE INDEX IF NOT EXISTS `idx_user_status` ON `user` (`status`);
CREATE INDEX IF NOT EXISTS `idx_user_phone` ON `user` (`phone`);
CREATE INDEX IF NOT EXISTS `idx_user_vip_type` ON `user` (`vipType`);

-- 4. 为登录记录表补充复合索引
CREATE INDEX IF NOT EXISTS `idx_login_user_time` ON `login_record` (`user_id`, `login_time`);
CREATE INDEX IF NOT EXISTS `idx_login_status_time` ON `login_record` (`status`, `login_time`);

-- 5. 为操作日志表补充复合索引
CREATE INDEX IF NOT EXISTS `idx_op_module_time` ON `operation_log` (`module`, `create_time`);
CREATE INDEX IF NOT EXISTS `idx_op_user_time` ON `operation_log` (`user_id`, `create_time`);
