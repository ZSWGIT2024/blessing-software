package com.itheima.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.common.UserConstant;
import com.itheima.dto.*;
import com.itheima.mapper.*;
import com.itheima.pojo.*;
import com.itheima.pojo.User;
import com.itheima.service.GroupChatService;
import com.itheima.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 群聊服务实现类
 * <p>
 * 群聊服务的核心实现，依赖以下组件完成群聊的完整生命周期管理：
 * <ul>
 *   <li><b>ChatGroupMapper</b>：群基本信息持久化</li>
 *   <li><b>ChatGroupMemberMapper</b>：群成员信息持久化</li>
 *   <li><b>ChatGroupMessageMapper</b>：群消息持久化</li>
 *   <li><b>ChatGroupMessageReadMapper</b>：消息已读状态持久化</li>
 *   <li><b>ChatGroupEventMapper</b>：群事件日志持久化</li>
 *   <li><b>ChatFileMapper</b>：聊天文件信息查询</li>
 *   <li><b>UserMapper</b>：用户信息查询（用于判断VIP等级）</li>
 *   <li><b>RateLimitService</b>：频率限制（防止刷屏）</li>
 *   <li><b>SensitiveFilterService</b>：敏感词过滤</li>
 * </ul>
 * </p>
 * <p>
 * 所有写操作均使用 {@link Transactional @Transactional} 注解确保数据一致性。
 * 关键业务操作通过 {@link #logEvent} 方法记录审计日志。
 * </p>
 *
 * @author idealU
 * @since 1.0
 * @see GroupChatService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupChatServiceImpl implements GroupChatService {

    private final ChatGroupMapper groupMapper;
    private final ChatGroupMemberMapper memberMapper;
    private final ChatGroupMessageMapper messageMapper;
    private final ChatGroupMessageReadMapper readMapper;
    private final ChatGroupEventMapper eventMapper;
    private final ChatFileMapper chatFileMapper;
    private final UserMapper userMapper;
    private final RateLimitService rateLimitService;
    private final SensitiveFilterService sensitiveFilterService;
    private final GroupCacheService groupCacheService;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    // ==================== Group CRUD ====================

    /**
     * 创建群聊
     * <p>
     * 执行流程：
     * <ol>
     *   <li>校验群名称（非空且不超过50字符）</li>
     *   <li>根据用户VIP状态检查创建上限</li>
     *   <li>生成16位唯一群ID</li>
     *   <li>插入群记录，创建者设为群主并自动成为群成员</li>
     *   <li>记录group_created事件</li>
     * </ol>
     * </p>
     * <p>
     * 默认值：最大成员200，加入权限为任何人可加入，全员禁言关闭。
     * </p>
     *
     * @param userId 创建者用户ID
     * @param dto    创建群请求参数
     * @return 创建成功返回群详情；名称无效/超过上限时返回错误
     */
    @Override
    @Transactional
    public Result<GroupInfoDTO> createGroup(Integer userId, CreateGroupDTO dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return Result.error("群名称不能为空");
        }
        dto.setName(dto.getName().trim());
        if (dto.getName().length() > 50) {
            return Result.error("群名称最多50个字符");
        }

        User user = userMapper.findUserById(userId);
        boolean isVip = user != null && user.getVipType() != null && user.getVipType() != UserConstant.VIP_TYPE_NONE;
        int maxGroups = isVip ? UserConstant.MAX_GROUPS_VIP : UserConstant.MAX_GROUPS_NORMAL;
        if (groupMapper.countOwnedGroups(userId) >= maxGroups) {
            return Result.error("创建的群聊数量已达上限");
        }

        String groupId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        ChatGroup group = new ChatGroup();
        group.setGroupId(groupId);
        group.setName(dto.getName());
        group.setAvatar(dto.getAvatar());
        group.setDescription(dto.getDescription());
        group.setOwnerId(userId);
        group.setMaxMembers(dto.getMaxMembers() != null ? Math.min(dto.getMaxMembers(), UserConstant.GROUP_MAX_MEMBERS) : 200);
        group.setCurrentMembers(1);
        group.setJoinPermission(dto.getJoinPermission() != null ? dto.getJoinPermission() : UserConstant.GROUP_JOIN_ANYONE);
        group.setIsMutedAll(false);
        group.setIsDissolved(false);

        groupMapper.insertGroup(group);

        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(UserConstant.GROUP_ROLE_OWNER);
        member.setIsMuted(false);
        memberMapper.insert(member);

        logEvent(groupId, "group_created", userId, null, null, null);
        groupCacheService.evictMyGroupsCache(userId);

        return Result.success(buildGroupInfo(userId, group, "owner"));
    }

    /**
     * 更新群信息
     * <p>
     * 仅群主或管理员可编辑。支持更新群名称、头像、描述、最大成员数、加入权限。
     * 不传的字段保持原值不变。群名称会trim处理。
     * </p>
     *
     * @param userId 操作者用户ID
     * @param dto    更新参数（groupId必传）
     * @return 更新后的群详情；无权限或群不存在时返回错误
     */
    @Override
    @Transactional
    public Result<GroupInfoDTO> updateGroup(Integer userId, UpdateGroupDTO dto) {
        ChatGroup group = groupMapper.selectByGroupId(dto.getGroupId());
        if (group == null) {
            return Result.error("群聊不存在");
        }
        if (!isOwnerOrAdmin(userId, dto.getGroupId())) {
            return Result.error("只有群主或管理员才能编辑群信息");
        }

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            group.setName(dto.getName().trim());
        }
        if (dto.getAvatar() != null) {
            group.setAvatar(dto.getAvatar());
        }
        if (dto.getDescription() != null) {
            group.setDescription(dto.getDescription());
        }
        if (dto.getMaxMembers() != null) {
            group.setMaxMembers(Math.min(dto.getMaxMembers(), UserConstant.GROUP_MAX_MEMBERS));
        }
        if (dto.getJoinPermission() != null) {
            group.setJoinPermission(dto.getJoinPermission());
        }
        if (dto.getIsMutedAll() != null) {
            group.setIsMutedAll(dto.getIsMutedAll());
        }

        groupMapper.updateGroup(group);
        logEvent(dto.getGroupId(), "group_edited", userId, null, null, null);
        groupCacheService.evictGroupInfoCache(dto.getGroupId());
        groupCacheService.evictMyGroupsCache(userId);

        ChatGroupMember member = memberMapper.selectByGroupAndUser(dto.getGroupId(), userId);
        return Result.success(buildGroupInfo(userId, group, member != null ? member.getRole() : "member"));
    }

    /**
     * 解散群聊
     * <p>
     * 仅群主可解散。执行后将群的is_dissolved标记置为1，
     * 该群不再接收新消息，成员也无法发送消息。操作记录日志。
     * </p>
     *
     * @param userId  群主用户ID
     * @param groupId 群ID
     * @return 成功返回空结果；非群主或群不存在时返回错误
     */
    @Override
    @Transactional
    public Result<?> dissolveGroup(Integer userId, String groupId) {
        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null) {
            return Result.error("群聊不存在");
        }
        if (!group.getOwnerId().equals(userId)) {
            return Result.error("只有群主才能解散群聊");
        }

        groupMapper.dissolveGroup(groupId);
        logEvent(groupId, "group_dissolved", userId, null, null, null);
        groupCacheService.evictAllGroupCache(groupId);
        groupCacheService.evictMyGroupsCache(userId);
        return Result.success();
    }

    // ==================== Membership ====================

    /**
     * 加入群聊
     * <p>
     * 支持通过群ID或邀请码加入。加入前校验：
     * <ul>
     *   <li>群是否存在且未解散</li>
     *   <li>用户是否已在群中（防止重复加入）</li>
     *   <li>群成员是否达到上限</li>
     * </ul>
     * 加入成功后成员计数+1，记录member_joined事件。
     * </p>
     *
     * @param userId 用户ID
     * @param dto    包含groupId或inviteCode的加入参数
     * @return 加入后的群详情；失败时返回具体错误原因
     */
    @Override
    @Transactional
    public Result<GroupInfoDTO> joinGroup(Integer userId, JoinGroupDTO dto) {
        ChatGroup group = null;
        if (dto.getInviteCode() != null && !dto.getInviteCode().isEmpty()) {
            group = groupMapper.selectByInviteCode(dto.getInviteCode());
        } else if (dto.getGroupId() != null && !dto.getGroupId().isEmpty()) {
            group = groupMapper.selectByGroupId(dto.getGroupId());
        }
        if (group == null) {
            return Result.error("群聊不存在或邀请码已失效");
        }
        String groupId = group.getGroupId();

        if (group.getIsDissolved()) {
            return Result.error("该群聊已解散");
        }

        ChatGroupMember existing = memberMapper.selectByGroupAndUser(groupId, userId);
        if (existing != null) {
            return Result.error("你已经是该群成员");
        }

        // For approval-based groups, submit a join request instead of directly joining
        if (UserConstant.GROUP_JOIN_APPROVAL.equals(group.getJoinPermission())) {
            String redisKey = "group:join_requests:" + groupId;
            redisUtil.sAdd(redisKey, userId);
            return Result.error("申请已提交，请等待群管理员审核");
        }

        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            return Result.error("群成员已满");
        }

        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(UserConstant.GROUP_ROLE_MEMBER);
        member.setIsMuted(false);
        memberMapper.insert(member);

        groupMapper.updateMemberCount(groupId, 1);
        logEvent(groupId, "member_joined", userId, null, null, null);
        groupCacheService.evictGroupOnMemberChange(groupId, userId);

        ChatGroupMember self = memberMapper.selectByGroupAndUser(groupId, userId);
        return Result.success(buildGroupInfo(userId, group, self.getRole()));
    }

    /**
     * 退出群聊
     * <p>
     * 群主不能直接退出（需先转让）。退出后标记leave_time，
     * 群成员计数-1。记录member_left事件。
     * </p>
     *
     * @param userId  用户ID
     * @param groupId 群ID
     * @return 成功返回空结果；群主尝试退出或非成员时返回错误
     */
    @Override
    @Transactional
    public Result<?> leaveGroup(Integer userId, String groupId) {
        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null) {
            return Result.error("群聊不存在");
        }

        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        if (member == null) {
            return Result.error("你不是该群成员");
        }
        if (UserConstant.GROUP_ROLE_OWNER.equals(member.getRole())) {
            return Result.error("群主不能直接退出，请先转让群主");
        }

        memberMapper.markAsLeft(groupId, userId);
        groupMapper.updateMemberCount(groupId, -1);
        logEvent(groupId, "member_left", userId, null, null, null);
        groupCacheService.evictGroupOnMemberChange(groupId, userId);
        return Result.success();
    }

    /**
     * 踢出群成员
     * <p>
     * 权限校验规则：
     * <ul>
     *   <li>操作者必须是群主或管理员</li>
     *   <li>不能踢出群主</li>
     *   <li>管理员不能踢出其他管理员（防止同级互踢）</li>
     * </ul>
     * 踢出后标记leave_time，成员计数-1，记录member_kicked事件。
     * </p>
     *
     * @param operatorId   操作者用户ID
     * @param groupId      群ID
     * @param targetUserId 被踢出的用户ID
     * @return 成功返回空结果；权限不足或目标不可踢时返回错误
     */
    @Override
    @Transactional
    public Result<?> kickMember(Integer operatorId, String groupId, Integer targetUserId) {
        if (!isOwnerOrAdmin(operatorId, groupId)) {
            return Result.error("只有群主或管理员才能踢人");
        }

        ChatGroupMember target = memberMapper.selectByGroupAndUser(groupId, targetUserId);
        if (target == null) {
            return Result.error("该用户不是群成员");
        }
        if (UserConstant.GROUP_ROLE_OWNER.equals(target.getRole())) {
            return Result.error("不能踢出群主");
        }
        if (isAdmin(operatorId, groupId) && isAdmin(targetUserId, groupId)) {
            return Result.error("管理员不能踢出其他管理员");
        }

        memberMapper.markAsLeft(groupId, targetUserId);
        groupMapper.updateMemberCount(groupId, -1);
        logEvent(groupId, "member_kicked", operatorId, targetUserId, null, null);
        return Result.success();
    }

    // ==================== Roles ====================

    /**
     * 修改成员角色
     * <p>
     * 仅群主可将成员设为管理员(admin)或降为普通成员(member)。
     * 不允许设置为owner角色（需使用转让群主功能）。
     * </p>
     *
     * @param operatorId   群主用户ID
     * @param groupId      群ID
     * @param targetUserId 目标用户ID
     * @param role         新角色（admin或member）
     * @return 成功返回空结果；非群主或无效角色时返回错误
     */
    @Override
    @Transactional
    public Result<?> changeMemberRole(Integer operatorId, String groupId, Integer targetUserId, String role) {
        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null || !group.getOwnerId().equals(operatorId)) {
            return Result.error("只有群主才能修改成员角色");
        }
        if (!UserConstant.GROUP_ROLE_ADMIN.equals(role) && !UserConstant.GROUP_ROLE_MEMBER.equals(role)) {
            return Result.error("无效的角色");
        }

        // When promoting to admin, check the admin count limit (max 2)
        if (UserConstant.GROUP_ROLE_ADMIN.equals(role)) {
            int adminCount = memberMapper.countByRole(groupId, UserConstant.GROUP_ROLE_ADMIN);
            if (adminCount >= 2) {
                return Result.error("管理员数量已达上限(2位)");
            }
        }

        memberMapper.updateRole(groupId, targetUserId, role);
        logEvent(groupId, "role_changed", operatorId, targetUserId, null, role);
        return Result.success();
    }

    /**
     * 转让群主
     * <p>
     * 仅群主可操作。转让时原群主自动降为admin，
     * 新群主升级为owner。同时更新群表的owner_id。
     * 记录owner转让日志。
     * </p>
     *
     * @param ownerId    当前群主用户ID
     * @param groupId    群ID
     * @param newOwnerId 新群主用户ID
     * @return 成功返回空结果；不是群主时返回错误
     */
    @Override
    @Transactional
    public Result<?> transferOwnership(Integer ownerId, String groupId, Integer newOwnerId) {
        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null || !group.getOwnerId().equals(ownerId)) {
            return Result.error("只有群主才能转让群主");
        }

        // Verify new owner is a current member of the group
        ChatGroupMember newOwnerMember = memberMapper.selectByGroupAndUser(groupId, newOwnerId);
        if (newOwnerMember == null) {
            return Result.error("新群主必须是当前群成员");
        }

        // New owner becomes owner
        memberMapper.updateRole(groupId, newOwnerId, UserConstant.GROUP_ROLE_OWNER);

        // Old owner gets role 'admin' if within 2 admin limit, otherwise 'member'
        int adminCount = memberMapper.countByRole(groupId, UserConstant.GROUP_ROLE_ADMIN);
        String oldOwnerNewRole = adminCount < 2 ? UserConstant.GROUP_ROLE_ADMIN : UserConstant.GROUP_ROLE_MEMBER;
        memberMapper.updateRole(groupId, ownerId, oldOwnerNewRole);

        // Update the chat_group table's owner_id
        group.setOwnerId(newOwnerId);
        groupMapper.updateGroup(group);
        logEvent(groupId, "role_changed", ownerId, newOwnerId, "owner", "owner");
        return Result.success();
    }

    // ==================== Mute ====================

    /**
     * 禁言成员
     * <p>
     * 权限规则：群主/管理员均可操作，但管理员不能禁言群主或其他管理员。
     * durationMinutes为null或小于等于0时视为永久禁言（mutedUntil为null）。
     * 否则计算到期时间 = now + durationMinutes。
     * </p>
     *
     * @param operatorId      操作者（群主或管理员）
     * @param groupId         群ID
     * @param targetUserId    被禁言用户ID
     * @param durationMinutes 禁言时长（分钟），null或非正数为永久
     * @return 成功返回空结果；权限不足时返回错误
     */
    @Override
    @Transactional
    public Result<?> muteMember(Integer operatorId, String groupId, Integer targetUserId, Integer durationMinutes) {
        if (!isOwnerOrAdmin(operatorId, groupId)) {
            return Result.error("只有群主或管理员才能禁言");
        }

        ChatGroupMember target = memberMapper.selectByGroupAndUser(groupId, targetUserId);
        if (target == null) {
            return Result.error("该用户不是群成员");
        }
        if (isOwnerOrAdmin(targetUserId, groupId) && !isOwner(operatorId, groupId)) {
            return Result.error("管理员不能禁言群主或其他管理员");
        }

        LocalDateTime mutedUntil = durationMinutes != null && durationMinutes > 0
                ? LocalDateTime.now().plusMinutes(durationMinutes) : null;

        memberMapper.updateMute(groupId, targetUserId, true, mutedUntil);
        logEvent(groupId, "member_muted", operatorId, targetUserId, null,
                durationMinutes != null ? durationMinutes + "分钟" : "永久");
        return Result.success();
    }

    /**
     * 解除成员禁言
     * <p>
     * 将isMuted设为false，mutedUntil清空为null。记录member_unmuted事件。
     * </p>
     *
     * @param operatorId   操作者（群主或管理员）
     * @param groupId      群ID
     * @param targetUserId 被解除禁言的用户ID
     * @return 成功返回空结果；权限不足时返回错误
     */
    @Override
    @Transactional
    public Result<?> unmuteMember(Integer operatorId, String groupId, Integer targetUserId) {
        if (!isOwnerOrAdmin(operatorId, groupId)) {
            return Result.error("只有群主或管理员才能解除禁言");
        }

        memberMapper.updateMute(groupId, targetUserId, false, null);
        logEvent(groupId, "member_unmuted", operatorId, targetUserId, null, null);
        return Result.success();
    }

    /**
     * 全员禁言开关
     * <p>
     * 仅群主或管理员可操作。开启后仅群主/admin可发言，
     * 普通成员发送消息时会被拒绝。关闭后恢复正常发言。
     * </p>
     *
     * @param operatorId 操作者（群主或管理员）
     * @param groupId    群ID
     * @param isMutedAll true开启全员禁言，false关闭
     * @return 成功返回空结果；权限不足时返回错误
     */
    @Override
    @Transactional
    public Result<?> setMuteAll(Integer operatorId, String groupId, Boolean isMutedAll) {
        if (!isOwnerOrAdmin(operatorId, groupId)) {
            return Result.error("只有群主或管理员才能设置全员禁言");
        }

        groupMapper.setMuteAll(groupId, isMutedAll);
        logEvent(groupId, isMutedAll ? "mute_all" : "unmute_all", operatorId, null, null, null);
        return Result.success();
    }

    // ==================== Messages ====================

    /**
     * 发送群消息
     * <p>
     * 完整发送流程：
     * <ol>
     *   <li>频率限制检查</li>
     *   <li>成员身份验证</li>
     *   <li>禁言状态检查（个人禁言和全员禁言）</li>
     *   <li>敏感词过滤（发现敏感词拒绝发送）</li>
     *   <li>写入消息记录（含过滤前后内容、敏感词列表）</li>
     *   <li>为群内其他成员累加未读计数</li>
     * </ol>
     * 禁言但禁言已到期时，自动解除禁言并允许发言。
     * </p>
     *
     * @param senderId    发送者用户ID
     * @param groupId     群ID
     * @param content     原始消息内容
     * @param contentType 消息类型（text/image/file等）
     * @param messageId   客户端生成的唯一消息ID（用于幂等去重）
     * @param extraData   扩展数据（如mentionAll、回复引用等）
     * @return 包含messageId、过滤后内容、状态、成员数等信息的Map
     */
    @Override
    @Transactional
    public Result<Map<String, Object>> sendGroupMessage(Integer senderId, String groupId, String content,
                                                         String contentType, String messageId,
                                                         Map<String, Object> extraData) {
        try {
            if (!rateLimitService.checkGroupMessageRate(senderId, groupId)) {
                return Result.error("发送消息过于频繁，请稍后再试");
            }

            ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, senderId);
            if (member == null) {
                return Result.error("你不是该群成员");
            }

            if (member.getIsMuted()) {
                if (member.getMutedUntil() == null || member.getMutedUntil().isAfter(LocalDateTime.now())) {
                    return Result.error("你已被禁言");
                }
                memberMapper.updateMute(groupId, senderId, false, null);
            }

            ChatGroup group = groupMapper.selectByGroupId(groupId);
            if (group != null && group.getIsMutedAll() && !isOwnerOrAdmin(senderId, groupId)) {
                return Result.error("当前已开启全员禁言");
            }

            String filteredContent = sensitiveFilterService.filter(content);
            boolean isFiltered = false;
            String filteredWordsStr = "";
            if (filteredContent == null) {
                return Result.error("消息内容包含敏感词，请修改后重新发送");
            }
            List<String> sensitiveWords = sensitiveFilterService.findAllSensitiveWords(content);
            isFiltered = !sensitiveWords.isEmpty();
            filteredWordsStr = String.join(",", sensitiveWords);

            boolean mentionAll = false;
            if (extraData != null && extraData.containsKey("mentionAll")) {
                mentionAll = Boolean.TRUE.equals(extraData.get("mentionAll"));
            }

            ChatGroupMessage message = new ChatGroupMessage();
            message.setMessageId(messageId);
            message.setGroupId(groupId);
            message.setSenderId(senderId);
            message.setContentType(contentType);
            message.setContent(filteredContent);
            message.setOriginalContent(content);
            message.setStatus("sent");
            message.setIsDeleted(false);
            message.setIsFiltered(isFiltered);
            message.setFilteredWords(filteredWordsStr);
            message.setMentionAll(mentionAll);

            if (extraData != null && !extraData.isEmpty()) {
                try {
                    message.setExtraData(objectMapper.writeValueAsString(extraData));
                } catch (Exception ignored) {
                }
            }

            messageMapper.insert(message);

            List<Integer> memberIds = memberMapper.selectMemberIdsByGroup(groupId);
            for (Integer uid : memberIds) {
                if (!uid.equals(senderId)) {
                    memberMapper.incrementUnreadCount(groupId, uid, 1);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("messageId", messageId);
            result.put("groupId", groupId);
            result.put("senderId", senderId);
            result.put("content", filteredContent);
            result.put("contentType", contentType);
            result.put("status", "sent");
            result.put("timestamp", System.currentTimeMillis());
            result.put("memberCount", memberIds.size());

            // 发送消息后清除消息缓存
            groupCacheService.evictGroupOnMessageChange(groupId);

            return Result.success(result);
        } catch (Exception e) {
            log.error("发送群消息失败", e);
            return Result.error("消息发送失败: " + e.getMessage());
        }
    }

    /**
     * 获取群聊天历史记录
     * <p>
     * 仅群成员可查看。基于lastMessageId的游标分页（比offset分页更高效），
     * pageSize自动钳制在1-50范围内，默认20条。
     * </p>
     *
     * @param userId        当前用户ID
     * @param groupId       群ID
     * @param lastMessageId 游标，首次传null
     * @param pageSize      每页条数
     * @return 历史消息列表
     */
    @Override
    public Result<List<GroupMessageDTO>> getGroupHistory(Integer userId, String groupId, String lastMessageId, int pageSize) {
        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        if (member == null) {
            return Result.error("你不是该群成员");
        }

        if (pageSize <= 0 || pageSize > 50) {
            pageSize = 20;
        }

        // 仅第一页使用缓存
        if (lastMessageId == null || lastMessageId.isEmpty()) {
            List<GroupMessageDTO> cached = groupCacheService.getCachedGroupMessages(groupId);
            if (cached != null) {
                return Result.success(cached);
            }
        }

        List<GroupMessageDTO> messages = messageMapper.selectHistory(groupId, lastMessageId, pageSize);

        if (messages != null && (lastMessageId == null || lastMessageId.isEmpty())) {
            groupCacheService.cacheGroupMessages(groupId, messages);
        }
        return Result.success(messages);
    }

    /**
     * 撤回群消息
     * <p>
     * 仅消息发送者可撤回。撤回后status变为"withdrawn"，
     * content替换为"【消息已撤回】"，同时记录撤回时间和原因。
     * </p>
     *
     * @param userId    发送者用户ID
     * @param groupId   群ID
     * @param messageId 消息ID
     * @param reason    撤回原因（可选）
     * @return 成功返回空结果；非发送者或消息不存在时返回错误
     */
    @Override
    @Transactional
    public Result<?> withdrawGroupMessage(Integer userId, String groupId, String messageId, String reason) {
        ChatGroupMessage message = messageMapper.selectByMessageId(messageId);
        if (message == null) {
            return Result.error("消息不存在");
        }
        if (!message.getSenderId().equals(userId)) {
            return Result.error("只能撤回自己的消息");
        }

        messageMapper.withdrawMessage(messageId, userId, reason);
        // 发送消息后清除消息缓存
        groupCacheService.evictGroupOnMessageChange(groupId);
        return Result.success();
    }

    /**
     * 删除群消息（软删除）
     * <p>
     * 消息发送者可删除自己的消息；群主或管理员可删除任何人的消息。
     * 删除为软删除（is_deleted=1），数据不物理删除。
     * </p>
     *
     * @param userId    用户ID（发送者或管理员）
     * @param groupId   群ID
     * @param messageId 消息ID
     * @return 成功返回空结果；无权限时返回错误
     */
    @Override
    @Transactional
    public Result<?> deleteGroupMessage(Integer userId, String groupId, String messageId) {
        ChatGroupMessage message = messageMapper.selectByMessageId(messageId);
        if (message == null) {
            return Result.error("消息不存在");
        }
        if (!message.getSenderId().equals(userId) && !isOwnerOrAdmin(userId, groupId)) {
            return Result.error("只能删除自己的消息");
        }

        messageMapper.markAsDeleted(messageId);
        // 发送消息后清除消息缓存
        groupCacheService.evictGroupOnMessageChange(groupId);
        return Result.success();
    }

    // ==================== Read Status ====================

    /**
     * 标记消息为已读
     * <p>
     * 在chat_group_message_read表中标记该用户已读此消息，
     * 同时更新chat_group_member表的last_read_message_id和清空unread_count。
     * 此操作幂等，重复调用不会产生副作用。
     * </p>
     *
     * @param userId    用户ID
     * @param groupId   群ID
     * @param messageId 已读到的消息ID
     * @return 操作结果
     */
    @Override
    public Result<?> markAsRead(Integer userId, String groupId, String messageId) {
        readMapper.markAsRead(messageId, userId);
        memberMapper.updateLastRead(groupId, userId, messageId);
        return Result.success();
    }

    /**
     * 获取已读用户列表
     * <p>
     * 查询指定消息的所有已读用户ID列表。常用于群聊中展示
     * "已读x人"或查看具体哪些成员已读。
     * </p>
     *
     * @param groupId   群ID
     * @param messageId 消息ID
     * @return 已读用户信息列表（含userId）
     */
    @Override
    public Result<List<Map<String, Object>>> getReadUsers(String groupId, String messageId) {
        List<Integer> userIds = readMapper.selectReadUserIds(messageId);
        List<Map<String, Object>> users = new ArrayList<>();
        for (Integer uid : userIds) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", uid);
            users.add(userMap);
        }
        return Result.success(users);
    }

    /**
     * 获取未读消息数
     * <p>
     * 从chat_group_member表的unread_count字段直接读取。
     * 非群成员时返回0。
     * </p>
     *
     * @param userId  用户ID
     * @param groupId 群ID
     * @return 未读消息数量，非成员返回0
     */
    @Override
    public Result<Integer> getUnreadCount(Integer userId, String groupId) {
        Integer cached = groupCacheService.getCachedUnreadCount(groupId, userId);
        if (cached != null) {
            return Result.success(cached);
        }
        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        int count = member != null ? member.getUnreadCount() : 0;
        groupCacheService.cacheUnreadCount(groupId, userId, count);
        return Result.success(count);
    }

    // ==================== Query ====================

    /**
     * 获取我的群列表
     * <p>
     * 查询用户所属的所有群（任何角色）。返回结果包含群基本信息
     * 及最后一条消息摘要。结果为空时返回空列表而非null。
     * </p>
     *
     * @param userId 用户ID
     * @return 群列表DTO
     */
    @Override
    public Result<List<GroupListDTO>> getMyGroups(Integer userId) {
        List<GroupListDTO> cached = groupCacheService.getCachedMyGroups(userId);
        if (cached != null) {
            return Result.success(cached);
        }
        List<GroupListDTO> groups = groupMapper.selectMyGroups(userId);
        List<GroupListDTO> result = groups != null ? groups : Collections.emptyList();
        groupCacheService.cacheMyGroups(userId, result);
        return Result.success(result);
    }

    /**
     * 获取群详情信息
     * <p>
     * 返回群完整信息，包括当前用户的角色（非成员为member）
     * 及其个人状态（禁言状态、未读计数）。群不存在时返回错误。
     * </p>
     *
     * @param userId  用户ID（非成员可传null查看基本信息）
     * @param groupId 群ID
     * @return 群详情DTO
     */
    @Override
    public Result<GroupInfoDTO> getGroupInfo(Integer userId, String groupId) {
        GroupInfoDTO cached = groupCacheService.getCachedGroupInfo(groupId);
        if (cached != null && (userId == null || cached.getMyRole() != null)) {
            return Result.success(cached);
        }

        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null) {
            return Result.error("群聊不存在");
        }

        // Non-members can view basic group info, but their role is null (guest)
        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        String role = null;
        if (member != null) {
            role = member.getRole();
        }

        GroupInfoDTO info = buildGroupInfo(userId, group, role);
        groupCacheService.cacheGroupInfo(groupId, info);
        return Result.success(info);
    }

    /**
     * 搜索群聊
     * <p>
     * 按键词模糊匹配群名称，仅返回未解散的群。
     * 结果按成员数量降序、创建时间降序排列。
     * 关键词为空时直接返回错误。
     * </p>
     *
     * @param keyword 搜索关键词
     * @return 匹配的群列表，无匹配时返回空列表
     */
    @Override
    public Result<List<GroupInfoDTO>> searchGroups(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("搜索关键词不能为空");
        }
        List<ChatGroup> groups = groupMapper.searchGroups(keyword.trim());
        List<GroupInfoDTO> result = new ArrayList<>();
        for (ChatGroup g : groups) {
            result.add(buildGroupInfo(null, g, null));
        }
        return Result.success(result);
    }

    /**
     * 获取群成员列表
     * <p>
     * 返回群内所有未离开的成员，按角色优先级（owner>admin>member）
     * 和加入时间排序。包含用户基本信息（头像、昵称）和群角色。
     * </p>
     *
     * @param userId  用户ID（保留参数，当前未做成员身份校验）
     * @param groupId 群ID
     * @return 成员DTO列表
     */
    @Override
    public Result<List<GroupMemberDTO>> getGroupMembers(Integer userId, String groupId) {
        List<GroupMemberDTO> cached = groupCacheService.getCachedGroupMembers(groupId);
        if (cached != null) {
            return Result.success(cached);
        }
        List<GroupMemberDTO> members = memberMapper.selectMembersWithUserInfo(groupId);
        List<GroupMemberDTO> result = members != null ? members : Collections.emptyList();
        groupCacheService.cacheGroupMembers(groupId, result);
        return Result.success(result);
    }

    /**
     * 获取在线成员ID列表
     * <p>
     * 当前版本返回群内所有未离开的成员ID列表。
     * 后续优化可通过WebSocket SessionMap过滤真正在线的成员。
     * </p>
     *
     * @param groupId 群ID
     * @return 成员ID列表
     */
    @Override
    public Result<List<Integer>> getOnlineMembers(String groupId) {
        List<Integer> members = memberMapper.selectMemberIdsByGroup(groupId);
        return Result.success(members != null ? members : Collections.emptyList());
    }

    // ==================== Invite ====================

    /**
     * 生成群邀请码
     * <p>
     * 仅群主或管理员可操作。生成8位大写字母数字混合的随机码，
     * 有效期7天。邀请码写入群记录，支持通过邀请码加入。
     * </p>
     *
     * @param userId  操作者（需为群主或管理员）
     * @param groupId 群ID
     * @return 8位邀请码字符串
     */
    @Override
    @Transactional
    public Result<String> generateInviteCode(Integer userId, String groupId) {
        if (!isOwnerOrAdmin(userId, groupId)) {
            return Result.error("只有群主或管理员才能生成邀请码");
        }

        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        LocalDateTime expireTime = LocalDateTime.now().plusDays(7);
        groupMapper.updateInviteCode(groupId, code, expireTime);
        return Result.success(code);
    }

    // ==================== Events ====================

    /**
     * 获取群事件日志
     * <p>
     * 仅群成员可查看。返回群内所有操作事件按时间倒序排列，
     * 包括：创建、加入、退出、踢人、角色变更、禁言等。
     * </p>
     *
     * @param userId  用户ID（需为群成员）
     * @param groupId 群ID
     * @return 事件记录列表
     */
    @Override
    public Result<List<Map<String, Object>>> getGroupEvents(Integer userId, String groupId) {
        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        if (member == null) {
            return Result.error("你不是该群成员");
        }

        List<ChatGroupEvent> events = eventMapper.selectByGroup(groupId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatGroupEvent event : events) {
            Map<String, Object> map = new HashMap<>();
            map.put("eventType", event.getEventType());
            map.put("operatorId", event.getOperatorId());
            map.put("targetUserId", event.getTargetUserId());
            map.put("oldValue", event.getOldValue());
            map.put("newValue", event.getNewValue());
            map.put("createTime", event.getCreateTime());
            result.add(map);
        }
        return Result.success(result);
    }

    /**
     * 设置本人在群中的群昵称
     * <p>
     * 仅群成员可操作。昵称最多20字符，仅在该群生效。
     * 传null或空字符串视为清空群昵称。
     * </p>
     *
     * @param userId   用户ID
     * @param groupId  群ID
     * @param nickname 群昵称（最多20字符）
     * @return 成功返回空结果；非成员或超长时返回错误
     */
    @Override
    @Transactional
    public Result<?> setMyNickname(Integer userId, String groupId, String nickname) {
        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        if (member == null) {
            return Result.error("你不是该群成员");
        }
        if (nickname != null && nickname.length() > 20) {
            return Result.error("群昵称最多20个字符");
        }
        memberMapper.updateNickname(groupId, userId, nickname);
        return Result.success();
    }

    // ==================== Self-demote ====================

    /**
     * 管理员自行降级为普通成员
     * <p>
     * 仅群管理员可操作，将自己从admin降级为member。
     * 群主不能使用此方法。
     * </p>
     *
     * @param userId  当前用户ID（需为群管理员）
     * @param groupId 群ID
     * @return 成功返回空结果；角色不符合时返回错误
     */
    @Override
    @Transactional
    public Result<?> demoteSelf(Integer userId, String groupId) {
        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null) {
            return Result.error("群聊不存在");
        }

        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        if (member == null) {
            return Result.error("你不是该群成员");
        }
        if (UserConstant.GROUP_ROLE_OWNER.equals(member.getRole())) {
            return Result.error("群主不能自行降级，请先转让群主");
        }
        if (!UserConstant.GROUP_ROLE_ADMIN.equals(member.getRole())) {
            return Result.error("你不是管理员，无需降级");
        }

        memberMapper.updateRole(groupId, userId, UserConstant.GROUP_ROLE_MEMBER);
        logEvent(groupId, "role_changed", userId, userId, "admin", "member");
        return Result.success();
    }

    // ==================== Join Requests ====================

    /**
     * 获取待审批的入群申请列表
     * <p>
     * 仅群主或管理员可查看。从Redis Set中读取待审批的用户ID，
     * 然后查询用户基本信息返回。
     * </p>
     *
     * @param userId  操作者用户ID（需为群主或管理员）
     * @param groupId 群ID
     * @return 申请者列表（含userId, username, avatar）
     */
    @Override
    public Result<List<Map<String, Object>>> getJoinRequests(Integer userId, String groupId) {
        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null) {
            return Result.error("群聊不存在");
        }
        if (!isOwnerOrAdmin(userId, groupId)) {
            return Result.error("只有群主或管理员才能查看入群申请");
        }

        String redisKey = "group:join_requests:" + groupId;
        Set<Object> memberSet = redisUtil.sMembers(redisKey);
        List<Map<String, Object>> result = new ArrayList<>();

        if (memberSet == null || memberSet.isEmpty()) {
            return Result.success(result);
        }

        for (Object obj : memberSet) {
            Integer applicantId = (Integer) obj;
            User user = userMapper.findUserById(applicantId);
            if (user != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("avatar", user.getAvatar());
                result.add(userMap);
            }
        }
        return Result.success(result);
    }

    /**
     * 审批通过入群申请
     * <p>
     * 仅群主或管理员可操作。从Redis Set中移除申请者，并将其加入群。
     * 群已满时拒绝通过。
     * </p>
     *
     * @param operatorId  操作者用户ID（需为群主或管理员）
     * @param groupId     群ID
     * @param applicantId 申请者用户ID
     * @return 成功返回空结果；失败时返回错误
     */
    @Override
    @Transactional
    public Result<?> approveJoinRequest(Integer operatorId, String groupId, Integer applicantId) {
        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null) {
            return Result.error("群聊不存在");
        }
        if (!isOwnerOrAdmin(operatorId, groupId)) {
            return Result.error("只有群主或管理员才能审批入群申请");
        }

        String redisKey = "group:join_requests:" + groupId;
        if (!redisUtil.sIsMember(redisKey, applicantId)) {
            return Result.error("该用户未申请入群");
        }

        // Check if already a member
        ChatGroupMember existing = memberMapper.selectByGroupAndUser(groupId, applicantId);
        if (existing != null) {
            redisUtil.sRemove(redisKey, applicantId);
            return Result.error("该用户已经是群成员");
        }

        if (group.getCurrentMembers() >= group.getMaxMembers()) {
            return Result.error("群成员已满");
        }

        // Remove from Redis set
        redisUtil.sRemove(redisKey, applicantId);

        // Add to group
        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(applicantId);
        member.setRole(UserConstant.GROUP_ROLE_MEMBER);
        member.setIsMuted(false);
        memberMapper.insert(member);

        groupMapper.updateMemberCount(groupId, 1);
        logEvent(groupId, "member_joined", operatorId, applicantId, null, "approved");
        groupCacheService.evictGroupOnMemberChange(groupId, applicantId);
        return Result.success();
    }

    /**
     * 拒绝入群申请
     * <p>
     * 仅群主或管理员可操作。从Redis Set中移除申请者。
     * </p>
     *
     * @param operatorId  操作者用户ID（需为群主或管理员）
     * @param groupId     群ID
     * @param applicantId 申请者用户ID
     * @return 成功返回空结果；失败时返回错误
     */
    @Override
    @Transactional
    public Result<?> rejectJoinRequest(Integer operatorId, String groupId, Integer applicantId) {
        ChatGroup group = groupMapper.selectByGroupId(groupId);
        if (group == null) {
            return Result.error("群聊不存在");
        }
        if (!isOwnerOrAdmin(operatorId, groupId)) {
            return Result.error("只有群主或管理员才能拒绝入群申请");
        }

        String redisKey = "group:join_requests:" + groupId;
        redisUtil.sRemove(redisKey, applicantId);
        return Result.success();
    }

    // ==================== Private Helpers ====================

    /**
     * 判断用户在群中是否为群主或管理员
     * <p>
     * 查询群成员表，检查role字段是否为owner或admin。
     * 用户不在群中时返回false。
     * </p>
     *
     * @param userId  用户ID
     * @param groupId 群ID
     * @return true表示是群主或管理员
     */
    private boolean isOwnerOrAdmin(Integer userId, String groupId) {
        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        return member != null && (UserConstant.GROUP_ROLE_OWNER.equals(member.getRole())
                || UserConstant.GROUP_ROLE_ADMIN.equals(member.getRole()));
    }

    /**
     * 判断用户在群中是否为管理员
     * <p>
     * 仅检查role是否为admin，不包括owner。
     * 用户不在群中时返回false。
     * </p>
     *
     * @param userId  用户ID
     * @param groupId 群ID
     * @return true表示是管理员（不含群主）
     */
    private boolean isAdmin(Integer userId, String groupId) {
        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        return member != null && UserConstant.GROUP_ROLE_ADMIN.equals(member.getRole());
    }

    /**
     * 判断用户在群中是否为群主
     * <p>
     * 仅检查role是否为owner。
     * 用户不在群中时返回false。
     * </p>
     *
     * @param userId  用户ID
     * @param groupId 群ID
     * @return true表示是群主
     */
    private boolean isOwner(Integer userId, String groupId) {
        ChatGroupMember member = memberMapper.selectByGroupAndUser(groupId, userId);
        return member != null && UserConstant.GROUP_ROLE_OWNER.equals(member.getRole());
    }

    /**
     * 构建群详情DTO
     * <p>
     * 将ChatGroup实体和成员信息转换为GroupInfoDTO。
     * 如果userId不为null且role不为null，会额外填充当前用户的
     * 禁言状态和未读计数等个性化信息。
     * </p>
     *
     * @param userId 用户ID（用于查询个人状态，可为null）
     * @param group  群实体对象
     * @param role   当前用户在该群中的角色，可为null
     * @return 群详情DTO
     */
    private GroupInfoDTO buildGroupInfo(Integer userId, ChatGroup group, String role) {
        GroupInfoDTO dto = new GroupInfoDTO();
        dto.setGroupId(group.getGroupId());
        dto.setName(group.getName());
        dto.setAvatar(group.getAvatar());
        dto.setDescription(group.getDescription());
        dto.setOwnerId(group.getOwnerId());
        dto.setMaxMembers(group.getMaxMembers());
        dto.setCurrentMembers(group.getCurrentMembers());
        dto.setJoinPermission(group.getJoinPermission());
        dto.setIsMutedAll(group.getIsMutedAll());
        dto.setInviteCode(group.getInviteCode());
        dto.setInviteExpireTime(group.getInviteExpireTime());
        dto.setIsDissolved(group.getIsDissolved());
        dto.setMyRole(role);
        dto.setCreateTime(group.getCreateTime());

        if (userId != null && role != null) {
            ChatGroupMember member = memberMapper.selectByGroupAndUser(group.getGroupId(), userId);
            if (member != null) {
                dto.setIsMuted(member.getIsMuted());
                dto.setUnreadCount(member.getUnreadCount());
            }
        }

        return dto;
    }

    /**
     * 记录群事件日志
     * <p>
     * 统一的事件日志记录方法。所有群操作（创建、加入、退出、踢人、
     * 角色变更、禁言等）均通过此方法记录。日志写入失败不会影响
     * 主业务流程（catch后只记录error日志）。
     * </p>
     *
     * @param groupId      群ID
     * @param eventType    事件类型标识（如group_created、member_joined、member_kicked）
     * @param operatorId   操作者用户ID
     * @param targetUserId 目标用户ID（被操作者，可为null）
     * @param oldValue     变更前的值（可为null）
     * @param newValue     变更后的值（可为null）
     */
    private void logEvent(String groupId, String eventType, Integer operatorId,
                          Integer targetUserId, String oldValue, String newValue) {
        try {
            ChatGroupEvent event = new ChatGroupEvent();
            event.setGroupId(groupId);
            event.setEventType(eventType);
            event.setOperatorId(operatorId);
            event.setTargetUserId(targetUserId);
            event.setOldValue(oldValue);
            event.setNewValue(newValue);
            eventMapper.insert(event);
        } catch (Exception e) {
            log.error("记录群事件失败 groupId:{} eventType:{}", groupId, eventType, e);
        }
    }

    @Override
    public Map<String, Object> checkGroupNameAvailability(String name, String excludeGroupId) {
        Map<String, Object> result = new HashMap<>();

        if (name == null || name.trim().isEmpty()) {
            result.put("available", false);
            result.put("message", "群名称不能为空");
            return result;
        }

        String trimmed = name.trim();
        if (trimmed.length() < 2) {
            result.put("available", false);
            result.put("message", "群名称不能少于2个字符");
            return result;
        }
        if (trimmed.length() > 50) {
            result.put("available", false);
            result.put("message", "群名称不能超过50个字符");
            return result;
        }

        ChatGroup existing = groupMapper.selectByName(trimmed);
        if (existing != null && (excludeGroupId == null || !existing.getGroupId().equals(excludeGroupId))) {
            result.put("available", false);
            result.put("message", "群名称已存在");
            return result;
        }

        result.put("available", true);
        result.put("message", "群名称可用");
        return result;
    }
}
