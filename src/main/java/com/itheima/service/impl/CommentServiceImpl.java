package com.itheima.service.impl;

import com.itheima.common.UserConstant;
import com.itheima.mapper.CommentLikeMapper;
import com.itheima.mapper.CommentMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.Comment;
import com.itheima.pojo.CommentLike;
import com.itheima.pojo.User;
import com.itheima.service.CommentService;
import com.itheima.service.ExpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;

    private final CommentLikeMapper commentLikeMapper;

    private final UserMapper userMapper;

    private final ExpService expService;

    @Override
    @Transactional
    public Map<String, Object> addComment(Comment comment, User user, String ipAddress, String userAgent) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 设置评论信息
            comment.setUserId(user.getId());
            comment.setStatus("active");
            comment.setAuditStatus("approved"); // 默认待审核
            comment.setIpAddress(ipAddress);
            comment.setUserAgent(userAgent);

            // 如果是顶级评论，rootId设为0
            if (comment.getParentId() == null || comment.getParentId() == 0) {
                comment.setParentId(0);
                comment.setRootId(0);
            } else {
                // 如果是回复评论，需要设置rootId
                Comment parentComment = commentMapper.findById(comment.getParentId());
                if (parentComment == null) {
                    result.put("success", false);
                    result.put("message", "父评论不存在");
                    return result;
                }

                // 设置rootId
                int rootId = parentComment.getRootId() == 0 ? parentComment.getId() : parentComment.getRootId();
                comment.setRootId(rootId);
                comment.setReplyToUserId(parentComment.getUserId());
                comment.setReplyToCommentId(parentComment.getId());

                // 更新父评论的回复数
                commentMapper.updateReplyCount(parentComment.getId(), 1);

                // 更新根评论的回复数
                if (rootId > 0) {
                    commentMapper.updateRootReplyCount(rootId, 1);
                }
            }

            // 保存评论
            int insertResult = commentMapper.insert(comment);
            if (insertResult <= 0) {
                result.put("success", false);
                result.put("message", "评论发布失败");
                return result;
            }

            // 增加用户经验（发表评论）
            expService.addExperience(user.getId(), UserConstant.ExpAction.COMMENT,
                    "发表评论: " + (comment.getContent().length() > 20 ?
                            comment.getContent().substring(0, 20) + "..." : comment.getContent()));

            // 更新用户表的评论数
            userMapper.incrementCommentCount(user.getId());

            log.info("用户 {} 发表评论: commentId={}, mediaId={}",
                    user.getId(), comment.getId(), comment.getMediaId());

            result.put("success", true);
            result.put("message", "评论发布成功");
            result.put("commentId", comment.getId());
            result.put("comment", comment);

        } catch (Exception e) {
            log.error("发表评论失败", e);
            result.put("success", false);
            result.put("message", "评论发布失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> replyComment(Comment comment, User user, String ipAddress, String userAgent) {
        // 回复评论其实就是发表评论的一种特殊情况
        return addComment(comment, user, ipAddress, userAgent);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteComment(Integer commentId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查评论是否存在
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }

            // 检查权限：只能删除自己的评论
            if (!comment.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "无权删除此评论");
                return result;
            }

            // 软删除评论
            int deleteResult = commentMapper.deleteById(commentId, userId);
            if (deleteResult <= 0) {
                result.put("success", false);
                result.put("message", "删除评论失败");
                return result;
            }

            // 如果是顶级评论，需要同时删除所有回复
            if (comment.getParentId() == 0) {
                List<Comment> replies = commentMapper.findRepliesByRootId(commentId);
                for (Comment reply : replies) {
                    commentMapper.deleteById(reply.getId(), reply.getUserId());
                }
            }

            // 更新用户表的评论数
            userMapper.decrementCommentCount(userId);

            log.info("用户 {} 删除评论: commentId={}", userId, commentId);

            result.put("success", true);
            result.put("message", "评论删除成功");

        } catch (Exception e) {
            log.error("删除评论失败", e);
            result.put("success", false);
            result.put("message", "删除评论失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getMediaComments(Integer mediaId, Integer page, Integer size, Integer currentUserId) {
        Map<String, Object> result = new HashMap<>();

        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        int offset = (page - 1) * size;

        try {
            // 查询评论总数
            Long total = commentMapper.countByMediaId(mediaId);
            if (total == null) total = 0L;

            // 查询评论列表
            List<Comment> comments = commentMapper.findByMediaId(mediaId, offset, size);

            // 设置用户信息和点赞状态
            enrichCommentsWithUserInfo(comments, currentUserId);

            // 获取热门评论（前3条）
            List<Comment> hotComments = commentMapper.findHotComments(mediaId, 3);
            if (!hotComments.isEmpty()) {
                enrichCommentsWithUserInfo(hotComments, currentUserId);
            }

            // 计算总页数
            int totalPages = (int) Math.ceil((double) total / size);

            result.put("success", true);
            result.put("page", page);
            result.put("size", size);
            result.put("total", total);
            result.put("totalPages", totalPages);
            result.put("comments", comments);
            result.put("hotComments", hotComments);

        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            result.put("success", false);
            result.put("message", "获取评论失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 丰富评论信息：添加用户信息和点赞状态
     */
    private void enrichCommentsWithUserInfo(List<Comment> comments, Integer currentUserId) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        // 获取所有用户ID
        List<Integer> userIds = new ArrayList<>();
        List<Integer> commentIds = new ArrayList<>();

        for (Comment comment : comments) {
            userIds.add(comment.getUserId());
            if (comment.getReplyToUserId() != null) {
                userIds.add(comment.getReplyToUserId());
            }
            commentIds.add(comment.getId());
        }

        // 批量查询用户信息
        Map<Integer, User> userMap = new HashMap<>();
        for (Integer userId : userIds) {
            User user = userMapper.findUserById(userId);
            if (user != null) {
                userMap.put(userId, user);
            }
        }

        // 批量查询点赞状态
        List<Integer> likedCommentIds = new ArrayList<>();
        if (currentUserId != null && !commentIds.isEmpty()) {
            likedCommentIds = commentLikeMapper.findLikedCommentIds(currentUserId, commentIds);
        }

        // 设置用户信息和点赞状态
        for (Comment comment : comments) {
            // 设置评论用户信息
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setUser(user);
            }

            // 设置被回复用户信息
            if (comment.getReplyToUserId() != null) {
                User replyToUser = userMap.get(comment.getReplyToUserId());
                if (replyToUser != null) {
                    comment.setReplyToUser(replyToUser);
                }
            }

            // 设置点赞状态
            comment.setIsLiked(likedCommentIds.contains(comment.getId()));
        }
    }

    @Override
    public Comment getCommentDetail(Integer commentId, Integer currentUserId) {
        try {
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                return null;
            }

            // 设置用户信息
            User user = userMapper.findUserById(comment.getUserId());
            if (user != null) {
                comment.setUser(user);
            }

            // 设置点赞状态
            if (currentUserId != null) {
                int exists = commentLikeMapper.exists(commentId, currentUserId);
                comment.setIsLiked(exists > 0);
            }

            // 如果是顶级评论，获取回复列表
            if (comment.getParentId() == 0) {
                List<Comment> replies = commentMapper.findRepliesByRootId(commentId);
                enrichCommentsWithUserInfo(replies, currentUserId);
                comment.setReplies(replies);
            }

            return comment;
        } catch (Exception e) {
            log.error("获取评论详情失败", e);
            return null;
        }
    }

    @Override
    public List<Comment> getCommentReplies(Integer commentId, Integer currentUserId) {
        try {
            List<Comment> replies = commentMapper.findRepliesByRootId(commentId);
            enrichCommentsWithUserInfo(replies, currentUserId);
            return replies;
        } catch (Exception e) {
            log.error("获取评论回复失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional
    public Map<String, Object> likeComment(Integer commentId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查评论是否存在
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }

            // 检查是否已点赞
            int exists = commentLikeMapper.exists(commentId, userId);
            if (exists > 0) {
                result.put("success", false);
                result.put("message", "已点赞过此评论");
                return result;
            }

            // 添加点赞记录
            CommentLike commentLike = new CommentLike();
            commentLike.setCommentId(commentId);
            commentLike.setUserId(userId);
            int insertResult = commentLikeMapper.insert(commentLike);

            if (insertResult <= 0) {
                result.put("success", false);
                result.put("message", "点赞失败");
                return result;
            }

            // 更新评论点赞数
            commentMapper.updateLikeCount(commentId, 1);

            // 更新用户表的获赞数（评论的作者）
            userMapper.incrementLikedCount(comment.getUserId());

            // 增加用户经验（收到点赞）
            expService.addExperience(comment.getUserId(), UserConstant.ExpAction.LIKE_RECEIVED,
                    "评论收到点赞");

            log.info("用户 {} 点赞评论: commentId={}", userId, commentId);

            result.put("success", true);
            result.put("message", "点赞成功");
            result.put("likeCount", comment.getLikeCount() + 1);

        } catch (Exception e) {
            log.error("点赞评论失败", e);
            result.put("success", false);
            result.put("message", "点赞失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> unlikeComment(Integer commentId, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查是否已点赞
            int exists = commentLikeMapper.exists(commentId, userId);
            if (exists == 0) {
                result.put("success", false);
                result.put("message", "未点赞过此评论");
                return result;
            }

            // 删除点赞记录
            int deleteResult = commentLikeMapper.delete(commentId, userId);
            if (deleteResult <= 0) {
                result.put("success", false);
                result.put("message", "取消点赞失败");
                return result;
            }

            // 更新评论点赞数
            commentMapper.updateLikeCount(commentId, -1);

            // 更新用户表的获赞数
            Comment comment = commentMapper.findById(commentId);
            if (comment != null) {
                userMapper.decrementLikedCount(comment.getUserId());
            }

            log.info("用户 {} 取消点赞评论: commentId={}", userId, commentId);

            result.put("success", true);
            result.put("message", "取消点赞成功");
            result.put("likeCount", comment != null ? Math.max(0, comment.getLikeCount() - 1) : 0);

        } catch (Exception e) {
            log.error("取消点赞评论失败", e);
            result.put("success", false);
            result.put("message", "取消点赞失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getUserComments(Integer userId, Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();

        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        int offset = (page - 1) * size;

        try {
            // 查询评论总数
            Long total = commentMapper.countByUserId(userId);
            if (total == null) total = 0L;

            // 查询评论列表
            List<Comment> comments = commentMapper.findByUserId(userId, offset, size);

            // 设置用户信息
            enrichCommentsWithUserInfo(comments, userId);

            // 计算总页数
            int totalPages = (int) Math.ceil((double) total / size);

            result.put("success", true);
            result.put("page", page);
            result.put("size", size);
            result.put("total", total);
            result.put("totalPages", totalPages);
            result.put("comments", comments);

        } catch (Exception e) {
            log.error("获取用户评论列表失败", e);
            result.put("success", false);
            result.put("message", "获取评论列表失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getUserLikes(Integer userId, Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();

        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        int offset = (page - 1) * size;

        try {
            // 查询点赞记录
            List<CommentLike> likes = commentLikeMapper.findByUserId(userId, offset, size);

            // 获取评论信息
            List<Comment> comments = new ArrayList<>();
            for (CommentLike like : likes) {
                Comment comment = commentMapper.findById(like.getCommentId());
                if (comment != null) {
                    comments.add(comment);
                }
            }

            // 设置用户信息
            enrichCommentsWithUserInfo(comments, userId);

            result.put("success", true);
            result.put("page", page);
            result.put("size", size);
            result.put("total", likes.size());
            result.put("likedComments", comments);

        } catch (Exception e) {
            log.error("获取用户点赞记录失败", e);
            result.put("success", false);
            result.put("message", "获取点赞记录失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public boolean hasLikedComment(Integer commentId, Integer userId) {
        try {
            int exists = commentLikeMapper.exists(commentId, userId);
            return exists > 0;
        } catch (Exception e) {
            log.error("检查点赞状态失败", e);
            return false;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> reportComment(Integer commentId, Integer userId, String reason) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查评论是否存在
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }

            // 不能举报自己的评论
            if (comment.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "不能举报自己的评论");
                return result;
            }

            // 增加举报数
            int updateResult = commentMapper.incrementReportCount(commentId);
            if (updateResult <= 0) {
                result.put("success", false);
                result.put("message", "举报失败");
                return result;
            }

            log.info("用户 {} 举报评论: commentId={}, reason={}", userId, commentId, reason);

            result.put("success", true);
            result.put("message", "举报成功，我们会尽快处理");

        } catch (Exception e) {
            log.error("举报评论失败", e);
            result.put("success", false);
            result.put("message", "举报失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> auditComment(Integer commentId, String auditStatus, String auditReason, Integer auditUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查评论是否存在
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }

            // 更新审核状态
            comment.setAuditStatus(auditStatus);
            comment.setAuditReason(auditReason);
            comment.setAuditTime(LocalDateTime.now());
            comment.setAuditUserId(auditUserId);

            int updateResult = commentMapper.updateAuditStatus(comment);
            if (updateResult <= 0) {
                result.put("success", false);
                result.put("message", "审核失败");
                return result;
            }

            // 如果审核不通过，隐藏评论
            if ("rejected".equals(auditStatus)) {
                commentMapper.updateStatus(commentId, "hidden");
            }

            log.info("管理员 {} 审核评论: commentId={}, status={}", auditUserId, commentId, auditStatus);

            result.put("success", true);
            result.put("message", "审核成功");

        } catch (Exception e) {
            log.error("审核评论失败", e);
            result.put("success", false);
            result.put("message", "审核失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> topComment(Integer commentId, Boolean isTop) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查评论是否存在
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }

            // 更新置顶状态
            int updateResult = commentMapper.updateTopStatus(commentId, isTop);
            if (updateResult <= 0) {
                result.put("success", false);
                result.put("message", "操作失败");
                return result;
            }

            log.info("管理员置顶评论: commentId={}, isTop={}", commentId, isTop);

            result.put("success", true);
            result.put("message", isTop ? "评论已置顶" : "评论已取消置顶");

        } catch (Exception e) {
            log.error("置顶评论失败", e);
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> adminDeleteComment(Integer commentId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 检查评论是否存在
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }

            // 硬删除评论（实际是软删除，修改状态）
            int deleteResult = commentMapper.updateStatus(commentId, "deleted");
            if (deleteResult <= 0) {
                result.put("success", false);
                result.put("message", "删除失败");
                return result;
            }

            log.info("管理员删除评论: commentId={}", commentId);

            result.put("success", true);
            result.put("message", "评论删除成功");

        } catch (Exception e) {
            log.error("管理员删除评论失败", e);
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getCommentListForAdmin(Integer page, Integer size, Map<String, Object> filters) {
        Map<String, Object> result = new HashMap<>();

        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        int offset = (page - 1) * size;

        try {
            // 从filters中获取参数
            Integer mediaId = (Integer) filters.get("mediaId");
            Integer userId = (Integer) filters.get("userId");
            String auditStatus = (String) filters.get("auditStatus");
            String status = (String) filters.get("status");
            String keyword = (String) filters.get("keyword");

            // 查询评论列表
            List<Comment> comments = commentMapper.findPageForAdmin(offset, size, mediaId, userId,
                    auditStatus, status, keyword);

            // 设置用户信息
            enrichCommentsWithUserInfo(comments, null);

            // 这里需要实现统计总数的方法，暂时返回简单结果
            result.put("success", true);
            result.put("page", page);
            result.put("size", size);
            result.put("total", comments.size());
            result.put("comments", comments);

        } catch (Exception e) {
            log.error("获取管理员评论列表失败", e);
            result.put("success", false);
            result.put("message", "获取评论列表失败: " + e.getMessage());
        }

        return result;
    }
}