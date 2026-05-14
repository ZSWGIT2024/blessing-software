package com.itheima.mapper;

import com.itheima.pojo.SystemMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 系统消息 Mapper 接口
 * <p>
 * 负责 system_message 表的数据库操作。管理系统消息的增删改查，
 * 支持按用户维度查询活跃消息（根据targetType自动匹配全站、用户、
 * 群组三种目标类型），以及过期消息的自动清理。
 * </p>
 *
 * @author idealU
 * @since 1.0
 */
@Mapper
public interface SystemMessageMapper {

    /**
     * 插入系统消息
     * <p>
     * create_time由数据库NOW()自动设置。消息默认is_active=1。
     * 支持设置过期时间expire_time，到期后由定时任务清理。
     * 使用自增主键回填id。
     * </p>
     *
     * @param message 系统消息实体
     * @return 影响行数
     */
    @Insert("INSERT INTO system_message (message_id, title, content, message_type, target_type, " +
            "target_id, sender_id, is_active, expire_time, create_time) VALUES (#{messageId}, #{title}, " +
            "#{content}, #{messageType}, #{targetType}, #{targetId}, #{senderId}, #{isActive}, " +
            "#{expireTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SystemMessage message);

    /**
     * 根据消息ID查询系统消息
     * <p>
     * 用于更新和删除前确认消息存在。不区分激活状态。
     * </p>
     *
     * @param messageId 消息ID（格式：sys_{userId}_{timestamp}）
     * @return 系统消息实体，不存在时返回null
     */
    @Select("SELECT * FROM system_message WHERE message_id = #{messageId}")
    SystemMessage selectByMessageId(@Param("messageId") String messageId);

    /**
     * 查询当前用户可见的活跃系统消息
     * <p>
     * 查询条件：
     * <ul>
     *   <li>消息激活中（is_active = 1）</li>
     *   <li>未过期（expire_time IS NULL 或 大于当前时间）</li>
     *   <li>目标范围匹配：
     *     <ul>
     *       <li>target_type = 'all'（全站）</li>
     *       <li>target_type = 'user' AND target_id = 当前用户ID</li>
     *       <li>target_type = 'group' AND 该用户在该群中</li>
     *     </ul>
     *   </li>
     * </ul>
     * 结果按创建时间倒序排列。
     * </p>
     *
     * @param userId 用户ID（以字符串形式传入）
     * @return 符合条件的系统消息列表
     */
    @Select("SELECT * FROM system_message WHERE is_active = 1 AND " +
            "(expire_time IS NULL OR expire_time > NOW()) " +
            "AND (target_type = 'all' OR (target_type = 'user' AND target_id = #{userId}) " +
            "OR (target_type = 'group' AND target_id IN " +
            "(SELECT group_id FROM chat_group_member WHERE user_id = #{userId} AND leave_time IS NULL))) " +
            "ORDER BY create_time DESC")
    List<SystemMessage> selectActiveForUser(@Param("userId") String userId);

    /**
     * 查询全部系统消息（管理后台用）
     * <p>
     * 不区分激活状态和过期，返回所有消息按创建时间倒序。
     * 用于管理后台展示完整消息列表。
     * </p>
     *
     * @return 全部系统消息列表
     */
    @Select("SELECT * FROM system_message ORDER BY create_time DESC")
    List<SystemMessage> selectAll();

    /**
     * 更新系统消息
     * <p>
     * 更新标题、内容、目标类型、目标ID和过期时间。
     * messageType和isActive不在此处更新（有专门方法）。
     * </p>
     *
     * @param message 包含更新字段的消息实体（需包含messageId）
     * @return 影响行数
     */
    @Update("UPDATE system_message SET title = #{title}, content = #{content}, " +
            "target_type = #{targetType}, target_id = #{targetId}, expire_time = #{expireTime} " +
            "WHERE message_id = #{messageId}")
    int update(SystemMessage message);

    /**
     * 更新消息的激活状态
     * <p>
     * 用于启用/停用消息。停用后消息不再对用户显示，
     * 但数据保留在数据库中便于后续审计。
     * </p>
     *
     * @param messageId 消息ID
     * @param isActive  true启用，false停用
     * @return 影响行数
     */
    @Update("UPDATE system_message SET is_active = #{isActive} WHERE message_id = #{messageId}")
    int updateActive(@Param("messageId") String messageId, @Param("isActive") Boolean isActive);

    /**
     * 物理删除系统消息
     * <p>
     * 从system_message表中永久删除该记录。注意此操作不可恢复。
     * </p>
     *
     * @param messageId 消息ID
     * @return 影响行数
     */
    @Delete("DELETE FROM system_message WHERE message_id = #{messageId}")
    int delete(@Param("messageId") String messageId);

    /**
     * 删除已过期的系统消息
     * <p>
     * 定时任务调用，清理expire_time早于当前时间的过期消息。
     * 仅删除设置过期时间且已到期的消息。
     * </p>
     *
     * @return 删除的行数
     */
    @Delete("DELETE FROM system_message WHERE expire_time IS NOT NULL AND expire_time < NOW()")
    int deleteExpired();
}
