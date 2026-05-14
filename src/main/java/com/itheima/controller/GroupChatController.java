package com.itheima.controller;

import com.itheima.dto.*;
import com.itheima.pojo.Result;
import com.itheima.service.GroupChatService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 群聊控制器
 * <p>
 * 提供群聊的创建、编辑、解散、成员管理、角色变更、禁言、消息收发、
 * 消息撤回/删除、已读状态、群组查询、邀请码生成、群事件查询和昵称设置等功能。
 * 所有需要用户身份的操作均从 ThreadLocal 中获取当前登录用户信息。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;

    /**
     * 检查群名称是否可用（实时校验）
     *
     * @param name           群名称
     * @param excludeGroupId 排除的群ID（编辑群名时传入，避免与自己当前的群名冲突）
     * @return {available: bool, message: str}
     */
    @GetMapping("/checkName")
    public Result<Map<String, Object>> checkGroupName(
            @RequestParam("name") String name,
            @RequestParam(value = "excludeGroupId", required = false) String excludeGroupId) {
        Map<String, Object> data = groupChatService.checkGroupNameAvailability(name, excludeGroupId);
        return Result.success(data);
    }

    // ==================== Group CRUD ====================

    /**
     * 创建群聊
     *
     * @param dto 创建群聊的请求参数，包含群名称、群头像、成员列表等信息
     * @return 包含新创建群组详细信息的 Result 对象
     */
    @PostMapping("/create")
    public Result<GroupInfoDTO> createGroup(@RequestBody CreateGroupDTO dto) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.createGroup(userId, dto);
    }

    /**
     * 更新群聊信息
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param dto     包含要更新的群组字段（如群名称、群头像、公告等）
     * @return 包含更新后群组详细信息的 Result 对象
     */
    @PutMapping("/{groupId}")
    public Result<GroupInfoDTO> updateGroup(@PathVariable("groupId") String groupId, @RequestBody UpdateGroupDTO dto) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        dto.setGroupId(groupId);
        return groupChatService.updateGroup(userId, dto);
    }

    /**
     * 解散群聊
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @DeleteMapping("/{groupId}")
    public Result<?> dissolveGroup(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.dissolveGroup(userId, groupId);
    }

    // ==================== Membership ====================

    /**
     * 加入群聊
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param dto     包含加入方式（如通过邀请码）的参数
     * @return 包含群组详细信息的 Result 对象
     */
    @PostMapping("/{groupId}/join")
    public Result<GroupInfoDTO> joinGroup(@PathVariable("groupId") String groupId, @RequestBody JoinGroupDTO dto) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        dto.setGroupId(groupId);
        return groupChatService.joinGroup(userId, dto);
    }

    /**
     * 退出群聊
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @PostMapping("/{groupId}/leave")
    public Result<?> leaveGroup(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.leaveGroup(userId, groupId);
    }

    /**
     * 踢出群成员
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含被踢用户 userId 的请求体
     * @return 操作结果的 Result 对象
     */
    @PostMapping("/{groupId}/kick")
    public Result<?> kickMember(@PathVariable("groupId") String groupId, @RequestBody Map<String, Integer> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer targetUserId = params.get("userId");
        return groupChatService.kickMember(userId, groupId, targetUserId);
    }

    // ==================== Roles ====================

    /**
     * 变更群成员角色
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含目标用户 userId 和新角色 role 的请求体
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/{groupId}/member/role")
    public Result<?> changeMemberRole(@PathVariable("groupId") String groupId, @RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer targetUserId = (Integer) params.get("userId");
        String role = (String) params.get("role");
        return groupChatService.changeMemberRole(userId, groupId, targetUserId, role);
    }

    /**
     * 转让群主身份
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含新群主 newOwnerId 的请求体
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/{groupId}/transfer")
    public Result<?> transferOwnership(@PathVariable("groupId") String groupId, @RequestBody Map<String, Integer> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer newOwnerId = params.get("newOwnerId");
        return groupChatService.transferOwnership(userId, groupId, newOwnerId);
    }

    /**
     * 管理员自行降级为普通成员
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/{groupId}/member/demote")
    public Result<?> demoteSelf(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.demoteSelf(userId, groupId);
    }

    // ==================== Mute ====================

    /**
     * 禁言群成员
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含目标用户 userId 和禁言时长 durationMinutes（分钟，null 表示永久禁言）的请求体
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/{groupId}/member/mute")
    public Result<?> muteMember(@PathVariable("groupId") String groupId, @RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer targetUserId = (Integer) params.get("userId");
        Integer durationMinutes = params.get("durationMinutes") != null
                ? ((Number) params.get("durationMinutes")).intValue() : null;
        return groupChatService.muteMember(userId, groupId, targetUserId, durationMinutes);
    }

    /**
     * 解除群成员禁言
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含目标用户 userId 的请求体
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/{groupId}/member/unmute")
    public Result<?> unmuteMember(@PathVariable("groupId") String groupId, @RequestBody Map<String, Integer> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer targetUserId = params.get("userId");
        return groupChatService.unmuteMember(userId, groupId, targetUserId);
    }

    /**
     * 设置全员禁言状态
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含全员禁言标识 isMutedAll 的请求体
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/{groupId}/mute-all")
    public Result<?> setMuteAll(@PathVariable("groupId") String groupId, @RequestBody Map<String, Boolean> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Boolean isMutedAll = params.get("isMutedAll");
        return groupChatService.setMuteAll(userId, groupId, isMutedAll);
    }

    // ==================== Messages ====================

    /**
     * 发送群聊消息
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含消息内容 content、消息类型 contentType、消息ID messageId 和额外数据 extraData 的请求体
     * @return 包含发送结果信息（如消息ID、发送时间等）的 Result 对象
     */
    @PostMapping("/{groupId}/message/send")
    public Result<Map<String, Object>> sendMessage(@PathVariable("groupId") String groupId, @RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String content = (String) params.get("content");
        String contentType = (String) params.get("contentType");
        String messageId = (String) params.get("messageId");
        @SuppressWarnings("unchecked")
        Map<String, Object> extraData = (Map<String, Object>) params.get("extraData");
        return groupChatService.sendGroupMessage(userId, groupId, content, contentType, messageId, extraData);
    }

    /**
     * 获取群聊历史消息
     *
     * @param groupId       群组ID，作为路径变量传入
     * @param lastMessageId 上一次拉取的最后一条消息ID，用于分页（可选）
     * @param pageSize      每页消息数量，默认值为 20
     * @return 包含历史消息列表的 Result 对象
     */
    @GetMapping("/{groupId}/messages")
    public Result<List<GroupMessageDTO>> getGroupHistory(@PathVariable("groupId") String groupId,
                                                          @RequestParam(required = false, value = "lastMessageId") String lastMessageId,
                                                          @RequestParam(defaultValue = "20", value = "pageSize") int pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.getGroupHistory(userId, groupId, lastMessageId, pageSize);
    }

    /**
     * 撤回群聊消息
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含消息ID messageId 和撤回原因 reason 的请求体
     * @return 操作结果的 Result 对象
     */
    @PostMapping("/{groupId}/message/withdraw")
    public Result<?> withdrawMessage(@PathVariable("groupId") String groupId, @RequestBody Map<String, String> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String messageId = params.get("messageId");
        String reason = params.get("reason");
        return groupChatService.withdrawGroupMessage(userId, groupId, messageId, reason);
    }

    /**
     * 删除群聊消息（管理员操作）
     *
     * @param groupId   群组ID，作为路径变量传入
     * @param messageId 消息ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @DeleteMapping("/{groupId}/message/{messageId}")
    public Result<?> deleteMessage(@PathVariable("groupId") String groupId, @PathVariable("messageId") String messageId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.deleteGroupMessage(userId, groupId, messageId);
    }

    // ==================== Read Status ====================

    /**
     * 标记消息为已读
     *
     * @param groupId   群组ID，作为路径变量传入
     * @param messageId 消息ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/{groupId}/read/{messageId}")
    public Result<?> markAsRead(@PathVariable("groupId") String groupId, @PathVariable("messageId") String messageId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.markAsRead(userId, groupId, messageId);
    }

    /**
     * 获取已读消息的用户列表
     *
     * @param groupId   群组ID，作为路径变量传入
     * @param messageId 消息ID，作为路径变量传入
     * @return 包含已读用户列表的 Result 对象，每个元素包含用户ID和已读时间等信息
     */
    @GetMapping("/{groupId}/message/{messageId}/read-users")
    public Result<List<Map<String, Object>>> getReadUsers(@PathVariable("groupId") String groupId, @PathVariable("messageId") String messageId) {
        return groupChatService.getReadUsers(groupId, messageId);
    }

    /**
     * 获取群聊未读消息数
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 包含当前用户未读消息数量的 Result 对象
     */
    @GetMapping("/{groupId}/unread")
    public Result<Integer> getUnreadCount(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.getUnreadCount(userId, groupId);
    }

    // ==================== Query ====================

    /**
     * 获取当前用户加入的所有群组列表
     *
     * @return 包含群组摘要信息列表的 Result 对象
     */
    @GetMapping("/my")
    public Result<List<GroupListDTO>> getMyGroups() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.getMyGroups(userId);
    }

    /**
     * 获取群组详细信息
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 包含群组详细信息的 Result 对象
     */
    @GetMapping("/{groupId}")
    public Result<GroupInfoDTO> getGroupInfo(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.getGroupInfo(userId, groupId);
    }

    /**
     * 按关键词搜索群组
     *
     * @param keyword 搜索关键词，作为请求参数传入
     * @return 包含匹配群组列表的 Result 对象
     */
    @GetMapping("/search")
    public Result<List<GroupInfoDTO>> searchGroups(@RequestParam("keyword") String keyword) {
        return groupChatService.searchGroups(keyword);
    }

    /**
     * 获取群成员列表
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 包含群成员信息列表的 Result 对象
     */
    @GetMapping("/{groupId}/members")
    public Result<List<GroupMemberDTO>> getGroupMembers(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.getGroupMembers(userId, groupId);
    }

    /**
     * 获取群在线成员列表
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 包含在线用户ID列表的 Result 对象
     */
    @GetMapping("/{groupId}/members/online")
    public Result<List<Integer>> getOnlineMembers(@PathVariable("groupId") String groupId) {
        return groupChatService.getOnlineMembers(groupId);
    }

    // ==================== Invite ====================

    /**
     * 生成群聊邀请码
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 包含邀请码字符串的 Result 对象
     */
    @GetMapping("/{groupId}/invite-code")
    public Result<String> generateInviteCode(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.generateInviteCode(userId, groupId);
    }

    // ==================== Events ====================

    /**
     * 获取群聊事件列表（如成员加入、退出、角色变更等）
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 包含群事件记录的 Result 对象
     */
    @GetMapping("/{groupId}/events")
    public Result<List<Map<String, Object>>> getGroupEvents(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.getGroupEvents(userId, groupId);
    }

    // ==================== Nickname ====================

    /**
     * 设置当前用户在群内的昵称
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param params  包含新昵称 nickname 的请求体
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/{groupId}/nickname")
    public Result<?> setMyNickname(@PathVariable("groupId") String groupId, @RequestBody Map<String, String> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String nickname = params.get("nickname");
        return groupChatService.setMyNickname(userId, groupId, nickname);
    }

    // ==================== Join Requests ====================

    /**
     * 获取待审批的入群申请列表
     *
     * @param groupId 群组ID，作为路径变量传入
     * @return 包含申请者列表的 Result 对象
     */
    @GetMapping("/{groupId}/join-requests")
    public Result<List<Map<String, Object>>> getJoinRequests(@PathVariable("groupId") String groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return groupChatService.getJoinRequests(userId, groupId);
    }

    /**
     * 审批通过入群申请
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param userId  申请者用户ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @PostMapping("/{groupId}/approve/{userId}")
    public Result<?> approveJoinRequest(@PathVariable("groupId") String groupId, @PathVariable("userId") Integer userId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        return groupChatService.approveJoinRequest(id, groupId, userId);
    }

    /**
     * 拒绝入群申请
     *
     * @param groupId 群组ID，作为路径变量传入
     * @param userId  申请者用户ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @PostMapping("/{groupId}/reject/{userId}")
    public Result<?> rejectJoinRequest(@PathVariable("groupId") String groupId, @PathVariable("userId") Integer userId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        return groupChatService.rejectJoinRequest(id, groupId, userId);
    }
}
