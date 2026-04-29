package com.itheima.mapper;

import com.itheima.pojo.Notification;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO notification (user_id, type, title, content, data, related_id, related_type, related_user_id) " +
            "VALUES (#{userId}, #{type}, #{title}, #{content}, #{data}, #{relatedId}, #{relatedType}, #{relatedUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    // 批量插入通知
    @Insert("<script>" +
            "INSERT INTO notification (user_id, type, title, content, data, related_id, related_type, related_user_id) " +
            "VALUES " +
            "<foreach collection='notifications' item='notification' separator=','>" +
            "(#{notification.userId}, #{notification.type}, #{notification.title}, #{notification.content}, " +
            "#{notification.data}, #{notification.relatedId}, #{notification.relatedType}, #{notification.relatedUserId})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("notifications") List<Notification> notifications);

    @Select("SELECT * FROM notification WHERE id = #{id}")
    Notification selectById(@Param("id") Long id);

    @Update("UPDATE notification SET is_read = 1, read_time = NOW() WHERE id = #{id}")
    int markAsRead(@Param("id") Long id);

    @Update("UPDATE notification SET is_read = 1, read_time = NOW() WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Integer userId);

    @Update("UPDATE notification SET is_deleted = 1 WHERE id = #{id}")
    int delete(@Param("id") Long id);

    @Update("UPDATE notification SET is_deleted = 1 WHERE user_id = #{userId} AND type = #{type}")
    int deleteByType(@Param("userId") Integer userId, @Param("type") String type);

    @Select("<script>" +
            "SELECT * FROM notification WHERE user_id = #{userId} AND is_deleted = 0 " +
            "<if test='type != null'>AND type = #{type}</if>" +
            "<if test='unreadOnly'>AND is_read = 0</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Notification> selectByUser(@Param("userId") Integer userId,
                                    @Param("type") String type,
                                    @Param("unreadOnly") Boolean unreadOnly,
                                    @Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0 AND is_deleted = 0")
    int countUnread(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND type = #{type} AND is_read = 0 AND is_deleted = 0")
    int countUnreadByType(@Param("userId") Integer userId, @Param("type") String type);

    // 获取最新的通知
    @Select("SELECT * FROM notification WHERE user_id = #{userId} AND is_deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<Notification> selectLatest(@Param("userId") Integer userId, @Param("limit") Integer limit);
}