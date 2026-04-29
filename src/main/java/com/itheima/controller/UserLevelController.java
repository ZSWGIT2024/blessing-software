package com.itheima.controller;

import com.itheima.common.UserConstant;
import com.itheima.dto.LevelInfoDTO;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.UserLevelService;
import com.itheima.service.UserService;
import com.itheima.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user/level")
@RequiredArgsConstructor
public class UserLevelController {

    private final UserLevelService userLevelService;

    private final UserMapper userMapper;

    /**
     * 获取当前用户的等级信息
     */
    @GetMapping("/info")
    public Result<LevelInfoDTO> getCurrentUserLevelInfo() {
        try {
            Integer userId = RequestUtils.getCurrentUserId();
            if (userId == null) {
                return Result.error("请先登录");
            }

            LevelInfoDTO levelInfo = userLevelService.getUserLevelInfo(userId);
            return Result.success(levelInfo);

        } catch (Exception e) {
            log.error("获取用户等级信息失败", e);
            return Result.error("获取等级信息失败");
        }
    }

    /**
     * 获取指定用户的等级信息（公开接口）
     */
    @GetMapping("/info/{userId}")
    public Result<LevelInfoDTO> getUserLevelInfo(@PathVariable Integer userId) {
        try {
            // 检查用户是否存在
            User user = userMapper.findUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            LevelInfoDTO levelInfo = userLevelService.getUserLevelInfo(userId);
            return Result.success(levelInfo);

        } catch (Exception e) {
            log.error("获取用户等级信息失败: userId={}", userId, e);
            return Result.error("获取等级信息失败");
        }
    }

    /**
     * 获取当前用户的升级详情
     */
    @GetMapping("/upgrade-details")
    public Result<Map<String, Object>> getUpgradeDetails() {
        try {
            Integer userId = RequestUtils.getCurrentUserId();
            if (userId == null) {
                return Result.error("请先登录");
            }

            Map<String, Object> details = userLevelService.getUpgradeDetails(userId);
            return Result.success(details);

        } catch (Exception e) {
            log.error("获取升级详情失败", e);
            return Result.error("获取升级详情失败");
        }
    }

    /**
     * 获取等级经验表（用于前端显示等级进度条）
     */
    @GetMapping("/exp-table")
    public Result<Map<Integer, Long>> getLevelExpTable() {
        try {
            Map<Integer, Long> expTable = UserConstant.getAllLevelExpRequirements();
            return Result.success(expTable);

        } catch (Exception e) {
            log.error("获取等级经验表失败", e);
            return Result.error("获取等级经验表失败");
        }
    }

    /**
     * 检查是否可以升级
     */
    @GetMapping("/can-upgrade")
    public Result<Map<String, Object>> checkCanUpgrade() {
        try {
            Integer userId = RequestUtils.getCurrentUserId();
            if (userId == null) {
                return Result.error("请先登录");
            }

            Map<String, Object> result = userLevelService.checkCanUpgrade(userId);
            return Result.success(result);

        } catch (Exception e) {
            log.error("检查升级状态失败", e);
            return Result.error("检查升级状态失败");
        }
    }

    /**
     * 获取等级特权列表
     */
    @GetMapping("/privileges")
    public Result<Map<Integer, String[]>> getLevelPrivileges() {
        try {
            Map<Integer, String[]> privileges = new HashMap<>();
            for (int level = 1; level <= UserConstant.MAX_LEVEL; level++) {
                privileges.put(level, UserConstant.getLevelPrivileges(level));
            }
            return Result.success(privileges);

        } catch (Exception e) {
            log.error("获取等级特权失败", e);
            return Result.error("获取等级特权失败");
        }
    }

    /**
     * 获取当前用户的等级排行信息
     */
    @GetMapping("/ranking")
    public Result<Map<String, Object>> getUserRanking() {
        try {
            Integer userId = RequestUtils.getCurrentUserId();
            if (userId == null) {
                return Result.error("请先登录");
            }

            Map<String, Object> ranking = userLevelService.getUserRanking(userId);
            return Result.success(ranking);

        } catch (Exception e) {
            log.error("获取用户排行失败", e);
            return Result.error("获取排行信息失败");
        }
    }

    /**
     * 获取等级统计信息（管理端）
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getLevelStatistics(
            @RequestParam(required = false) Integer level) {
        try {
            // 检查管理员权限
            if (!RequestUtils.isAdmin()) {
                return Result.error("没有权限");
            }

            Map<String, Object> statistics = userLevelService.getLevelStatistics(level);
            return Result.success(statistics);

        } catch (Exception e) {
            log.error("获取等级统计失败", e);
            return Result.error("获取统计信息失败");
        }
    }
}
