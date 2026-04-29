package com.itheima.controller;

import com.itheima.common.UserConstant;
import com.itheima.pojo.Result;
import com.itheima.service.ExpService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/exp")
@RequiredArgsConstructor
public class ExpController {

    private final ExpService expService;

    /**
     * 获取用户等级信息
     */
    @GetMapping("/level-info")
    public Result<Map<String, Object>> getLevelInfo() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Map<String, Object> levelInfo = expService.getUserLevelInfo(userId);
        if (!Boolean.TRUE.equals(levelInfo.get("exists"))) {
            return Result.error("用户不存在");
        }

        return Result.success(levelInfo);
    }

    /**
     * 获取今日经验统计
     */
    @GetMapping("/today-stats")
    public Result<Map<String, Object>> getTodayStats() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Map<String, Object> stats = expService.getTodayExpStats(userId);
        return Result.success(stats);
    }

    /**
     * 执行经验动作（通用接口）
     */
    @PostMapping("/action")
    public Result<Map<String, Object>> executeAction(
            @RequestParam String actionType,
            @RequestParam(defaultValue = "") String description) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Map<String, Object> result = expService.addExperience(userId, actionType, description);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 获取所有可执行的动作
     */
    @GetMapping("/actions")
    public Result<Map<String, String>> getAllActions() {
        Map<String, String> actions = UserConstant.ExpAction.getAllActions();
        return Result.success(actions);
    }

    /**
     * 上传作品获得经验
     */
    @PostMapping("/action/upload")
    public Result<Map<String, Object>> uploadAction(
            @RequestParam Integer mediaId,
            @RequestParam(defaultValue = "上传作品") String title) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        String description = String.format("上传作品: %s (ID: %d)", title, mediaId);
        Map<String, Object> result = expService.addExperience(userId,
                UserConstant.ExpAction.UPLOAD, description);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 发表评论获得经验
     */
    @PostMapping("/action/comment")
    public Result<Map<String, Object>> commentAction(
            @RequestParam Integer commentId,
            @RequestParam(defaultValue = "") String content) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        // 截取内容前50字作为描述
        String descContent = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        String description = String.format("发表评论: %s (ID: %d)", descContent, commentId);

        Map<String, Object> result = expService.addExperience(userId,
                UserConstant.ExpAction.COMMENT, description);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 每日登录获得经验
     */
    @PostMapping("/action/daily-login")
    public Result<Map<String, Object>> dailyLoginAction() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Map<String, Object> result = expService.addExperience(userId,
                UserConstant.ExpAction.DAILY_LOGIN, "每日登录奖励");

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 收到点赞获得经验
     */
    @PostMapping("/action/like-received")
    public Result<Map<String, Object>> likeReceivedAction(
            @RequestParam Integer mediaId,
            @RequestParam Integer fromUserId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        String description = String.format("收到用户 %d 对作品 %d 的点赞", fromUserId, mediaId);
        Map<String, Object> result = expService.addExperience(userId,
                UserConstant.ExpAction.LIKE_RECEIVED, description);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }

    /**
     * 完善资料获得经验
     */
    @PostMapping("/action/complete-profile")
    public Result<Map<String, Object>> completeProfileAction() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Map<String, Object> result = expService.addExperience(userId,
                UserConstant.ExpAction.COMPLETE_PROFILE, "完善个人资料");

        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        } else {
            return Result.error((String) result.get("message"));
        }
    }
}
