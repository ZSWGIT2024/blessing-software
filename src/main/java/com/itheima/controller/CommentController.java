package com.itheima.controller;

import com.itheima.common.UserConstant;
import com.itheima.pojo.Comment;
import com.itheima.pojo.User;
import com.itheima.service.CommentService;
import com.itheima.utils.RequestUtils;
import com.itheima.utils.ValidationUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user/comments")
@Validated
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 发表评论
     */
    @PostMapping
    public Map<String, Object> addComment(@RequestBody @Validated Comment comment) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", false, "message", "请先登录");
            }

            // 检查用户状态
            if (!RequestUtils.isUserActive()) {
                return Map.of("success", false, "message", "您的账号已被封禁，无法发表评论");
            }

            // 验证必填字段
            if (comment.getMediaId() == null) {
                return Map.of("success", false, "message", "作品ID不能为空");
            }
            if (ValidationUtils.isEmpty(comment.getContent())) {
                return Map.of("success", false, "message", "评论内容不能为空");
            }
            if (comment.getContent().length() > 1000) {
                return Map.of("success", false, "message", "评论内容不能超过1000字");
            }

            // 创建用户对象
            User currentUser = new User();
            currentUser.setId(currentUserId);
            currentUser.setUsername(RequestUtils.getCurrentUsername());
            currentUser.setNickname(RequestUtils.getCurrentNickname());
            currentUser.setUserType(RequestUtils.getCurrentUserType());

            // 获取客户端信息
            String ipAddress = RequestUtils.getClientIp();
            String userAgent = RequestUtils.getUserAgent();

            // 发表评论
            Map<String, Object> result = commentService.addComment(comment, currentUser, ipAddress, userAgent);
            return result;

        } catch (Exception e) {
            log.error("发表评论异常", e);
            return Map.of("success", false, "message", "评论发表失败，请稍后重试");
        }
    }

    /**
     * 回复评论
     */
    @PostMapping("/reply")
    public Map<String, Object> replyComment(@RequestBody @Validated Comment comment) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", false, "message", "请先登录");
            }

            // 检查用户状态
            if (!RequestUtils.isUserActive()) {
                return Map.of("success", false, "message", "您的账号已被封禁，无法回复评论");
            }

            // 验证必填字段
            if (comment.getMediaId() == null) {
                return Map.of("success", false, "message", "作品ID不能为空");
            }
            if (comment.getParentId() == null || comment.getParentId() == 0) {
                return Map.of("success", false, "message", "请指定要回复的评论");
            }
            if (ValidationUtils.isEmpty(comment.getContent())) {
                return Map.of("success", false, "message", "回复内容不能为空");
            }
            if (comment.getContent().length() > 1000) {
                return Map.of("success", false, "message", "回复内容不能超过1000字");
            }

            // 创建用户对象
            User currentUser = new User();
            currentUser.setId(currentUserId);
            currentUser.setUsername(RequestUtils.getCurrentUsername());
            currentUser.setNickname(RequestUtils.getCurrentNickname());
            currentUser.setUserType(RequestUtils.getCurrentUserType());

            // 获取客户端信息
            String ipAddress = RequestUtils.getClientIp();
            String userAgent = RequestUtils.getUserAgent();

            // 回复评论
            Map<String, Object> result = commentService.replyComment(comment, currentUser, ipAddress, userAgent);
            return result;

        } catch (Exception e) {
            log.error("回复评论异常", e);
            return Map.of("success", false, "message", "回复评论失败，请稍后重试");
        }
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public Map<String, Object> deleteComment(@PathVariable @NotNull @Min(1) Integer commentId) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", false, "message", "请先登录");
            }

            // 检查用户状态
            if (!RequestUtils.isUserActive()) {
                return Map.of("success", false, "message", "您的账号已被封禁，无法删除评论");
            }

            // 删除评论
            Map<String, Object> result = commentService.deleteComment(commentId, currentUserId);
            return result;

        } catch (Exception e) {
            log.error("删除评论异常", e);
            return Map.of("success", false, "message", "删除评论失败，请稍后重试");
        }
    }

    /**
     * 获取作品评论列表
     */
    @GetMapping("/media/{mediaId}")
    public Map<String, Object> getMediaComments(
            @PathVariable @NotNull @Min(1) Integer mediaId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        try {
            // 获取当前登录用户ID（可为空）
            Integer currentUserId = RequestUtils.getCurrentUserId();

            // 获取评论列表
            Map<String, Object> result = commentService.getMediaComments(mediaId, page, size, currentUserId);
            return result;

        } catch (Exception e) {
            log.error("获取作品评论列表异常", e);
            return Map.of("success", false, "message", "获取评论列表失败，请稍后重试");
        }
    }

    /**
     * 获取评论详情
     */
    @GetMapping("/{commentId}")
    public Map<String, Object> getCommentDetail(@PathVariable @NotNull @Min(1) Integer commentId) {
        try {
            // 获取当前登录用户ID（可为空）
            Integer currentUserId = RequestUtils.getCurrentUserId();

            // 获取评论详情
            Comment comment = commentService.getCommentDetail(commentId, currentUserId);
            if (comment == null) {
                return Map.of("success", false, "message", "评论不存在或已被删除");
            }

            return Map.of("success", true, "data", comment);

        } catch (Exception e) {
            log.error("获取评论详情异常", e);
            return Map.of("success", false, "message", "获取评论详情失败，请稍后重试");
        }
    }

    /**
     * 获取评论的回复列表
     */
    @GetMapping("/{commentId}/replies")
    public Map<String, Object> getCommentReplies(@PathVariable @NotNull @Min(1) Integer commentId) {
        try {
            // 获取当前登录用户ID（可为空）
            Integer currentUserId = RequestUtils.getCurrentUserId();

            // 获取回复列表
            var replies = commentService.getCommentReplies(commentId, currentUserId);
            return Map.of("success", true, "data", replies);

        } catch (Exception e) {
            log.error("获取评论回复列表异常", e);
            return Map.of("success", false, "message", "获取回复列表失败，请稍后重试");
        }
    }

    /**
     * 点赞评论
     */
    @PostMapping("/{commentId}/like")
    public Map<String, Object> likeComment(@PathVariable @NotNull @Min(1) Integer commentId) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", false, "message", "请先登录");
            }

            // 检查用户状态
            if (!RequestUtils.isUserActive()) {
                return Map.of("success", false, "message", "您的账号已被封禁，无法点赞评论");
            }

            // 点赞评论
            Map<String, Object> result = commentService.likeComment(commentId, currentUserId);
            return result;

        } catch (Exception e) {
            log.error("点赞评论异常", e);
            return Map.of("success", false, "message", "点赞失败，请稍后重试");
        }
    }

    /**
     * 取消点赞评论
     */
    @DeleteMapping("/{commentId}/like")
    public Map<String, Object> unlikeComment(@PathVariable @NotNull @Min(1) Integer commentId) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", false, "message", "请先登录");
            }

            // 取消点赞
            Map<String, Object> result = commentService.unlikeComment(commentId, currentUserId);
            return result;

        } catch (Exception e) {
            log.error("取消点赞评论异常", e);
            return Map.of("success", false, "message", "取消点赞失败，请稍后重试");
        }
    }

    /**
     * 获取用户的评论列表
     */
    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserComments(
            @PathVariable @NotNull @Min(1) Integer userId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        try {
            Map<String, Object> result = commentService.getUserComments(userId, page, size);
            return result;

        } catch (Exception e) {
            log.error("获取用户评论列表异常", e);
            return Map.of("success", false, "message", "获取评论列表失败，请稍后重试");
        }
    }

    /**
     * 获取用户的点赞记录
     */
    @GetMapping("/user/{userId}/likes")
    public Map<String, Object> getUserLikes(
            @PathVariable @NotNull @Min(1) Integer userId,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        try {
            Map<String, Object> result = commentService.getUserLikes(userId, page, size);
            return result;

        } catch (Exception e) {
            log.error("获取用户点赞记录异常", e);
            return Map.of("success", false, "message", "获取点赞记录失败，请稍后重试");
        }
    }

    /**
     * 检查用户是否点赞了评论
     */
    @GetMapping("/{commentId}/like/status")
    public Map<String, Object> getLikeStatus(@PathVariable @NotNull @Min(1) Integer commentId) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", true, "liked", false);
            }

            // 检查点赞状态
            boolean hasLiked = commentService.hasLikedComment(commentId, currentUserId);
            return Map.of("success", true, "liked", hasLiked);

        } catch (Exception e) {
            log.error("检查点赞状态异常", e);
            return Map.of("success", false, "message", "检查点赞状态失败");
        }
    }

    /**
     * 举报评论
     */
    @PostMapping("/{commentId}/report")
    public Map<String, Object> reportComment(
            @PathVariable @NotNull @Min(1) Integer commentId,
            @RequestParam @NotNull String reason) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", false, "message", "请先登录");
            }

            // 检查用户状态
            if (!RequestUtils.isUserActive()) {
                return Map.of("success", false, "message", "您的账号已被封禁，无法举报评论");
            }

            // 验证举报原因
            if (ValidationUtils.isEmpty(reason) || reason.length() > 500) {
                return Map.of("success", false, "message", "举报原因不能为空且不能超过500字");
            }

            // 举报评论
            Map<String, Object> result = commentService.reportComment(commentId, currentUserId, reason);
            return result;

        } catch (Exception e) {
            log.error("举报评论异常", e);
            return Map.of("success", false, "message", "举报失败，请稍后重试");
        }
    }

    /**
     * 获取当前用户的评论列表
     */
    @GetMapping("/my")
    public Map<String, Object> getMyComments(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", false, "message", "请先登录");
            }

            Map<String, Object> result = commentService.getUserComments(currentUserId, page, size);
            return result;

        } catch (Exception e) {
            log.error("获取我的评论列表异常", e);
            return Map.of("success", false, "message", "获取评论列表失败，请稍后重试");
        }
    }

    /**
     * 获取当前用户的点赞记录
     */
    @GetMapping("/my/likes")
    public Map<String, Object> getMyLikes(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        try {
            // 获取当前登录用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();
            if (currentUserId == null) {
                return Map.of("success", false, "message", "请先登录");
            }

            Map<String, Object> result = commentService.getUserLikes(currentUserId, page, size);
            return result;

        } catch (Exception e) {
            log.error("获取我的点赞记录异常", e);
            return Map.of("success", false, "message", "获取点赞记录失败，请稍后重试");
        }
    }
}