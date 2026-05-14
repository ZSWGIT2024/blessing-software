package com.itheima.mapper;

import com.itheima.pojo.ChatGroupEvent;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 群事件日志 Mapper 接口
 * <p>
 * 负责 chat_group_event 表的数据库操作。记录群内所有操作事件，
 * 包括群创建/解散、成员加入/退出/踢出、角色变更、禁言/解禁等。
 * 事件日志用于群管理审计和前端事件流展示。
 * 支持按时间清理过期事件以控制数据量。
 * </p>
 *
 * @author idealU
 * @since 1.0
 */
@Mapper
public interface ChatGroupEventMapper {

    /**
     * 插入群事件记录
     * <p>
     * create_time由数据库NOW()自动设置。包含事件类型、操作者、
     * 目标用户、变更前后的值等信息。使用自增主键回填id。
     * </p>
     *
     * @param event 事件实体（需包含groupId、eventType、operatorId等）
     * @return 影响行数
     */
    @Insert("INSERT INTO chat_group_event (group_id, event_type, operator_id, target_user_id, " +
            "old_value, new_value, create_time) VALUES (#{groupId}, #{eventType}, #{operatorId}, " +
            "#{targetUserId}, #{oldValue}, #{newValue}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatGroupEvent event);

    /**
     * 查询群的所有事件日志
     * <p>
     * 按创建时间倒序排列，最新的日志排在前面。
     * 用于前端展示群动态时间线。
     * </p>
     *
     * @param groupId 群ID
     * @return 事件列表（按时间倒序）
     */
    @Select("SELECT * FROM chat_group_event WHERE group_id = #{groupId} ORDER BY create_time DESC")
    List<ChatGroupEvent> selectByGroup(@Param("groupId") String groupId);

    /**
     * 删除指定时间之前的旧事件
     * <p>
     * 用于定时任务清理过期的事件日志，控制表数据量。
     * </p>
     *
     * @param beforeTime 删除此时间之前创建的事件
     * @return 删除的影响行数
     */
    @Delete("DELETE FROM chat_group_event WHERE create_time < #{beforeTime}")
    int deleteOldEvents(@Param("beforeTime") java.time.LocalDateTime beforeTime);
}
