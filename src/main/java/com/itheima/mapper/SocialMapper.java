package com.itheima.mapper;

import com.itheima.dto.*;
import com.itheima.pojo.*;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SocialMapper {

    // ==================== 好友申请相关 ====================
    @Insert("INSERT INTO friend_apply (applicant_id, receiver_id, apply_msg, status, expire_time) " +
            "VALUES (#{applicantId}, #{receiverId}, #{applyMsg}, #{status}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFriendApply(FriendApply apply);

    @Select("SELECT * FROM friend_apply WHERE id = #{id}")
    FriendApply selectFriendApplyById(@Param("id") Long id);

    //获取所有收到的好友申请
    @Select("SELECT fa.*, u.username as applicant_username, u.avatar as applicant_avatar " +
            "FROM friend_apply fa " +
            "JOIN user u ON fa.applicant_id = u.id " +
            "WHERE fa.receiver_id = #{receiverId}")
    List<FriendApplyDTO> selectPendingApplies(@Param("receiverId") Integer receiverId);

    //获取所有我发出的好友申请请求
    @Select("SELECT fa.*, u.username as receiver_username, u.avatar as receiver_avatar " +
            "FROM friend_apply fa " +
            "JOIN user u ON fa.receiver_id = u.id " +
            "WHERE fa.applicant_id = #{applicantId}")
    List<FriendApplyDTO> selectMyAllApplies(@Param("applicantId") Integer applicantId);

    @Update("UPDATE friend_apply SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateFriendApplyStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM friend_apply WHERE id = #{id}")
    int deleteFriendApply(Long id);

    @Select("SELECT COUNT(*) FROM friend_apply " +
            "WHERE applicant_id = #{applicantId} AND receiver_id = #{receiverId} AND status = 'pending'")
    int checkPendingApplyExists(@Param("applicantId") Integer applicantId,
                                @Param("receiverId") Integer receiverId);

    // ==================== 好友关系相关 ====================
    @Select("SELECT COUNT(*) FROM friend_relation WHERE user_id = #{userId}")
    int countFriends(Integer userId);

    @Select("SELECT fr.*, u.username as friend_username, u.avatar as friend_avatar " +
            "FROM friend_relation fr " +
            "JOIN user u ON fr.friend_id = u.id " +
            "WHERE fr.user_id = #{userId} AND relation_type = 'friend' AND is_blocked = 0 ORDER BY fr.create_time DESC")
    List<FriendRelationDTO> selectFriendsByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM friend_relation WHERE user_id = #{userId} AND friend_id = #{friendId}")
    FriendRelation selectFriendRelation(@Param("userId") Integer userId,
                                        @Param("friendId") Integer friendId);

    @Select("SELECT COUNT(*) FROM friend_relation " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int checkFriendRelationExists(@Param("userId") Integer userId,
                                  @Param("friendId") Integer friendId);

    @Insert("INSERT INTO friend_relation (user_id, friend_id, group_name, remark, relation_type, become_friend_time) " +
            "VALUES (#{userId}, #{friendId}, #{groupName}, #{remark}, #{relationType}, NOW())")
    int insertFriendRelation(FriendRelation relation);


    @Update("<script>" +
            "UPDATE friend_relation " +
            "<set>" +
            "   <if test='groupName != null'>group_name = #{groupName},</if>" +
            "   <if test='relationType != null'>relation_type = #{relationType},</if>" +
            "   update_time = NOW()" +
            "</set>" +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}" +
            "</script>")
    int updateFriendRelation(FriendRelation relation);

    @Update("UPDATE friend_relation SET remark = #{remark}, update_time = NOW() " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int updateFriendRemark(@Param("userId") Integer userId,@Param("friendId") Integer friendId,@Param("remark") String remark);

    @Delete("DELETE FROM friend_relation WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int deleteFriendRelation(@Param("userId") Integer userId,
                             @Param("friendId") Integer friendId);

    @Update("UPDATE friend_relation SET is_blocked = #{isBlocked} " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int updateBlockStatus(@Param("userId") Integer userId,
                          @Param("friendId") Integer friendId,
                          @Param("isBlocked") Boolean isBlocked);

    @Update("UPDATE friend_relation SET last_interaction_time = NOW(), " +
            "interaction_count = interaction_count + 1 " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int updateLastInteractionTime(@Param("userId") Integer userId,
                                  @Param("friendId") Integer friendId);

    @Update("UPDATE friend_relation SET is_starred = #{isStarred} " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int updateStarStatus(@Param("userId") Integer userId,
                         @Param("friendId") Integer friendId,
                         @Param("isStarred") Boolean isStarred);

    // ==================== 更新用户统计信息 ====================

    @Update("UPDATE user SET follow_count = follow_count + 1 WHERE id = #{userId}")
    int incrementFollowCount(@Param("userId") Integer userId);

    @Update("UPDATE user SET follow_count = follow_count - 1 WHERE id = #{userId} AND follow_count > 0")
    int decrementFollowCount(@Param("userId") Integer userId);

    @Update("UPDATE user SET follower_count = follower_count + 1 WHERE id = #{userId}")
    int incrementFollowerCount(@Param("userId") Integer userId);

    @Update("UPDATE user SET follower_count = follower_count - 1 WHERE id = #{userId} AND follower_count > 0")
    int decrementFollowerCount(@Param("userId") Integer userId);

    // ==================== 关注关系相关 ====================
    @Insert("INSERT INTO user_follow (follower_id, following_id, relation_type, remark) " +
            "VALUES (#{followerId}, #{followingId}, #{relationType}, #{remark})")
    int insertUserFollow(FollowDTO follow);

    @Select("SELECT COUNT(*) FROM user_follow WHERE follower_id = #{followerId}")
    int countFollowing(@Param("followerId") Integer followerId);

    @Select("SELECT uf.*, u.username as following_username, u.avatar as following_avatar " +
            "FROM user_follow uf " +
            "JOIN user u ON uf.following_id = u.id " +
            "WHERE uf.follower_id = #{followerId}")
    List<FollowRelationDTO> selectFollowingByUserId(@Param("followerId") Integer followerId);

    @Select("SELECT uf.*, u.username as follower_username, u.avatar as follower_avatar " +
            "FROM user_follow uf " +
            "JOIN user u ON uf.follower_id = u.id " +
            "WHERE uf.following_id = #{followingId}")
    List<FollowRelationDTO> selectFollowersByUserId(@Param("followingId") Integer followingId);

    @Select("SELECT COUNT(*) FROM user_follow " +
            "WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int checkFollowRelationExists(@Param("followerId") Integer followerId,
                                  @Param("followingId") Integer followingId);

    @Update("UPDATE user_follow SET remark = #{remark} " +
            "WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int updateFollowRemark(@Param("followerId") Integer followerId,
                           @Param("followingId") Integer followingId,
                           @Param("remark") String remark);

    @Delete("DELETE FROM user_follow WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int deleteUserFollow(@Param("followerId") Integer followerId,
                         @Param("followingId") Integer followingId);

    // ==================== 消息通知相关 ====================
    @Insert("INSERT INTO notification (user_id, type, title, content, data, " +
            "related_id, related_type, related_user_id, is_read) " +
            "VALUES (#{userId}, #{type}, #{title}, #{content}, #{data}, " +
            "#{relatedId}, #{relatedType}, #{relatedUserId}, #{isRead})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotification(Notification notification);

    @Select("SELECT * FROM notification WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<Notification> selectNotificationsByUserIdWithPagination(
            @Param("userId") Integer userId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);


    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadNotifications(Integer userId);

    @Update("UPDATE notification SET is_read = 1, read_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int markNotificationAsRead(@Param("id") Integer id, @Param("userId") Integer userId);

    @Update("UPDATE notification SET is_read = 1, read_time = NOW() " +
            "WHERE user_id = #{userId} AND related_user_id = #{relatedUserId} AND type = 'chat'")
    int markChatNotificationsAsRead(@Param("userId") Integer userId,
                                    @Param("relatedUserId") Integer relatedUserId);


    @Update("UPDATE notification SET is_read = 1, read_time = NOW() " +
            "WHERE user_id = #{userId} AND is_read = 0")
    int markAllNotificationsAsRead(Integer userId);


    // ==================== 用户搜索 ====================
    @Select("SELECT id, nickname, avatar, bio, level, vip_type, " +
            "follow_count, follower_count " +
            "FROM user WHERE nickname LIKE CONCAT('%', #{keyword}, '%') " +
            "AND status = 'active' AND id != #{excludeId} " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<UserSimpleDTO> searchUsers(@Param("keyword") String keyword,
                                    @Param("excludeId") Integer excludeId,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM user WHERE nickname LIKE CONCAT('%', #{keyword}, '%') " +
            "AND status = 'active' AND id != #{excludeId}")
    int countUsers(@Param("keyword") String keyword,
                   @Param("excludeId") Integer excludeId);


    // 在SocialMapper中添加

    @Select("SELECT * FROM notification WHERE data LIKE CONCAT('%', #{messageId}, '%') AND type = 'chat' LIMIT 1")
    Notification selectNotificationByMessageId(String messageId);

    @Update("UPDATE notification SET content = #{content}, data = #{data} WHERE id = #{id}")
    int updateNotification(Notification notification);

    // 在SocialMapper中添加

    @Insert("INSERT INTO chat_file (uploader_id, receiver_id, file_name, file_path, " +
            "file_type, file_size, thumbnail_path, message_id) " +
            "VALUES (#{uploaderId}, #{receiverId}, #{fileName}, #{filePath}, " +
            "#{fileType}, #{fileSize}, #{thumbnailPath}, #{messageId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertChatFile(ChatFile chatFile);

    @Select("SELECT cf.*, u.nickname as uploader_nickname, u.avatar as uploader_avatar " +
            "FROM chat_file cf " +
            "JOIN user u ON cf.uploader_id = u.id " +
            "WHERE cf.uploader_id = #{userId} " +
            "AND (#{fileType} IS NULL OR cf.file_type = #{fileType}) " +
            "ORDER BY cf.create_time DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<ChatFileDTO> selectChatFiles(@Param("userId") Integer userId,
                                      @Param("fileType") String fileType,
                                      @Param("offset") Integer offset,
                                      @Param("pageSize") Integer pageSize);

    @Select("SELECT * FROM chat_file WHERE id = #{id}")
    ChatFile selectChatFileById(@Param("id") Long id);

    @Delete("DELETE FROM chat_file WHERE id = #{id}")
    int deleteChatFile(@Param("id") Long id);


    // 在 SocialMapper.java 中添加以下聊天消息相关方法

    /**
     * 插入聊天消息
     */
    int insertChatMessage(ChatMessage chatMessage);

    /**
     * 根据消息ID查询消息
     */
    ChatMessage selectChatMessageByMessageId(@Param("messageId") String messageId);

    /**
     * 获取最近聊天列表（从chat_message表）
     */
    @Select("SELECT DISTINCT " +
            "CASE " +
            "  WHEN cm.sender_id = #{userId} THEN cm.receiver_id " +
            "  WHEN cm.receiver_id = #{userId} THEN cm.sender_id " +
            "END as related_user_id, " +
            "MAX(cm.create_time) as last_time " +
            "FROM chat_message cm " +
            "WHERE (cm.sender_id = #{userId} OR cm.receiver_id = #{userId}) " +
            "AND cm.is_deleted = 0 " +
            "GROUP BY CASE " +
            "  WHEN cm.sender_id = #{userId} THEN cm.receiver_id " +
            "  WHEN cm.receiver_id = #{userId} THEN cm.sender_id " +
            "END " +
            "ORDER BY last_time DESC " +
            "LIMIT #{limit}")
    List<RecentChatDTO> selectRecentChatsFromChatMessage(@Param("userId") Integer userId,
                                                         @Param("limit") Integer limit);

    /**
     * 获取与指定用户的聊天历史（从chat_message表）
     */
    @Select("SELECT cm.*, " +
            "u1.nickname as sender_nickname, u1.avatar as sender_avatar, " +
            "u2.nickname as receiver_nickname, u2.avatar as receiver_avatar, " +
            "cf.file_name, cf.file_path, cf.file_type, cf.file_size, cf.thumbnail_path " +
            "FROM chat_message cm " +
            "LEFT JOIN user u1 ON cm.sender_id = u1.id " +
            "LEFT JOIN user u2 ON cm.receiver_id = u2.id " +
            "LEFT JOIN chat_file cf ON cm.file_id = cf.id " +
            "WHERE ((cm.sender_id = #{userId} AND cm.receiver_id = #{relatedUserId}) " +
            "   OR (cm.sender_id = #{relatedUserId} AND cm.receiver_id = #{userId})) " +
            "AND cm.is_deleted = 0 " + // 只过滤掉双方都已删除的消息
            "AND NOT EXISTS ( " + // 过滤掉当前用户单方删除的消息
            "   SELECT 1 FROM chat_message_status_history cmsh " +
            "   WHERE cmsh.message_id = cm.message_id " +
            "   AND cmsh.status = CONCAT('deleted_by_', #{userId})" +
            ") " +
            "ORDER BY cm.create_time")
    List<Map<String, Object>> selectChatHistoryFromChatMessage(@Param("userId") Integer userId,
                                                               @Param("relatedUserId") Integer relatedUserId);
    /**
     * 更新消息状态
     */
    int updateMessageStatus(@Param("messageId") String messageId,
                            @Param("status") String status);

    /**
     * 逻辑删除该消息(更新is_delete字段为1)
     */
    @Update("UPDATE chat_message SET is_deleted = 1, update_time = NOW() " +
            "WHERE message_id = #{messageId}")
    void updateMessageToDeleted(String messageId);

    /**
     * 标记消息为已读
     */
    int markMessagesAsRead(@Param("userId") Integer userId,
                           @Param("senderId") Integer senderId);

    /**
     * 撤回消息
     */
    int withdrawMessage(@Param("messageId") String messageId,
                        @Param("senderId") Integer senderId,
                        @Param("reason") String reason);

    /**
     * 获取未读消息数量
     */
    int selectUnreadCount(@Param("userId") Integer userId);

    /**
     * 获取与指定用户的未读消息数量
     */
    int selectUnreadCountWithUser(@Param("userId") Integer userId,
                                  @Param("senderId") Integer senderId);

    /**
     * 删除聊天记录（逻辑删除）
     */
    int deleteHistoryFromChat(@Param("userId") Integer userId,
                              @Param("friendId") Integer friendId);

    /**
     * 删除与聊天记录关联的文件记录（可选）
     */
    int deleteHistoryFromChatFile(@Param("userId") Integer userId,
                                  @Param("friendId") Integer friendId);


    // 查询消息状态历史
    @Select("SELECT COUNT(*) FROM chat_message_status_history " +
            "WHERE message_id = #{messageId} AND status = #{status}")
    int checkMessageStatusHistory(@Param("messageId") String messageId,
                                      @Param("status") String status);

    // 统计删除状态数量
    @Select("SELECT COUNT(DISTINCT operator_id) FROM chat_message_status_history " +
            "WHERE message_id = #{messageId} AND status LIKE 'deleted_by_%'")
    int countMessageDeleteStatus(@Param("messageId") String messageId);

    /**
     * 插入消息状态历史记录
     */
    int insertMessageStatusHistory(ChatMessageStatusHistory history);

    /**
     * 获取最近一条消息内容
     */
    @Select("SELECT content, content_type FROM chat_message " +
            "WHERE ((sender_id = #{userId} AND receiver_id = #{otherUserId}) " +
            "   OR (sender_id = #{otherUserId} AND receiver_id = #{userId})) " +
            "AND is_deleted = 0 " +
            "ORDER BY create_time DESC " +
            "LIMIT 1")
    Map<String, Object> selectLastMessageContent(@Param("userId") Integer userId,
                                                 @Param("otherUserId") Integer otherUserId);

    @Update("UPDATE chat_message" +
            " SET content = #{content} " +
            " status = #{status} WHERE id = #{id}")
    void updateChatMessage(ChatMessage chatMessage);

    @Select("SELECT * FROM friend_relation WHERE user_id = #{userId} AND is_blocked = 1")
    List<FriendRelationDTO> selectBlacklistByUserId(Integer userId);

    @Update("UPDATE chat_message_status_history" +
            " SET reason = #{reason}, status = #{status} " +
            "WHERE message_id = #{messageId}")
    void updateMessageStatusHistory(
            @Param("messageId") String messageId,
            @Param("status") String status,
            @Param("reason") String reason
    );

}