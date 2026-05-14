package com.itheima.mapper;

import com.itheima.dto.GroupListDTO;
import com.itheima.pojo.ChatGroup;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 群聊基本信息 Mapper 接口
 * <p>
 * 负责 chat_group 表的数据库操作。提供群的增删改查功能，
 * 包括按群ID查询、按群主查询、关键词搜索、成员计数更新、
 * 解散、全员禁言、邀请码管理等功能。查询时自动过滤已解散的群。
 * </p>
 *
 * @author idealU
 * @since 1.0
 */
@Mapper
public interface ChatGroupMapper {

    /**
     * 插入新群记录
     * <p>
     * 插入群基本信息，create_time和update_time由数据库自动设置。
     * 使用自增主键回填到group对象的id字段。
     * </p>
     *
     * @param group 群实体（需包含groupId、name、ownerId等必要字段）
     * @return 影响行数
     */
    @Insert("INSERT INTO chat_group (group_id, name, avatar, description, owner_id, max_members, " +
            "current_members, join_permission, is_muted_all, invite_code, invite_expire_time, create_time, update_time) " +
            "VALUES (#{groupId}, #{name}, #{avatar}, #{description}, #{ownerId}, #{maxMembers}, " +
            "#{currentMembers}, #{joinPermission}, #{isMutedAll}, #{inviteCode}, #{inviteExpireTime}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGroup(ChatGroup group);

    /**
     * 根据群ID查询群信息
     * <p>
     * 仅返回未解散的群（is_dissolved = 0）。
     * </p>
     *
     * @param groupId 群ID（16位字符串）
     * @return 群实体，不存在或已解散时返回null
     */
    @Select("SELECT * FROM chat_group WHERE group_id = #{groupId} AND is_dissolved = 0")
    ChatGroup selectByGroupId(@Param("groupId") String groupId);

    /**
     * 根据自增主键ID查询群
     * <p>
     * 不区分是否解散，直接按主键查询。
     * </p>
     *
     * @param id 自增主键ID
     * @return 群实体，不存在时返回null
     */
    @Select("SELECT * FROM chat_group WHERE id = #{id}")
    ChatGroup selectById(@Param("id") Long id);

    /**
     * 查询某用户创建的所有群
     * <p>
     * 返回指定用户作为群主的所有未解散群，按创建时间倒序排列。
     * </p>
     *
     * @param ownerId 群主用户ID
     * @return 群列表
     */
    @Select("SELECT * FROM chat_group WHERE owner_id = #{ownerId} AND is_dissolved = 0 ORDER BY create_time DESC")
    List<ChatGroup> selectByOwner(@Param("ownerId") Integer ownerId);

    /**
     * 按关键词搜索群
     * <p>
     * 对群名称进行模糊匹配（LIKE %keyword%），仅返回未解散的群。
     * 结果按成员数量降序、创建时间降序排列（优先展示热门群）。
     * </p>
     *
     * @param keyword 搜索关键词
     * @return 匹配的群列表
     */
    @Select("SELECT * FROM chat_group WHERE (name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%') OR join_permission LIKE CONCAT('%', #{keyword}, '%'))" +
            " AND is_dissolved = 0 " +
            "ORDER BY current_members DESC, create_time DESC")
    List<ChatGroup> searchGroups(@Param("keyword") String keyword);

    /**
     * 更新群信息
     * <p>
     * 更新群的名称、头像、描述、最大成员数、加入权限。
     * 自动更新update_time时间戳。
     * </p>
     *
     * @param group 包含更新字段的群实体（需包含groupId）
     * @return 影响行数
     */
    @Update("UPDATE chat_group SET name = #{name}, avatar = #{avatar}, description = #{description}, " +
            "owner_id = #{ownerId}, max_members = #{maxMembers}, join_permission = #{joinPermission}, " +
            "is_muted_all = #{isMutedAll}, update_time = NOW() " +
            "WHERE group_id = #{groupId}")
    int updateGroup(ChatGroup group);

    /**
     * 更新群成员计数
     * <p>
     * 对current_members字段做增量更新（+1或-1）。
     * 加入成员时delta=1，退出/踢出时delta=-1。
     * </p>
     *
     * @param groupId 群ID
     * @param delta   增量值（正数增加，负数减少）
     * @return 影响行数
     */
    @Update("UPDATE chat_group SET current_members = current_members + #{delta}, update_time = NOW() " +
            "WHERE group_id = #{groupId}")
    int updateMemberCount(@Param("groupId") String groupId, @Param("delta") int delta);

    /**
     * 解散群
     * <p>
     * 将is_dissolved标记为1，群在查询时会被过滤掉。
     * 数据不物理删除，仅逻辑标记。
     * </p>
     *
     * @param groupId 群ID
     * @return 影响行数
     */
    @Update("UPDATE chat_group SET is_dissolved = 1, update_time = NOW() WHERE group_id = #{groupId}")
    int dissolveGroup(@Param("groupId") String groupId);

    /**
     * 设置全员禁言
     * <p>
     * 更新is_muted_all字段。true开启全员禁言（仅群主/admin可发言），
     * false关闭。
     * </p>
     *
     * @param groupId    群ID
     * @param isMutedAll true开启全员禁言，false关闭
     * @return 影响行数
     */
    @Update("UPDATE chat_group SET is_muted_all = #{isMutedAll}, update_time = NOW() WHERE group_id = #{groupId}")
    int setMuteAll(@Param("groupId") String groupId, @Param("isMutedAll") Boolean isMutedAll);

    /**
     * 更新群邀请码
     * <p>
     * 设置或刷新群的邀请码和过期时间。其他用户可通过邀请码加入该群。
     * </p>
     *
     * @param groupId    群ID
     * @param inviteCode 8位邀请码
     * @param expireTime 邀请码过期时间
     * @return 影响行数
     */
    @Update("UPDATE chat_group SET invite_code = #{inviteCode}, invite_expire_time = #{expireTime}, " +
            "update_time = NOW() WHERE group_id = #{groupId}")
    int updateInviteCode(@Param("groupId") String groupId, @Param("inviteCode") String inviteCode,
                         @Param("expireTime") java.time.LocalDateTime expireTime);

    /**
     * 根据邀请码查询群
     * <p>
     * 要求群未解散且邀请码未过期（或过期时间为null）。
     * 用于用户通过邀请码加入群的场景。
     * </p>
     *
     * @param inviteCode 邀请码
     * @return 群实体，邀请码无效或已过期时返回null
     */
    @Select("SELECT * FROM chat_group WHERE invite_code = #{inviteCode} AND is_dissolved = 0 " +
            "AND (invite_expire_time IS NULL OR invite_expire_time > NOW())")
    ChatGroup selectByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 统计用户拥有的群数量（作为群主）
     * <p>
     * 仅统计未解散的群。用于创建群时检查是否达到上限。
     * </p>
     *
     * @param userId 用户ID
     * @return 拥有的群数
     */
    @Select("SELECT COUNT(*) FROM chat_group WHERE owner_id = #{userId} AND is_dissolved = 0")
    int countOwnedGroups(@Param("userId") Integer userId);

    /**
     * 统计用户加入的群数量
     * <p>
     * 统计chat_group_member表中用户未离开的群记录数。
     * 用于检查加入群数量上限。
     * </p>
     *
     * @param userId 用户ID
     * @return 加入的群数
     */
    @Select("SELECT COUNT(*) FROM chat_group_member WHERE user_id = #{userId} AND leave_time IS NULL")
    int countJoinedGroups(@Param("userId") Integer userId);

    /**
     * 根据群名称精确查找未解散的群
     * <p>
     * 用于创建/编辑群时校验群名是否重复。
     * </p>
     *
     * @param name 群名称
     * @return 群实体，不存在时返回null
     */
    @Select("SELECT * FROM chat_group WHERE name = #{name} AND is_dissolved = 0 LIMIT 1")
    ChatGroup selectByName(@Param("name") String name);

    /**
     * 获取用户的所有群列表（含最后一条消息摘要）
     * <p>
     * 通过XML Mapper实现（com/itheima/mapper/ChatGroupMapper.xml），
     * 联表查询群信息和最后消息内容，用于群列表页面展示。
     * </p>
     *
     * @param userId 用户ID
     * @return 群列表DTO（含群基本信息+最后消息）
     */
    List<GroupListDTO> selectMyGroups(@Param("userId") Integer userId);
}
