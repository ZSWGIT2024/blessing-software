package com.itheima.service;

import com.itheima.dto.*;
import com.itheima.pojo.Result;

import java.util.List;
import java.util.Map;

/**
 * 群聊服务接口
 * <p>
 * 提供群聊的完整生命周期管理，包括：
 * <ul>
 *   <li>群的创建、编辑、解散</li>
 *   <li>成员的加入、退出、踢出</li>
 *   <li>角色管理（管理员任免、群主转让）</li>
 *   <li>禁言管理（单人禁言、全员禁言）</li>
 *   <li>消息发送、历史查询、撤回、删除</li>
 *   <li>已读状态管理</li>
 *   <li>群信息查询与搜索</li>
 *   <li>邀请码生成与群事件日志</li>
 * </ul>
 * </p>
 *
 * @author idealU
 * @since 1.0
 */
public interface GroupChatService {

    // Group CRUD

    /**
     * 创建群聊
     * <p>
     * 业务规则：
     * <ul>
     *   <li>群名称不能为空，最多50个字符</li>
     *   <li>VIP用户可创建的群数量上限高于普通用户</li>
     *   <li>群主自动成为群成员，角色为owner</li>
     *   <li>自动生成16位唯一群ID</li>
     *   <li>记录群创建事件日志</li>
     * </ul>
     * </p>
     *
     * @param userId 创建者用户ID
     * @param dto    创建群请求参数，包含群名称、头像、描述、最大成员数、加入权限等
     * @return 创建成功返回群详情信息，失败返回错误消息
     */
    Result<GroupInfoDTO> createGroup(Integer userId, CreateGroupDTO dto);

    /**
     * 更新群信息
     * <p>
     * 仅群主或管理员可操作。支持更新群名称、头像、描述、最大成员数、加入权限。
     * 更新后记录群编辑事件日志。
     * </p>
     *
     * @param userId 操作者用户ID（需为群主或管理员）
     * @param dto    更新群请求参数，包含群ID及要更新的字段
     * @return 更新后的群详情信息，权限不足或群不存在时返回错误
     */
    Result<GroupInfoDTO> updateGroup(Integer userId, UpdateGroupDTO dto);

    /**
     * 解散群聊
     * <p>
     * 仅群主可操作。解散后群标记为已解散状态，成员无法继续发送消息。
     * 记录群解散事件日志。
     * </p>
     *
     * @param userId  操作者用户ID（需为群主）
     * @param groupId 群ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> dissolveGroup(Integer userId, String groupId);

    // Membership

    /**
     * 加入群聊
     * <p>
     * 支持两种入群方式：
     * <ul>
     *   <li>通过群ID直接加入（需要符合群加入权限设置）</li>
     *   <li>通过邀请码加入（需要有效的邀请码）</li>
     * </ul>
     * 禁止重复加入、群已满或群已解散时不允许加入。加入后自动记录事件日志。
     * </p>
     *
     * @param userId 用户ID
     * @param dto    加入群请求参数，可包含群ID或邀请码
     * @return 加入成功返回群详情信息，失败返回错误消息
     */
    Result<GroupInfoDTO> joinGroup(Integer userId, JoinGroupDTO dto);

    /**
     * 退出群聊
     * <p>
     * 群主不能直接退出，需先转让群主。退出后标记离开时间，减少群成员计数。
     * 记录成员退出事件日志。
     * </p>
     *
     * @param userId  用户ID
     * @param groupId 群ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> leaveGroup(Integer userId, String groupId);

    /**
     * 踢出成员
     * <p>
     * 仅群主或管理员可操作。不能踢出群主；管理员不能踢出其他管理员。
     * 被踢出成员标记离开时间，减少群成员计数。记录踢人事件日志。
     * </p>
     *
     * @param operatorId   操作者用户ID（需为群主或管理员）
     * @param groupId      群ID
     * @param targetUserId 被踢出的目标用户ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> kickMember(Integer operatorId, String groupId, Integer targetUserId);

    // Roles

    /**
     * 修改成员角色
     * <p>
     * 仅群主可操作。可将成员设为管理员(admin)或普通成员(member)。
     * 记录角色变更事件日志。
     * </p>
     *
     * @param operatorId   操作者用户ID（需为群主）
     * @param groupId      群ID
     * @param targetUserId 目标用户ID
     * @param role         新角色（admin或member）
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> changeMemberRole(Integer operatorId, String groupId, Integer targetUserId, String role);

    /**
     * 转让群主
     * <p>
     * 仅群主可操作。原群主降为管理员，新群主升为owner，更新群的owner_id。
     * 记录群主转让事件日志。
     * </p>
     *
     * @param ownerId    当前群主用户ID
     * @param groupId    群ID
     * @param newOwnerId 新群主用户ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> transferOwnership(Integer ownerId, String groupId, Integer newOwnerId);

    // Mute

    /**
     * 禁言成员
     * <p>
     * 仅群主或管理员可操作。管理员不能禁言群主或其他管理员。
     * durationMinutes为null或0表示永久禁言，否则为限时禁言。
     * 记录禁言事件日志。
     * </p>
     *
     * @param operatorId      操作者用户ID
     * @param groupId         群ID
     * @param targetUserId    被禁言的目标用户ID
     * @param durationMinutes 禁言时长（分钟），null或0表示永久禁言
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> muteMember(Integer operatorId, String groupId, Integer targetUserId, Integer durationMinutes);

    /**
     * 解除禁言
     * <p>
     * 仅群主或管理员可操作。清除成员的禁言状态和到期时间。
     * 记录解除禁言事件日志。
     * </p>
     *
     * @param operatorId   操作者用户ID
     * @param groupId      群ID
     * @param targetUserId 被解除禁言的目标用户ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> unmuteMember(Integer operatorId, String groupId, Integer targetUserId);

    /**
     * 全员禁言开关
     * <p>
     * 仅群主或管理员可操作。开启后仅群主和管理员可以发言。
     * 记录全员禁言/解禁事件日志。
     * </p>
     *
     * @param operatorId 操作者用户ID
     * @param groupId    群ID
     * @param isMutedAll true开启全员禁言，false关闭
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> setMuteAll(Integer operatorId, String groupId, Boolean isMutedAll);

    // Messages

    /**
     * 发送群消息
     * <p>
     * 业务规则：
     * <ul>
     *   <li>发送前检查频率限制，防止刷屏</li>
     *   <li>被禁言或全员禁言时（非群主/管理员）不允许发送</li>
     *   <li>消息内容经过敏感词过滤，包含敏感词时拒绝发送</li>
     *   <li>支持@全体成员（mentionAll）等扩展数据</li>
     *   <li>发送成功后自动为群内其他成员增加未读计数</li>
     * </ul>
     * </p>
     *
     * @param senderId    发送者用户ID
     * @param groupId     群ID
     * @param content     消息内容（原始内容）
     * @param contentType 消息类型（text/image/file等）
     * @param messageId   客户端生成的唯一消息ID
     * @param extraData   扩展数据（如@提及信息）
     * @return 包含消息ID、发送状态和时间戳的结果
     */
    Result<Map<String, Object>> sendGroupMessage(Integer senderId, String groupId, String content,
                                                  String contentType, String messageId,
                                                  Map<String, Object> extraData);

    /**
     * 获取群聊天历史记录
     * <p>
     * 仅群成员可查看。支持基于lastMessageId的游标分页，每次最多50条。
     * pageSize超出范围时自动修正为20条默认值。
     * </p>
     *
     * @param userId        用户ID（需为群成员）
     * @param groupId       群ID
     * @param lastMessageId 上一页最后一条消息ID，首次请求传null
     * @param pageSize      每页条数（1-50）
     * @return 消息列表，按时间倒序
     */
    Result<List<GroupMessageDTO>> getGroupHistory(Integer userId, String groupId, String lastMessageId, int pageSize);

    /**
     * 撤回群消息
     * <p>
     * 仅消息发送者可撤回自己的消息。撤回后消息状态变为"withdrawn"，
     * 内容替换为"【消息已撤回】"。
     * </p>
     *
     * @param userId    用户ID（需为消息发送者）
     * @param groupId   群ID
     * @param messageId 消息ID
     * @param reason    撤回原因（可选）
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> withdrawGroupMessage(Integer userId, String groupId, String messageId, String reason);

    /**
     * 删除群消息
     * <p>
     * 消息发送者可直接删除自己的消息。群主或管理员可删除任何人的消息。
     * 删除为软删除，标记is_deleted标志。
     * </p>
     *
     * @param userId    用户ID（发送者或群主/管理员）
     * @param groupId   群ID
     * @param messageId 消息ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> deleteGroupMessage(Integer userId, String groupId, String messageId);

    // Read status

    /**
     * 标记消息为已读
     * <p>
     * 标记指定消息为已读，并更新群成员的最后读取消息ID和清空未读计数。
     * </p>
     *
     * @param userId    用户ID
     * @param groupId   群ID
     * @param messageId 已读到的消息ID
     * @return 操作成功返回空结果
     */
    Result<?> markAsRead(Integer userId, String groupId, String messageId);

    /**
     * 获取已读用户列表
     * <p>
     * 查询指定消息的所有已读用户ID列表。
     * </p>
     *
     * @param groupId   群ID
     * @param messageId 消息ID
     * @return 已读用户的ID及其基本信息列表
     */
    Result<List<Map<String, Object>>> getReadUsers(String groupId, String messageId);

    /**
     * 获取未读消息数
     * <p>
     * 获取指定用户在指定群中的未读消息数量。
     * </p>
     *
     * @param userId  用户ID
     * @param groupId 群ID
     * @return 未读消息数
     */
    Result<Integer> getUnreadCount(Integer userId, String groupId);

    // Query

    /**
     * 获取我的群列表
     * <p>
     * 查询当前用户所属的所有群聊（包括作为群主、管理员或普通成员加入的群）。
     * </p>
     *
     * @param userId 用户ID
     * @return 群列表摘要信息
     */
    Result<List<GroupListDTO>> getMyGroups(Integer userId);

    /**
     * 获取群详情
     * <p>
     * 查询指定群的完整信息，包括群设置、当前用户的角色和禁言状态。
     * 非群成员也可查看基本信息，但不会返回个人专属数据。
     * </p>
     *
     * @param userId  用户ID（可为null，表示游客查看）
     * @param groupId 群ID
     * @return 群详情信息，群不存在时返回错误
     */
    Result<GroupInfoDTO> getGroupInfo(Integer userId, String groupId);

    /**
     * 搜索群聊
     * <p>
     * 按关键字模糊匹配群名称，返回未解散的群。
     * 结果按成员数量和创建时间排序。
     * </p>
     *
     * @param keyword 搜索关键词
     * @return 匹配的群列表
     */
    Result<List<GroupInfoDTO>> searchGroups(String keyword);

    /**
     * 获取群成员列表
     * <p>
     * 查询指定群的所有成员，按角色（群主>管理员>成员）和加入时间排序，
     * 包含用户基本信息（头像、昵称等）。
     * </p>
     *
     * @param userId  用户ID
     * @param groupId 群ID
     * @return 成员列表
     */
    Result<List<GroupMemberDTO>> getGroupMembers(Integer userId, String groupId);

    /**
     * 获取在线成员列表
     * <p>
     * 查询指定群中当前在线的成员ID列表。
     * </p>
     *
     * @param groupId 群ID
     * @return 在线成员ID列表
     */
    Result<List<Integer>> getOnlineMembers(String groupId);

    // Invite

    /**
     * 生成邀请码
     * <p>
     * 仅群主或管理员可操作。生成8位大写字母数字邀请码，有效期7天。
     * 邀请码存储在群记录中，其他用户可通过邀请码加入该群。
     * </p>
     *
     * @param userId  用户ID（需为群主或管理员）
     * @param groupId 群ID
     * @return 生成的8位邀请码
     */
    Result<String> generateInviteCode(Integer userId, String groupId);

    // Events

    /**
     * 获取群事件日志
     * <p>
     * 仅群成员可查看。返回群内所有操作事件记录，如创建、加入、退出、
     * 踢人、角色变更、禁言等，以时间倒序排列。
     * </p>
     *
     * @param userId  用户ID（需为群成员）
     * @param groupId 群ID
     * @return 事件记录列表
     */
    Result<List<Map<String, Object>>> getGroupEvents(Integer userId, String groupId);

    // Nickname

    /**
     * 设置本人在群中的昵称
     * <p>
     * 仅群成员可操作。群昵称最多20个字符，仅在该群中生效，
     * 不影响用户全局昵称。
     * </p>
     *
     * @param userId   用户ID
     * @param groupId  群ID
     * @param nickname 群昵称（最多20个字符）
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> setMyNickname(Integer userId, String groupId, String nickname);

    // Self-demote

    /**
     * 管理员自行降级为普通成员
     * <p>
     * 仅群管理员可操作，将自己从admin降为member。
     * 群主不能使用此方法（需转让群主后退出）。
     * </p>
     *
     * @param userId  当前用户ID（需为群管理员）
     * @param groupId 群ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> demoteSelf(Integer userId, String groupId);

    // Join requests (approval-based groups)

    /**
     * 获取待审批的入群申请列表
     * <p>
     * 仅群主或管理员可查看。返回申请入群的用户基本信息列表。
     * 适用于join_permission为approval的群。
     * </p>
     *
     * @param userId  操作者用户ID（需为群主或管理员）
     * @param groupId 群ID
     * @return 申请者列表（含userId, username, avatar）
     */
    Result<List<Map<String, Object>>> getJoinRequests(Integer userId, String groupId);

    /**
     * 审批通过入群申请
     * <p>
     * 仅群主或管理员可操作。将申请者从Redis待审批集合中移除，
     * 并将其加入群。群已满时无法通过。
     * </p>
     *
     * @param userId      操作者用户ID（需为群主或管理员）
     * @param groupId     群ID
     * @param applicantId 申请者用户ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> approveJoinRequest(Integer userId, String groupId, Integer applicantId);

    /**
     * 拒绝入群申请
     * <p>
     * 仅群主或管理员可操作。将申请者从Redis待审批集合中移除。
     * </p>
     *
     * @param userId      操作者用户ID（需为群主或管理员）
     * @param groupId     群ID
     * @param applicantId 申请者用户ID
     * @return 操作成功返回空结果，否则返回错误消息
     */
    Result<?> rejectJoinRequest(Integer userId, String groupId, Integer applicantId);

    /**
     * 检查群名称是否可用
     * <p>
     * 校验群名称是否已被其他未解散的群占用。
     * 当 excludeGroupId 不为null时，排除指定群（用于编辑群名时忽略自身）。
     * </p>
     *
     * @param name           群名称
     * @param excludeGroupId 排除的群ID（可为null）
     * @return {available: bool, message: str}
     */
    Map<String, Object> checkGroupNameAvailability(String name, String excludeGroupId);
}
