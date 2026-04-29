package com.itheima.controller;

import com.itheima.common.UserConstant;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/user")
@Validated
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    /**
     * 检查当前用户是否是管理员
     */
    private boolean checkAdminPermission() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        User currentUser = userService.findUserById(userId);
        return currentUser != null && currentUser.isAdmin();
    }

    // 优化getUserList方法：
    @GetMapping("/list")
    public Result<Map<String, Object>> getUserList(
            @RequestParam(defaultValue = "1", name = "page") @Min(1) Integer page,
            @RequestParam(defaultValue = "20", name = "size") @Min(1) @Max(100) Integer size,
            @RequestParam(required = false, name = "keyword") String keyword,
            @RequestParam(required = false, name = "vipType") Integer vipType,
            @RequestParam(required = false, name = "status") String status,
            @RequestParam(required = false, name = "minLevel") Integer minLevel,
            @RequestParam(required = false, name = "maxLevel") Integer maxLevel) {

        if (!checkAdminPermission()) {
            return Result.error("权限不足");
        }

        Map<String, Object> filters = new HashMap<>();
        filters.put("keyword", keyword);
        filters.put("vipType", vipType);
        filters.put("status", status);
        filters.put("minLevel", minLevel);
        filters.put("maxLevel", maxLevel);

        Map<String, Object> result = userService.getUserList(page, size, filters);
        return Result.success(result);
    }

    /**
     * 获取用户详情（管理员视角）
     */
    @GetMapping("/detail/{id}")
    public Result<User> getUserDetail(@PathVariable("id") Integer id) {
        if (!checkAdminPermission()) {
            return Result.error("权限不足");
        }

        User user = userService.findUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        return Result.success(user);
    }

    /**
     * 封禁/解封用户
     */
    @PatchMapping("/status/{id}")
    public Result<String> updateUserStatus(
            @PathVariable("id") Integer id,
            @RequestParam("status") @NotNull String status) {

        if (!checkAdminPermission()) {
            return Result.error("权限不足");
        }

        // 验证状态参数
        if (!UserConstant.STATUS_ACTIVE.equals(status) &&
                !UserConstant.STATUS_BANNED.equals(status)) {
            return Result.error("状态参数无效");
        }

        // 检查用户是否存在
        User user = userService.findUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 不能修改管理员状态
        if (user.isAdmin()) {
            return Result.error("不能修改管理员状态");
        }

        // 更新用户状态
        boolean success = userService.updateUserStatus(id, status);
        if (success) {
            log.info("管理员更新用户状态: 用户ID={}, 新状态={}", id, status);
            return Result.success("状态更新成功");
        } else {
            return Result.error("状态更新失败");
        }
    }

    /**
     * 更新用户VIP信息
     */
    @PostMapping("/vip/{id}")
    public Result<String> updateUserVip(
            @PathVariable("id") Integer id,
            @RequestParam("vipType") @Min(0) @Max(4) Integer vipType,
            @RequestParam(required = false, name = "days") Integer days) {

        if (!checkAdminPermission()) {
            return Result.error("权限不足");
        }

        User user = userService.findUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        LocalDateTime expireTime = null;

        // 计算VIP过期时间
        if (vipType != null && vipType > 0) {
            if (UserConstant.VIP_TYPE_LIFETIME.equals(vipType)) {
                // 终身VIP，设置很远的未来时间
                expireTime = LocalDateTime.now().plusYears(100);
            } else if (days != null && days > 0) {
                // 设置指定天数
                expireTime = LocalDateTime.now().plusDays(days);
            } else {
                // 默认时长
                expireTime = LocalDateTime.now().plusMonths(
                        switch (vipType) {
                            case 1 -> 1;   // 月度VIP
                            case 2 -> 3;   // 季度VIP
                            case 3 -> 12;  // 年度VIP
                            default -> 1;
                        }
                );
            }
        } else {
            // 取消VIP
            expireTime = null;
        }

        boolean success = userService.updateUserVip(id, vipType, expireTime);
        if (success) {
            log.info("管理员更新用户VIP: 用户ID={}, VIP类型={}, 过期时间={}",
                    id, vipType, expireTime);

            String message = vipType > 0 ?
                    String.format("VIP设置成功，类型: %s", getVipTypeName(vipType)) :
                    "VIP已取消";
            return Result.success(message);
        } else {
            return Result.error("VIP更新失败");
        }
    }

    /**
     * 调整用户积分
     */
    @PostMapping("/coins/{id}")
    public Result<String> updateUserCoins(
            @PathVariable("id") Integer id,
            @RequestParam("amount") Integer amount,
            @RequestParam(required = false, name = "reason") String reason) {

        if (!checkAdminPermission()) {
            return Result.error("权限不足");
        }

        User user = userService.findUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        int newBalance = user.getCoinBalance() + amount;
        if (newBalance < 0) {
            return Result.error("积分不能为负数");
        }

        boolean success = userService.updateCoinBalance(id, newBalance);
        if (success) {
            log.info("管理员调整用户积分: 用户ID={}, 调整={}, 新余额={}, 原因={}",
                    id, amount, newBalance, reason);
            return Result.success(String.format("积分调整成功，新余额: %d", newBalance));
        } else {
            return Result.error("积分调整失败");
        }
    }

    /**
     * 修改用户等级
     */
    @PostMapping("/level/{id}")
    public Result<String> updateUserLevel(
            @PathVariable("id") Integer id,
            @RequestParam("level") @Min(1) Integer level,
            @RequestParam(defaultValue = "0", name = "exp") Long exp) {

        if (!checkAdminPermission()) {
            return Result.error("权限不足");
        }

        User user = userService.findUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 计算下一级所需经验
        Long nextLevelExp = UserConstant.getNextLevelRequiredExp(level);

        boolean success = userService.updateUserLevel(id, level, exp, nextLevelExp);
        if (success) {
            log.info("管理员修改用户等级: 用户ID={}, 新等级={}, 新经验={}", id, level, exp);
            return Result.success(String.format("等级修改成功，当前等级: %d", level));
        } else {
            return Result.error("等级修改失败");
        }
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public Result<List<User>> searchUsers(String keyword) {

        if (!checkAdminPermission()) {
            return Result.error("权限不足");
        }

        if (keyword == null || keyword.isEmpty()) {
            return Result.error("请输入搜索关键词");
        }

        List<User> users = userService.searchUsers(keyword);
        if (users != null) {
            return Result.success(users);
        } else {
            return Result.error("未查询到相关用户，请重新输入");
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        if (!checkAdminPermission()) {
            return Result.error("权限不足");
        }

        Map<String, Object> stats = userService.getUserStatistics();
        return Result.success(stats);
    }

    /**
     * 获取VIP类型名称
     */
    private String getVipTypeName(Integer vipType) {
        return switch (vipType) {
            case 0 -> "非VIP";
            case 1 -> "月度VIP";
            case 2 -> "季度VIP";
            case 3 -> "年度VIP";
            case 4 -> "终身VIP";
            default -> "未知";
        };
    }
}
