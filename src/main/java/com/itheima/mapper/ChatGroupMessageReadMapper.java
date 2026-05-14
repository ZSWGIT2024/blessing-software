package com.itheima.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群消息已读状态 Mapper 接口
 * <p>
 * 负责 chat_group_message_read 表的数据库操作。管理群消息的已读状态，
 * 支持批量初始化和逐条标记。每条消息需要为群内每个成员创建已读记录，
 * 通过is_read字段标识是否已读，read_time记录阅读时间。
 * </p>
 *
 * @author idealU
 * @since 1.0
 */
@Mapper
public interface ChatGroupMessageReadMapper {

    /**
     * 标记单条消息为已读
     * <p>
     * 更新指定用户对指定消息的is_read=1并记录read_time。
     * 操作幂等，重复标记不影响结果。
     * </p>
     *
     * @param messageId 消息ID
     * @param userId    用户ID
     * @return 影响行数
     */
    @Update("UPDATE chat_group_message_read SET is_read = 1, read_time = NOW() " +
            "WHERE message_id = #{messageId} AND user_id = #{userId}")
    int markAsRead(@Param("messageId") String messageId, @Param("userId") Integer userId);

    /**
     * 批量标记某时间之前的所有消息为已读
     * <p>
     * 用于标记一个群内多条消息为已读（级联更新）。
     * 联表chat_group_message确定消息所属群和时间范围，
     * 标记指定创建时间之前的本群所有未读消息为已读。
     * </p>
     *
     * @param groupId    群ID
     * @param userId     用户ID
     * @param beforeTime 标记此时间之前创建的消息
     * @return 影响行数
     */
    @Update("UPDATE chat_group_message_read cgmr " +
            "INNER JOIN chat_group_message cgm ON cgmr.message_id = cgm.message_id " +
            "SET cgmr.is_read = 1, cgmr.read_time = NOW() " +
            "WHERE cgm.group_id = #{groupId} AND cgmr.user_id = #{userId} AND cgmr.is_read = 0 " +
            "AND cgm.create_time <= #{beforeTime}")
    int batchMarkAsRead(@Param("groupId") String groupId, @Param("userId") Integer userId,
                        @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 查询某条消息的已读用户ID列表
     * <p>
     * 返回所有已读该消息的用户ID。用于展示"已读x人"及其成员列表。
     * </p>
     *
     * @param messageId 消息ID
     * @return 已读用户ID列表
     */
    @Select("SELECT user_id FROM chat_group_message_read WHERE message_id = #{messageId} AND is_read = 1")
    List<Integer> selectReadUserIds(@Param("messageId") String messageId);

    /**
     * 统计某条消息的已读人数
     * <p>
     * 用于快速显示"已读x人"而不需要获取具体用户列表。
     * </p>
     *
     * @param messageId 消息ID
     * @return 已读该消息的用户数
     */
    @Select("SELECT COUNT(*) FROM chat_group_message_read WHERE message_id = #{messageId} AND is_read = 1")
    int countReadByMessage(@Param("messageId") String messageId);

    /**
     * 统计用户在某群中的未读消息数
     * <p>
     * 直接计算chat_group_message_read表中is_read=0的记录数。
     * 提供了另一种未读计数方式，与成员表的unread_count字段互补。
     * </p>
     *
     * @param groupId 群ID
     * @param userId  用户ID
     * @return 未读消息数量
     */
    @Select("SELECT COUNT(*) FROM chat_group_message_read WHERE group_id = #{groupId} " +
            "AND user_id = #{userId} AND is_read = 0")
    int countUnreadByGroup(@Param("groupId") String groupId, @Param("userId") Integer userId);
}
