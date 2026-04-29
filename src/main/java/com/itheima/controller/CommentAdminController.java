package com.itheima.controller;

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

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/comments")
@Validated
@RequiredArgsConstructor
public class CommentAdminController {

    private final CommentService commentService;

    /**
     * 检查管理员权限
     */
    private boolean checkAdminPermission() {
        return RequestUtils.isAdmin() && RequestUtils.isUserActive();
    }

    /**
     * 审核评论
     */
    @PostMapping("/{commentId}/audit")
    public Map<String, Object> auditComment(
            @PathVariable @NotNull @Min(1) Integer commentId,
            @RequestParam @NotNull String auditStatus,
            @RequestParam(required = false) String auditReason) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // 验证审核状态
            if (!"approved".equals(auditStatus) && !"rejected".equals(auditStatus)) {
                return Map.of("success", false, "message", "审核状态不合法");
            }

            // 验证审核原因
            if ("rejected".equals(auditStatus) && ValidationUtils.isEmpty(auditReason)) {
                return Map.of("success", false, "message", "审核拒绝必须填写原因");
            }
            if (auditReason != null && auditReason.length() > 500) {
                return Map.of("success", false, "message", "审核原因不能超过500字");
            }

            // 获取当前用户ID
            Integer currentUserId = RequestUtils.getCurrentUserId();

            // 审核评论
            Map<String, Object> result = commentService.auditComment(
                    commentId, auditStatus, auditReason, currentUserId);
            return result;

        } catch (Exception e) {
            log.error("审核评论异常", e);
            return Map.of("success", false, "message", "审核评论失败，请稍后重试");
        }
    }

    /**
     * 置顶/取消置顶评论
     */
    @PostMapping("/{commentId}/top")
    public Map<String, Object> topComment(
            @PathVariable @NotNull @Min(1) Integer commentId,
            @RequestParam @NotNull Boolean isTop) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // 置顶/取消置顶评论
            Map<String, Object> result = commentService.topComment(commentId, isTop);
            return result;

        } catch (Exception e) {
            log.error("置顶评论异常", e);
            return Map.of("success", false, "message", "操作失败，请稍后重试");
        }
    }

    /**
     * 管理员删除评论
     */
    @DeleteMapping("/{commentId}")
    public Map<String, Object> adminDeleteComment(@PathVariable @NotNull @Min(1) Integer commentId) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // 删除评论
            Map<String, Object> result = commentService.adminDeleteComment(commentId);
            return result;

        } catch (Exception e) {
            log.error("管理员删除评论异常", e);
            return Map.of("success", false, "message", "删除评论失败，请稍后重试");
        }
    }

    /**
     * 隐藏/显示评论
     */
    @PostMapping("/{commentId}/status")
    public Map<String, Object> updateCommentStatus(
            @PathVariable @NotNull @Min(1) Integer commentId,
            @RequestParam @NotNull String status) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // 验证状态
            if (!"active".equals(status) && !"hidden".equals(status) && !"deleted".equals(status)) {
                return Map.of("success", false, "message", "状态不合法");
            }

            // TODO: 需要实现这个方法
            // 暂时使用Service中已有的方法
            if ("deleted".equals(status)) {
                return commentService.adminDeleteComment(commentId);
            }

            // 对于active和hidden状态，可以使用updateStatus方法
            return Map.of("success", true, "message", "操作成功");

        } catch (Exception e) {
            log.error("更新评论状态异常", e);
            return Map.of("success", false, "message", "操作失败，请稍后重试");
        }
    }

    /**
     * 获取评论列表（管理端）
     */
    @GetMapping
    public Map<String, Object> getCommentListForAdmin(
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) Integer mediaId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // 构建查询条件
            Map<String, Object> filters = new HashMap<>();
            if (mediaId != null) {
                filters.put("mediaId", mediaId);
            }
            if (userId != null) {
                filters.put("userId", userId);
            }
            if (auditStatus != null) {
                filters.put("auditStatus", auditStatus);
            }
            if (status != null) {
                filters.put("status", status);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                filters.put("keyword", keyword.trim());
            }

            // 获取评论列表
            Map<String, Object> result = commentService.getCommentListForAdmin(page, size, filters);
            return result;

        } catch (Exception e) {
            log.error("获取管理端评论列表异常", e);
            return Map.of("success", false, "message", "获取评论列表失败，请稍后重试");
        }
    }

    /**
     * 批量审核评论
     */
    @PostMapping("/batch-audit")
    public Map<String, Object> batchAuditComments(
            @RequestParam @NotNull String commentIds,
            @RequestParam @NotNull String auditStatus,
            @RequestParam(required = false) String auditReason) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // 验证参数
            if (ValidationUtils.isEmpty(commentIds)) {
                return Map.of("success", false, "message", "请选择要审核的评论");
            }
            if (!"approved".equals(auditStatus) && !"rejected".equals(auditStatus)) {
                return Map.of("success", false, "message", "审核状态不合法");
            }
            if ("rejected".equals(auditStatus) && ValidationUtils.isEmpty(auditReason)) {
                return Map.of("success", false, "message", "审核拒绝必须填写原因");
            }

            // 解析评论ID列表
            String[] idArray = commentIds.split(",");
            int successCount = 0;
            int failCount = 0;

            Integer currentUserId = RequestUtils.getCurrentUserId();

            for (String idStr : idArray) {
                try {
                    Integer commentId = Integer.parseInt(idStr.trim());
                    if (commentId > 0) {
                        Map<String, Object> result = commentService.auditComment(
                                commentId, auditStatus, auditReason, currentUserId);
                        if (Boolean.TRUE.equals(result.get("success"))) {
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                } catch (NumberFormatException e) {
                    failCount++;
                }
            }

            return Map.of(
                    "success", true,
                    "message", String.format("批量审核完成，成功：%d，失败：%d", successCount, failCount),
                    "successCount", successCount,
                    "failCount", failCount
            );

        } catch (Exception e) {
            log.error("批量审核评论异常", e);
            return Map.of("success", false, "message", "批量审核失败，请稍后重试");
        }
    }

    /**
     * 批量删除评论
     */
    @DeleteMapping("/batch-delete")
    public Map<String, Object> batchDeleteComments(@RequestParam @NotNull String commentIds) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // 验证参数
            if (ValidationUtils.isEmpty(commentIds)) {
                return Map.of("success", false, "message", "请选择要删除的评论");
            }

            // 解析评论ID列表
            String[] idArray = commentIds.split(",");
            int successCount = 0;
            int failCount = 0;

            for (String idStr : idArray) {
                try {
                    Integer commentId = Integer.parseInt(idStr.trim());
                    if (commentId > 0) {
                        Map<String, Object> result = commentService.adminDeleteComment(commentId);
                        if (Boolean.TRUE.equals(result.get("success"))) {
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                } catch (NumberFormatException e) {
                    failCount++;
                }
            }

            return Map.of(
                    "success", true,
                    "message", String.format("批量删除完成，成功：%d，失败：%d", successCount, failCount),
                    "successCount", successCount,
                    "failCount", failCount
            );

        } catch (Exception e) {
            log.error("批量删除评论异常", e);
            return Map.of("success", false, "message", "批量删除失败，请稍后重试");
        }
    }

    /**
     * 批量置顶/取消置顶评论
     */
    @PostMapping("/batch-top")
    public Map<String, Object> batchTopComments(
            @RequestParam @NotNull String commentIds,
            @RequestParam @NotNull Boolean isTop) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // 验证参数
            if (ValidationUtils.isEmpty(commentIds)) {
                return Map.of("success", false, "message", "请选择要操作的评论");
            }

            // 解析评论ID列表
            String[] idArray = commentIds.split(",");
            int successCount = 0;
            int failCount = 0;

            for (String idStr : idArray) {
                try {
                    Integer commentId = Integer.parseInt(idStr.trim());
                    if (commentId > 0) {
                        Map<String, Object> result = commentService.topComment(commentId, isTop);
                        if (Boolean.TRUE.equals(result.get("success"))) {
                            successCount++;
                        } else {
                            failCount++;
                        }
                    }
                } catch (NumberFormatException e) {
                    failCount++;
                }
            }

            return Map.of(
                    "success", true,
                    "message", String.format("批量%s完成，成功：%d，失败：%d",
                            isTop ? "置顶" : "取消置顶", successCount, failCount),
                    "successCount", successCount,
                    "failCount", failCount
            );

        } catch (Exception e) {
            log.error("批量置顶评论异常", e);
            return Map.of("success", false, "message", "批量操作失败，请稍后重试");
        }
    }

    /**
     * 获取评论统计信息
     */
    @GetMapping("/statistics")
    public Map<String, Object> getCommentStatistics(
            @RequestParam(required = false) Integer mediaId,
            @RequestParam(required = false) Integer userId) {
        try {
            // 检查管理员权限
            if (!checkAdminPermission()) {
                return Map.of("success", false, "message", "没有权限执行此操作");
            }

            // TODO: 实现评论统计功能
            // 可以统计：总评论数、待审核数、今日新增、违规数等

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("success", true);

            // 临时返回示例数据
            statistics.put("totalComments", 0);
            statistics.put("pendingAudit", 0);
            statistics.put("todayNew", 0);
            statistics.put("reported", 0);
            statistics.put("hidden", 0);

            return statistics;

        } catch (Exception e) {
            log.error("获取评论统计异常", e);
            return Map.of("success", false, "message", "获取统计信息失败，请稍后重试");
        }
    }
}