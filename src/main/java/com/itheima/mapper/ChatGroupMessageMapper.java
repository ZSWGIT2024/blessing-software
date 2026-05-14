package com.itheima.mapper;

import com.itheima.dto.GroupMessageDTO;
import com.itheima.pojo.ChatGroupMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 群消息 Mapper 接口
 * <p>
 * 负责 chat_group_message 表的数据库操作。实现群消息的发送、
 * 历史查询、撤回、删除（软删除）、状态更新等功能。
 * 支持按消息ID精确查询、按群ID分页查询历史、按发送者统计
 * 时间段内消息数等场景。查询时自动过滤is_deleted=1的消息。
 * </p>
 *
 * @author idealU
 * @since 1.0
 */
@Mapper
public interface ChatGroupMessageMapper {

    /**
     * 插入群消息记录
     * <p>
     * 包含消息ID、群ID、发送者、内容类型、过滤前后内容、
     * 文件ID、扩展数据、状态、敏感词过滤信息、@全体标记等。
     * create_time和update_time由数据库NOW()自动设置。
     * </p>
     *
     * @param message 群消息实体
     * @return 影响行数
     */
    @Insert("INSERT INTO chat_group_message (message_id, group_id, sender_id, content_type, content, " +
            "original_content, file_id, extra_data, status, is_filtered, filtered_words, mention_all, " +
            "create_time, update_time) VALUES (#{messageId}, #{groupId}, #{senderId}, #{contentType}, " +
            "#{content}, #{originalContent}, #{fileId}, #{extraData}, #{status}, #{isFiltered}, " +
            "#{filteredWords}, #{mentionAll}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatGroupMessage message);

    /**
     * 根据消息ID查询消息
     * <p>
     * 仅返回未删除的消息（is_deleted = 0）。
     * 用于撤回、删除前的消息存在性检查。
     * </p>
     *
     * @param messageId 消息唯一ID
     * @return 消息实体，不存在或已删除时返回null
     */
    @Select("SELECT * FROM chat_group_message WHERE message_id = #{messageId} AND is_deleted = 0")
    ChatGroupMessage selectByMessageId(@Param("messageId") String messageId);

    /**
     * 查询群聊天历史记录（游标分页）
     * <p>
     * 通过XML Mapper实现（ChatGroupMessageMapper.xml）。
     * 基于lastMessageId的游标分页，比offset分页更高效。
     * 仅返回未删除的消息，按时间倒序排列。
     * </p>
     *
     * @param groupId       群ID
     * @param lastMessageId 游标（上一页最后一条消息ID），首次传null
     * @param pageSize      每页条数
     * @return 消息DTO列表（含发送者信息）
     */
    List<GroupMessageDTO> selectHistory(@Param("groupId") String groupId,
                                        @Param("lastMessageId") String lastMessageId,
                                        @Param("pageSize") int pageSize);

    /**
     * 更新消息状态
     * <p>
     * 用于更改消息状态标识（如sent -> withdrawn）。
     * </p>
     *
     * @param messageId 消息ID
     * @param status    新状态值
     * @return 影响行数
     */
    @Update("UPDATE chat_group_message SET status = #{status}, update_time = NOW() WHERE message_id = #{messageId}")
    int updateStatus(@Param("messageId") String messageId, @Param("status") String status);

    /**
     * 软删除消息
     * <p>
     * 将is_deleted标记为1，消息在查询时被过滤但数据保留。
     * </p>
     *
     * @param messageId 消息ID
     * @return 影响行数
     */
    @Update("UPDATE chat_group_message SET is_deleted = 1, update_time = NOW() WHERE message_id = #{messageId}")
    int markAsDeleted(@Param("messageId") String messageId);

    /**
     * 撤回消息
     * <p>
     * 仅发送者本人可撤回。撤回后status变为"withdrawn"，
     * content替换为"【消息已撤回】"，同时记录撤回时间和原因。
     * </p>
     *
     * @param messageId 消息ID
     * @param senderId  发送者ID（用于权限校验）
     * @param reason    撤回原因（可选）
     * @return 影响行数
     */
    @Update("UPDATE chat_group_message SET status = 'withdrawn', content = '【消息已撤回】', " +
            "withdraw_time = NOW(), withdraw_reason = #{reason}, update_time = NOW() " +
            "WHERE message_id = #{messageId} AND sender_id = #{senderId}")
    int withdrawMessage(@Param("messageId") String messageId, @Param("senderId") Integer senderId,
                        @Param("reason") String reason);

    /**
     * 获取群的最后一条消息
     * <p>
     * 查询群内最新的未删除消息。用于群列表页显示最后消息摘要。
     * </p>
     *
     * @param groupId 群ID
     * @return 最后一条消息实体，群内无消息时返回null
     */
    @Select("SELECT * FROM chat_group_message WHERE group_id = #{groupId} AND is_deleted = 0 " +
            "ORDER BY create_time DESC LIMIT 1")
    ChatGroupMessage selectLastMessage(@Param("groupId") String groupId);

    /**
     * 统计某时间段内某用户在群中发送的消息数
     * <p>
     * 用于频率限制判断。统计自since时间以来该用户在某群
     * 发送的未删除消息总数。
     * </p>
     *
     * @param groupId  群ID
     * @param senderId 发送者ID
     * @param since    统计起始时间
     * @return 消息数量
     */
    @Select("SELECT COUNT(*) FROM chat_group_message WHERE group_id = #{groupId} AND " +
            "sender_id = #{senderId} AND create_time >= #{since} AND is_deleted = 0")
    int countMessagesSince(@Param("groupId") String groupId, @Param("senderId") Integer senderId,
                           @Param("since") java.time.LocalDateTime since);
}
