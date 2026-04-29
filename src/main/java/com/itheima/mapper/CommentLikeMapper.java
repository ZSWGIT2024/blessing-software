package com.itheima.mapper;

import com.itheima.pojo.CommentLike;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentLikeMapper {

    /**
     * 添加点赞记录
     */
    @Insert("INSERT INTO comment_like(comment_id, user_id, create_time) " +
            "VALUES(#{commentId}, #{userId}, now())")
    int insert(CommentLike commentLike);

    /**
     * 删除点赞记录
     */
    @Delete("DELETE FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int delete(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    /**
     * 查询用户是否点赞了评论
     */
    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int exists(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    /**
     * 查询评论的点赞列表
     */
    @Select("SELECT * FROM comment_like WHERE comment_id = #{commentId} ORDER BY create_time DESC")
    List<CommentLike> findByCommentId(@Param("commentId") Integer commentId);

    /**
     * 查询用户的点赞记录
     */
    @Select("SELECT * FROM comment_like WHERE user_id = #{userId} ORDER BY create_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<CommentLike> findByUserId(@Param("userId") Integer userId,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);

    /**
     * 统计评论的点赞数
     */
    @Select("SELECT COUNT(*) FROM comment_like WHERE comment_id = #{commentId}")
    int countByCommentId(@Param("commentId") Integer commentId);

    /**
     * 批量查询用户是否点赞了评论
     */
    @Select("<script>" +
            "SELECT comment_id FROM comment_like WHERE user_id = #{userId} AND comment_id IN " +
            "<foreach collection='commentIds' item='commentId' open='(' separator=',' close=')'>" +
            "   #{commentId}" +
            "</foreach>" +
            "</script>")
    List<Integer> findLikedCommentIds(@Param("userId") Integer userId,
                                      @Param("commentIds") List<Integer> commentIds);
}
