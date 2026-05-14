package com.itheima.service;

import com.itheima.dto.SystemMessageDTO;
import com.itheima.pojo.Result;

import java.util.List;

/**
 * 系统消息服务接口
 * <p>
 * 提供系统消息的完整管理能力。系统消息用于向用户推送公告、通知等内容，
 * 支持按用户（user）、群组（group）或全站（all）投放目标类型，
 * 消息可设置过期时间和激活状态。管理员可进行增删改查操作。
 * </p>
 *
 * @author idealU
 * @since 1.0
 */
public interface SystemMessageService {

    /**
     * 创建系统消息
     * <p>
     * 标题和内容为必填项。自动生成唯一消息ID（格式：sys_{senderId}_{timestamp}）。
     * 可选设置消息类型（默认admin_broadcast）、目标类型（默认all）、目标ID和过期时间。
     * 创建后消息默认激活。
     * </p>
     *
     * @param senderId 发送者（管理员）用户ID
     * @param dto      系统消息DTO，包含标题、内容、类型、目标、过期时间等
     * @return 创建成功的系统消息（含生成的messageId和创建时间）
     */
    Result<SystemMessageDTO> createSystemMessage(Integer senderId, SystemMessageDTO dto);

    /**
     * 更新系统消息
     * <p>
     * 仅更新DTO中不为空的字段。消息不存在时返回错误。
     * </p>
     *
     * @param messageId 要更新的消息ID
     * @param dto       包含更新字段的DTO（null字段不更新）
     * @return 更新后的系统消息
     */
    Result<SystemMessageDTO> updateSystemMessage(String messageId, SystemMessageDTO dto);

    /**
     * 停用系统消息
     * <p>
     * 将消息的激活状态设为false。停用后该消息不再推送给用户，
     * 但数据保留在数据库中。
     * </p>
     *
     * @param messageId 消息ID
     * @return 操作成功返回空结果
     */
    Result<?> deactivateSystemMessage(String messageId);

    /**
     * 删除系统消息
     * <p>
     * 物理删除指定消息，数据从数据库中永久移除。
     * </p>
     *
     * @param messageId 消息ID
     * @return 操作成功返回空结果
     */
    Result<?> deleteSystemMessage(String messageId);

    /**
     * 获取用户当前的活跃系统消息
     * <p>
     * 返回满足以下条件之一的消息：
     * <ul>
     *   <li>目标类型为all（全站通知）</li>
     *   <li>目标类型为user且targetId匹配当前用户</li>
     *   <li>目标类型为group且targetId是用户所在的群</li>
     * </ul>
     * 同时要求消息激活状态为true且未过期（到期时间为null或大于当前时间）。
     * </p>
     *
     * @param userId 用户ID
     * @return 符合条件的活跃系统消息列表
     */
    Result<List<SystemMessageDTO>> getActiveMessages(Integer userId);

    /**
     * 获取全部系统消息
     * <p>
     * 查询所有系统消息（包括激活和停用的），按创建时间倒序排列。
     * 通常用于管理后台。
     * </p>
     *
     * @return 全部系统消息列表
     */
    Result<List<SystemMessageDTO>> getAllMessages();
}
