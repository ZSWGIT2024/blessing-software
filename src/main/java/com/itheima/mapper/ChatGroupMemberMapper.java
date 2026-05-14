package com.itheima.mapper;

import com.itheima.dto.GroupMemberDTO;
import com.itheima.pojo.ChatGroupMember;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群成员 Mapper 接口
 * <p>
 * 负责 chat_group_member 表的数据库操作。管理群成员的完整生命周期，
 * 包括成员的加入、退出（软删除）、角色变更、禁言状态、群昵称、
 * 未读计数和已读状态等。查询时自动过滤已离开的成员（leave_time IS NULL）。
 * </p>
 *
 * @author idealU
 * @since 1.0
 */
@Mapper
public interface ChatGroupMemberMapper {

    /**
     * 插入新群成员记录
     * <p>
     * join_time由数据库NOW()自动设置。inviterId记录邀请人信息（可选）。
     * 使用自增主键回填id。
     * </p>
     *
     * @param member 群成员实体（需包含groupId、userId、role、isMuted等）
     * @return 影响行数
     */
    @Insert("INSERT INTO chat_group_member (group_id, user_id, role, nickname_in_group, is_muted, " +
            "muted_until, join_time, inviter_id) VALUES (#{groupId}, #{userId}, #{role}, #{nicknameInGroup}, " +
            "#{isMuted}, #{mutedUntil}, NOW(), #{inviterId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatGroupMember member);

    /**
     * 按群ID和用户ID查询成员
     * <p>
     * 仅返回未离开的成员（leave_time IS NULL）。用于判断用户是否
     * 在群中及获取其角色、禁言状态等信息。
     * </p>
     *
     * @param groupId 群ID
     * @param userId  用户ID
     * @return 成员实体，不存在或已离开时返回null
     */
    @Select("SELECT * FROM chat_group_member WHERE group_id = #{groupId} AND user_id = #{userId} AND leave_time IS NULL")
    ChatGroupMember selectByGroupAndUser(@Param("groupId") String groupId, @Param("userId") Integer userId);

    /**
     * 查询群的所有成员（按角色优先级排序）
     * <p>
     * 仅返回未离开的成员。排序规则：群主(owner)最先，
     * 其次管理员(admin)，最后普通成员(member)，同角色按加入时间升序。
     * </p>
     *
     * @param groupId 群ID
     * @return 成员列表
     */
    @Select("SELECT * FROM chat_group_member WHERE group_id = #{groupId} AND leave_time IS NULL ORDER BY " +
            "CASE role WHEN 'owner' THEN 0 WHEN 'admin' THEN 1 ELSE 2 END, join_time ASC")
    List<ChatGroupMember> selectMembersByGroup(@Param("groupId") String groupId);

    /**
     * 查询用户加入的所有群成员关系
     * <p>
     * 返回指定用户在所有群中的成员关系记录（未离开的）。
     * 可用于获取用户的群列表。
     * </p>
     *
     * @param userId 用户ID
     * @return 成员关系列表
     */
    @Select("SELECT * FROM chat_group_member WHERE user_id = #{userId} AND leave_time IS NULL")
    List<ChatGroupMember> selectUserGroups(@Param("userId") Integer userId);

    /**
     * 获取群的所有成员用户ID列表
     * <p>
     * 仅返回user_id。用于消息发送时为其他成员增加未读计数等场景。
     * </p>
     *
     * @param groupId 群ID
     * @return 用户ID列表
     */
    @Select("SELECT user_id FROM chat_group_member WHERE group_id = #{groupId} AND leave_time IS NULL")
    List<Integer> selectMemberIdsByGroup(@Param("groupId") String groupId);

    /**
     * 查询群成员详细信息（含用户基本信息）
     * <p>
     * 通过XML Mapper实现（ChatGroupMemberMapper.xml），联表查询
     * 用户头像、昵称等信息。用于群成员列表展示。
     * </p>
     *
     * @param groupId 群ID
     * @return 成员DTO列表（含用户基本信息和群角色）
     */
    List<GroupMemberDTO> selectMembersWithUserInfo(@Param("groupId") String groupId);

    /**
     * 更新成员角色
     * <p>
     * 直接更新成员角色字段。角色变更权限校验由Service层完成。
     * </p>
     *
     * @param groupId 群ID
     * @param userId  用户ID
     * @param role    新角色（owner/admin/member）
     * @return 影响行数
     */
    @Update("UPDATE chat_group_member SET role = #{role} WHERE group_id = #{groupId} AND user_id = #{userId}")
    int updateRole(@Param("groupId") String groupId, @Param("userId") Integer userId, @Param("role") String role);

    /**
     * 更新成员禁言状态
     * <p>
     * 同时更新is_muted标志和禁言到期时间muted_until。
     * 解除禁言时isMuted=false、mutedUntil=null。
     * </p>
     *
     * @param groupId    群ID
     * @param userId     用户ID
     * @param isMuted    是否禁言
     * @param mutedUntil 禁言到期时间（null表示永久或已解除）
     * @return 影响行数
     */
    @Update("UPDATE chat_group_member SET is_muted = #{isMuted}, muted_until = #{mutedUntil} " +
            "WHERE group_id = #{groupId} AND user_id = #{userId}")
    int updateMute(@Param("groupId") String groupId, @Param("userId") Integer userId,
                   @Param("isMuted") Boolean isMuted, @Param("mutedUntil") LocalDateTime mutedUntil);

    /**
     * 更新成员在群中的昵称
     * <p>
     * 群昵称仅在该群中生效，不影响用户全局昵称。
     * </p>
     *
     * @param groupId  群ID
     * @param userId   用户ID
     * @param nickname 新群昵称
     * @return 影响行数
     */
    @Update("UPDATE chat_group_member SET nickname_in_group = #{nickname} " +
            "WHERE group_id = #{groupId} AND user_id = #{userId}")
    int updateNickname(@Param("groupId") String groupId, @Param("userId") Integer userId,
                       @Param("nickname") String nickname);

    /**
     * 累加未读消息计数
     * <p>
     * 对unread_count字段做增量更新。发送新消息时delta=1，
     * 标记已读时通过updateLastRead方法归零。
     * </p>
     *
     * @param groupId 群ID
     * @param userId  用户ID
     * @param delta   增量值
     * @return 影响行数
     */
    @Update("UPDATE chat_group_member SET unread_count = unread_count + #{delta} " +
            "WHERE group_id = #{groupId} AND user_id = #{userId}")
    int incrementUnreadCount(@Param("groupId") String groupId, @Param("userId") Integer userId,
                             @Param("delta") int delta);

    /**
     * 更新最后已读消息（标记群为已读）
     * <p>
     * 更新last_read_time、last_read_message_id，并将unread_count清零。
     * 通常在用户打开群聊或手动标记已读时调用。
     * </p>
     *
     * @param groupId   群ID
     * @param userId    用户ID
     * @param messageId 最后已读的消息ID
     * @return 影响行数
     */
    @Update("UPDATE chat_group_member SET last_read_time = NOW(), last_read_message_id = #{messageId}, " +
            "unread_count = 0 WHERE group_id = #{groupId} AND user_id = #{userId}")
    int updateLastRead(@Param("groupId") String groupId, @Param("userId") Integer userId,
                       @Param("messageId") String messageId);

    /**
     * 标记成员离开群（软删除）
     * <p>
     * 设置leave_time为当前时间，将角色重置为member，清空未读计数。
     * 数据保留不物理删除，查询时通过leave_time IS NULL过滤。
     * </p>
     *
     * @param groupId 群ID
     * @param userId  用户ID
     * @return 影响行数
     */
    @Update("UPDATE chat_group_member SET leave_time = NOW(), role = 'member', unread_count = 0 " +
            "WHERE group_id = #{groupId} AND user_id = #{userId}")
    int markAsLeft(@Param("groupId") String groupId, @Param("userId") Integer userId);

    /**
     * 物理删除群成员记录
     * <p>
     * 直接从表中删除记录。通常不直接调用，优先使用markAsLeft做软删除。
     * </p>
     *
     * @param groupId 群ID
     * @param userId  用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM chat_group_member WHERE group_id = #{groupId} AND user_id = #{userId}")
    int delete(@Param("groupId") String groupId, @Param("userId") Integer userId);

    /**
     * 统计群成员数量
     * <p>
     * 仅统计未离开的成员。用于验证群成员是否达到上限。
     * </p>
     *
     * @param groupId 群ID
     * @return 当前成员数量
     */
    @Select("SELECT COUNT(*) FROM chat_group_member WHERE group_id = #{groupId} AND leave_time IS NULL")
    int countMembers(@Param("groupId") String groupId);

    /**
     * 获取群的管理员ID列表
     * <p>
     * 返回群主(owner)和所有管理员(admin)的user_id。
     * 用于消息推送、权限判断等需要通知管理员的场景。
     * </p>
     *
     * @param groupId 群ID
     * @return 管理员用户ID列表
     */
    @Select("SELECT user_id FROM chat_group_member WHERE group_id = #{groupId} AND " +
            "role IN ('owner', 'admin') AND leave_time IS NULL")
    List<Integer> selectAdminIds(@Param("groupId") String groupId);

    /**
     * 统计群中某角色的成员数量
     * <p>
     * 仅统计未离开的成员。用于管理员数量上限检查等场景。
     * </p>
     *
     * @param groupId 群ID
     * @param role    角色（owner/admin/member）
     * @return 该角色的成员数量
     */
    @Select("SELECT COUNT(*) FROM chat_group_member WHERE group_id = #{groupId} AND role = #{role} AND leave_time IS NULL")
    int countByRole(@Param("groupId") String groupId, @Param("role") String role);
}
