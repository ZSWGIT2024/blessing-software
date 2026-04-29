package com.itheima.mapper;

import com.itheima.pojo.Comment;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper {

    /**
     * 添加评论
     */
    @Insert("INSERT INTO comment(media_id, user_id, content, content_html, mentioned_users, " +
            "parent_id, root_id, reply_to_user_id, reply_to_comment_id, status, ip_address, user_agent, " +
            "create_time, update_time) " +
            "VALUES(#{mediaId}, #{userId}, #{content}, #{contentHtml}, " +
            "#{mentionedUsers, typeHandler=com.itheima.handler.JsonTypeHandler}, " +
            "#{parentId}, #{rootId}, #{replyToUserId}, #{replyToCommentId}, #{status}, " +
            "#{ipAddress}, #{userAgent}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    /**
     * 根据ID查询评论
     */
    @Select("SELECT * FROM comment WHERE id = #{id}")
    @Results({
            @Result(property = "mentionedUsers", column = "mentioned_users",
                    typeHandler = com.itheima.handler.JsonTypeHandler.class)
    })
    Comment findById(@Param("id") Integer id);

    /**
     * 根据作品ID查询评论列表（顶级评论）
     */
    @Select("SELECT c.* FROM comment c " +
            "WHERE c.media_id = #{mediaId} AND c.parent_id = 0 AND c.status = 'active' " +
            "ORDER BY " +
            "CASE WHEN c.is_top = 1 THEN 0 ELSE 1 END, " +  // 置顶评论在前
            "c.create_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "mentionedUsers", column = "mentioned_users",
                    typeHandler = com.itheima.handler.JsonTypeHandler.class)
    })
    List<Comment> findByMediaId(@Param("mediaId") Integer mediaId,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);

    /**
     * 查询评论的回复列表
     */
    @Select("SELECT c.* FROM comment c " +
            "WHERE c.root_id = #{rootId} AND c.parent_id != 0 AND c.status = 'active' " +
            "ORDER BY c.create_time ASC")
    @Results({
            @Result(property = "mentionedUsers", column = "mentioned_users",
                    typeHandler = com.itheima.handler.JsonTypeHandler.class)
    })
    List<Comment> findRepliesByRootId(@Param("rootId") Integer rootId);

    /**
     * 查询用户的评论列表
     */
    @Select("SELECT * FROM comment WHERE user_id = #{userId} AND status = 'active' " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    @Results({
            @Result(property = "mentionedUsers", column = "mentioned_users",
                    typeHandler = com.itheima.handler.JsonTypeHandler.class)
    })
    List<Comment> findByUserId(@Param("userId") Integer userId,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    /**
     * 更新评论内容
     */
    @Update("UPDATE comment SET content = #{content}, content_html = #{contentHtml}, " +
            "mentioned_users = #{mentionedUsers, typeHandler=com.itheima.handler.JsonTypeHandler}, " +
            "update_time = now() WHERE id = #{id} AND user_id = #{userId}")
    int updateContent(Comment comment);

    /**
     * 删除评论（软删除）
     */
    @Update("UPDATE comment SET status = 'deleted', update_time = now() " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int deleteById(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 更新评论点赞数
     */
    @Update("UPDATE comment SET like_count = like_count + #{delta}, update_time = now() " +
            "WHERE id = #{commentId}")
    int updateLikeCount(@Param("commentId") Integer commentId, @Param("delta") Integer delta);

    /**
     * 更新评论回复数
     */
    @Update("UPDATE comment SET reply_count = reply_count + #{delta}, update_time = now() " +
            "WHERE id = #{commentId}")
    int updateReplyCount(@Param("commentId") Integer commentId, @Param("delta") Integer delta);

    /**
     * 更新根评论的回复数
     */
    @Update("UPDATE comment SET reply_count = reply_count + #{delta}, update_time = now() " +
            "WHERE id = #{rootId}")
    int updateRootReplyCount(@Param("rootId") Integer rootId, @Param("delta") Integer delta);

    /**
     * 统计作品的评论数
     */
    @Select("SELECT COUNT(*) FROM comment " +
            "WHERE media_id = #{mediaId} AND status = 'active' AND parent_id = 0")
    Long countByMediaId(@Param("mediaId") Integer mediaId);

    /**
     * 统计用户的评论数
     */
    @Select("SELECT COUNT(*) FROM comment WHERE user_id = #{userId} AND status = 'active'")
    Long countByUserId(@Param("userId") Integer userId);

    /**
     * 查询热门评论（按点赞数排序）
     */
    @Select("SELECT * FROM comment " +
            "WHERE media_id = #{mediaId} AND status = 'active' AND parent_id = 0 " +
            "ORDER BY like_count DESC, create_time DESC " +
            "LIMIT #{limit}")
    @Results({
            @Result(property = "mentionedUsers", column = "mentioned_users",
                    typeHandler = com.itheima.handler.JsonTypeHandler.class)
    })
    List<Comment> findHotComments(@Param("mediaId") Integer mediaId, @Param("limit") Integer limit);

    /**
     * 管理员：更新评论审核状态
     */
    @Update("UPDATE comment SET audit_status = #{auditStatus}, audit_reason = #{auditReason}, " +
            "audit_time = #{auditTime}, audit_user_id = #{auditUserId}, update_time = now() " +
            "WHERE id = #{id}")
    int updateAuditStatus(Comment comment);

    /**
     * 管理员：更新评论状态
     */
    @Update("UPDATE comment SET status = #{status}, update_time = now() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    /**
     * 管理员：置顶/取消置顶评论
     */
    @Update("UPDATE comment SET is_top = #{isTop}, update_time = now() WHERE id = #{id}")
    int updateTopStatus(@Param("id") Integer id, @Param("isTop") Boolean isTop);

    /**
     * 管理员：分页查询评论（用于审核）
     */
    @Select("<script>" +
            "SELECT * FROM comment " +
            "<where>" +
            "<if test='mediaId != null'>" +
            "   AND media_id = #{mediaId}" +
            "</if>" +
            "<if test='userId != null'>" +
            "   AND user_id = #{userId}" +
            "</if>" +
            "<if test='auditStatus != null and auditStatus != \"\"'>" +
            "   AND audit_status = #{auditStatus}" +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "   AND status = #{status}" +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "   AND content LIKE CONCAT('%', #{keyword}, '%')" +
            "</if>" +
            "</where>" +
            " ORDER BY create_time DESC " +
            " LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results({
            @Result(property = "mentionedUsers", column = "mentioned_users",
                    typeHandler = com.itheima.handler.JsonTypeHandler.class)
    })
    List<Comment> findPageForAdmin(@Param("offset") Integer offset,
                                   @Param("limit") Integer limit,
                                   @Param("mediaId") Integer mediaId,
                                   @Param("userId") Integer userId,
                                   @Param("auditStatus") String auditStatus,
                                   @Param("status") String status,
                                   @Param("keyword") String keyword);

    /**
     * 增加举报数
     */
    @Update("UPDATE comment SET report_count = report_count + 1, update_time = now() " +
            "WHERE id = #{commentId}")
    int incrementReportCount(@Param("commentId") Integer commentId);
}