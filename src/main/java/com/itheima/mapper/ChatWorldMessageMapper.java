package com.itheima.mapper;

import com.itheima.dto.WorldMessageDTO;
import com.itheima.pojo.ChatWorldMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatWorldMessageMapper {

    @Insert("INSERT INTO chat_world_message (message_id, sender_id, content_type, content, " +
            "original_content, file_url, extra_data, status, is_deleted, is_filtered, filtered_words, " +
            "create_time, update_time) VALUES (#{messageId}, #{senderId}, #{contentType}, " +
            "#{content}, #{originalContent}, #{fileUrl}, #{extraData}, #{status}, #{isDeleted}, " +
            "#{isFiltered}, #{filteredWords}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatWorldMessage message);

    @Select("SELECT * FROM chat_world_message WHERE message_id = #{messageId}")
    ChatWorldMessage selectByMessageId(@Param("messageId") String messageId);

    @Select("SELECT * FROM chat_world_message WHERE create_time > #{since} AND is_deleted = 0 ORDER BY create_time ASC")
    List<ChatWorldMessage> selectSince(@Param("since") java.time.LocalDateTime since);

    @Select("SELECT * FROM chat_world_message WHERE is_deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<ChatWorldMessage> selectRecent(@Param("limit") int limit);

    @Update("UPDATE chat_world_message SET status = 'withdrawn', content = '【消息已撤回】', " +
            "withdraw_time = NOW(), withdraw_reason = #{reason}, update_time = NOW() " +
            "WHERE message_id = #{messageId}")
    int withdrawMessage(@Param("messageId") String messageId, @Param("reason") String reason);

    @Update("UPDATE chat_world_message SET is_deleted = 1, update_time = NOW() WHERE message_id = #{messageId}")
    int softDelete(@Param("messageId") String messageId);

    @Delete("DELETE FROM chat_world_message WHERE create_time < #{beforeTime}")
    int deleteOlderThan(@Param("beforeTime") java.time.LocalDateTime beforeTime);

    @Select("SELECT COUNT(*) FROM chat_world_message")
    int count();

    @Delete("DELETE FROM chat_world_message WHERE id NOT IN " +
            "(SELECT id FROM (SELECT id FROM chat_world_message ORDER BY create_time DESC LIMIT #{keepCount}) t)")
    int deleteExcess(@Param("keepCount") int keepCount);

    List<WorldMessageDTO> selectRecentWithUser(@Param("limit") int limit);
}
