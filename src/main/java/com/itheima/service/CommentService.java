package com.itheima.service;

import com.itheima.pojo.Comment;
import com.itheima.pojo.User;

import java.util.List;
import java.util.Map;

public interface CommentService {

    /**
     * 发表评论
     */
    Map<String, Object> addComment(Comment comment, User user, String ipAddress, String userAgent);

    /**
     * 回复评论
     */
    Map<String, Object> replyComment(Comment comment, User user, String ipAddress, String userAgent);

    /**
     * 删除评论
     */
    Map<String, Object> deleteComment(Integer commentId, Integer userId);

    /**
     * 获取作品评论列表
     */
    Map<String, Object> getMediaComments(Integer mediaId, Integer page, Integer size, Integer currentUserId);

    /**
     * 获取评论详情
     */
    Comment getCommentDetail(Integer commentId, Integer currentUserId);

    /**
     * 获取评论的回复列表
     */
    List<Comment> getCommentReplies(Integer commentId, Integer currentUserId);

    /**
     * 点赞评论
     */
    Map<String, Object> likeComment(Integer commentId, Integer userId);

    /**
     * 取消点赞评论
     */
    Map<String, Object> unlikeComment(Integer commentId, Integer userId);

    /**
     * 获取用户的评论列表
     */
    Map<String, Object> getUserComments(Integer userId, Integer page, Integer size);

    /**
     * 获取用户的点赞记录
     */
    Map<String, Object> getUserLikes(Integer userId, Integer page, Integer size);

    /**
     * 检查用户是否点赞了评论
     */
    boolean hasLikedComment(Integer commentId, Integer userId);

    /**
     * 举报评论
     */
    Map<String, Object> reportComment(Integer commentId, Integer userId, String reason);

    /**
     * 管理员：审核评论
     */
    Map<String, Object> auditComment(Integer commentId, String auditStatus, String auditReason, Integer auditUserId);

    /**
     * 管理员：置顶评论
     */
    Map<String, Object> topComment(Integer commentId, Boolean isTop);

    /**
     * 管理员：删除评论（硬删除）
     */
    Map<String, Object> adminDeleteComment(Integer commentId);

    /**
     * 管理员：获取评论列表（用于审核）
     */
    Map<String, Object> getCommentListForAdmin(Integer page, Integer size, Map<String, Object> filters);
}