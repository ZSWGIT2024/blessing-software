package com.itheima.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.dto.SystemMessageDTO;
import com.itheima.mapper.SystemMessageMapper;
import com.itheima.pojo.Result;
import com.itheima.pojo.SystemMessage;
import com.itheima.service.SystemMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统消息服务实现类
 * <p>
 * 负责系统消息的全生命周期管理。系统消息用于向用户推送
 * 公告、通知等内容，支持按用户(user)、群组(group)或全站(all)
 * 三种目标类型进行定向投放。消息可设置过期时间，到期后
 * 由定时任务自动清理。
 * </p>
 * <p>
 * 主要功能：
 * <ul>
 *   <li>创建/编辑/停用/删除系统消息</li>
 *   <li>按用户维度查询活跃消息（支持全站、用户、群组三种范围）</li>
 *   <li>管理后台查询全部消息</li>
 * </ul>
 * </p>
 *
 * @author idealU
 * @since 1.0
 * @see SystemMessageService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMessageServiceImpl implements SystemMessageService {

    private final SystemMessageMapper systemMessageMapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建系统消息
     * <p>
     * 标题和内容为必填项（已trim处理）。自动生成messageId
     * （格式：sys_{senderId}_{timestamp}）。默认值：
     * <ul>
     *   <li>messageType默认为"admin_broadcast"</li>
     *   <li>targetType默认为"all"</li>
     *   <li>isActive默认为true（创建即激活）</li>
     * </ul>
     * 创建后在DTO中填充messageId和createTime返回。
     * </p>
     *
     * @param senderId 发送者（管理员）用户ID
     * @param dto      系统消息DTO（标题和内容必填）
     * @return 创建后的系统消息DTO（含生成的messageId）
     */
    @Override
    @Transactional
    public Result<SystemMessageDTO> createSystemMessage(Integer senderId, SystemMessageDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return Result.error("标题不能为空");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return Result.error("内容不能为空");
        }

        String messageId = "sys_" + senderId + "_" + System.currentTimeMillis();

        SystemMessage message = new SystemMessage();
        message.setMessageId(messageId);
        message.setTitle(dto.getTitle().trim());
        message.setContent(dto.getContent().trim());
        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "admin_broadcast");
        message.setTargetType(dto.getTargetType() != null ? dto.getTargetType() : "all");
        message.setTargetId(dto.getTargetId());
        message.setSenderId(senderId);
        message.setIsActive(true);
        message.setExpireTime(dto.getExpireTime());

        systemMessageMapper.insert(message);

        dto.setMessageId(messageId);
        dto.setCreateTime(LocalDateTime.now());
        return Result.success(dto);
    }

    /**
     * 更新系统消息
     * <p>
     * 仅更新DTO中不为空且不为空字符串的字段（增量更新）。
     * 先查询现有消息，存在时才执行更新，消息不存在时返回错误。
     * 不能更新messageType和isActive（这些有专门的方法）。
     * </p>
     *
     * @param messageId 消息ID
     * @param dto       包含要更新字段的DTO
     * @return 更新后的系统消息DTO
     */
    @Override
    @Transactional
    public Result<SystemMessageDTO> updateSystemMessage(String messageId, SystemMessageDTO dto) {
        SystemMessage existing = systemMessageMapper.selectByMessageId(messageId);
        if (existing == null) {
            return Result.error("系统消息不存在");
        }

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            existing.setTitle(dto.getTitle().trim());
        }
        if (dto.getContent() != null && !dto.getContent().trim().isEmpty()) {
            existing.setContent(dto.getContent().trim());
        }
        if (dto.getTargetType() != null) {
            existing.setTargetType(dto.getTargetType());
        }
        if (dto.getTargetId() != null) {
            existing.setTargetId(dto.getTargetId());
        }
        if (dto.getExpireTime() != null) {
            existing.setExpireTime(dto.getExpireTime());
        }

        systemMessageMapper.update(existing);
        dto.setMessageId(messageId);
        return Result.success(dto);
    }

    /**
     * 停用系统消息
     * <p>
     * 将消息的is_active字段设为false。停用后该消息不再
     * 对用户可见（通过getActiveMessages查询不到），
     * 但数据保留在数据库中。
     * </p>
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @Override
    @Transactional
    public Result<?> deactivateSystemMessage(String messageId) {
        systemMessageMapper.updateActive(messageId, false);
        return Result.success();
    }

    /**
     * 删除系统消息（物理删除）
     * <p>
     * 从system_message表中永久删除该记录。注意这是物理删除，
     * 不可恢复。建议先停用再删除。
     * </p>
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @Override
    @Transactional
    public Result<?> deleteSystemMessage(String messageId) {
        systemMessageMapper.delete(messageId);
        return Result.success();
    }

    /**
     * 获取用户当前的活跃系统消息
     * <p>
     * 返回激活且未过期的消息，目标范围包含：
     * <ul>
     *   <li>targetType为"all"的全站通知</li>
     *   <li>targetType为"user"且targetId等于当前用户ID</li>
     *   <li>targetType为"group"且targetId是用户所在的群（通过子查询）</li>
     * </ul>
     * 结果按创建时间倒序排列。通过Stream API转换为DTO返回。
     * </p>
     *
     * @param userId 用户ID
     * @return 符合条件的活跃系统消息DTO列表
     */
    @Override
    public Result<List<SystemMessageDTO>> getActiveMessages(Integer userId) {
        List<SystemMessage> messages = systemMessageMapper.selectActiveForUser(String.valueOf(userId));
        List<SystemMessageDTO> dtos = messages.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    /**
     * 获取全部系统消息（管理后台用）
     * <p>
     * 查询所有系统消息（包括激活和停用的），按创建时间倒序排列。
     * 用于管理后台展示完整消息列表。
     * </p>
     *
     * @return 全部系统消息DTO列表
     */
    @Override
    public Result<List<SystemMessageDTO>> getAllMessages() {
        List<SystemMessage> messages = systemMessageMapper.selectAll();
        List<SystemMessageDTO> dtos = messages.stream().map(this::toDTO).collect(Collectors.toList());
        return Result.success(dtos);
    }

    /**
     * 将SystemMessage实体转换为SystemMessageDTO
     * <p>
     * 字段映射：messageId、title、content、messageType、targetType、
     * targetId、senderId、isActive、expireTime、createTime。
     * 这是一个纯数据映射方法，不涉及业务逻辑。
     * </p>
     *
     * @param msg 系统消息实体对象
     * @return 系统消息DTO对象
     */
    private SystemMessageDTO toDTO(SystemMessage msg) {
        SystemMessageDTO dto = new SystemMessageDTO();
        dto.setMessageId(msg.getMessageId());
        dto.setTitle(msg.getTitle());
        dto.setContent(msg.getContent());
        dto.setMessageType(msg.getMessageType());
        dto.setTargetType(msg.getTargetType());
        dto.setTargetId(msg.getTargetId());
        dto.setSenderId(msg.getSenderId());
        dto.setIsActive(msg.getIsActive());
        dto.setExpireTime(msg.getExpireTime());
        dto.setCreateTime(msg.getCreateTime());
        return dto;
    }
}
