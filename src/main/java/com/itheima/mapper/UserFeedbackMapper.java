package com.itheima.mapper;

import com.itheima.pojo.UserFeedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserFeedbackMapper {

    @Insert("INSERT INTO user_feedback (user_id, username, type, content, images, contact, status, create_time, update_time) " +
            "VALUES (#{userId}, #{username}, #{type}, #{content}, #{images}, #{contact}, 'pending', NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserFeedback feedback);

    @Select("SELECT * FROM user_feedback WHERE id = #{id} AND is_deleted = 0")
    UserFeedback selectById(@Param("id") Long id);

    @Select("SELECT * FROM user_feedback WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<UserFeedback> selectByUserId(@Param("userId") Integer userId);

    @Update("UPDATE user_feedback SET type = #{type}, content = #{content}, images = #{images}, " +
            "contact = #{contact}, update_time = NOW() WHERE id = #{id} AND user_id = #{userId} AND status = 'pending'")
    int updateByUser(UserFeedback feedback);

    @Update("UPDATE user_feedback SET is_deleted = 1, update_time = NOW() WHERE id = #{id} AND user_id = #{userId}")
    int softDeleteByUser(@Param("id") Long id, @Param("userId") Integer userId);

    // ===== Admin =====

    @Select("<script>" +
            "SELECT * FROM user_feedback WHERE is_deleted = 0 " +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if> " +
            "ORDER BY FIELD(status, 'pending','processing','resolved','closed'), create_time DESC " +
            "LIMIT #{offset}, #{limit}</script>")
    List<UserFeedback> selectPage(@Param("offset") int offset, @Param("limit") int limit,
                                  @Param("status") String status);

    @Select("<script>SELECT COUNT(*) FROM user_feedback WHERE is_deleted = 0 " +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if></script>")
    Long count(@Param("status") String status);

    @Update("UPDATE user_feedback SET status = #{status}, admin_reply = #{adminReply}, " +
            "admin_id = #{adminId}, resolved_at = CASE WHEN #{status} IN ('resolved','closed') THEN NOW() ELSE resolved_at END, " +
            "update_time = NOW() WHERE id = #{id}")
    int updateByAdmin(@Param("id") Long id, @Param("status") String status,
                      @Param("adminReply") String adminReply, @Param("adminId") Integer adminId);

    @Update("UPDATE user_feedback SET is_deleted = 1, update_time = NOW() WHERE id = #{id}")
    int softDelete(@Param("id") Long id);

    @Select("SELECT status, COUNT(*) as cnt FROM user_feedback WHERE is_deleted = 0 GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();
}
