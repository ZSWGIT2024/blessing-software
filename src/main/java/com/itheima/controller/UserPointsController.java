package com.itheima.controller;

import com.itheima.common.UserConstant;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.pojo.UserPoints;
import com.itheima.pojo.VipConfig;
import com.itheima.service.UserPointsService;
import com.itheima.service.VipConfigService;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/points")
@Validated
@Slf4j
@RequiredArgsConstructor
public class UserPointsController {

    private final UserPointsService userPointsService;

    private final VipConfigService vipConfigService;

    private final UserMapper userMapper;

    /**
     * 获取用户积分余额
     */
    @GetMapping("/balance")
    public Result<Integer> getCoinBalance() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Integer balance = userPointsService.getUserCoinBalance(userId);
        return Result.success(balance);
    }

    /**
     * 获取积分流水记录
     */
    @GetMapping("/records")
    public Result<Object> getPointsRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Object records = userPointsService.getPointsRecords(userId, page, size);
        return Result.success(records);
    }

    /**
     * 积分赚取记录（如完成任务等）
     */
    @PostMapping("/earn")
    public Result<Map<String, Object>> earnPoints(
            @RequestParam @Min(1) Integer amount,
            @RequestParam String businessType,
            @RequestParam(required = false) Integer businessId,
            @RequestParam String description) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        try {
            Map<String, Object> result = userPointsService.earnPoints(userId, amount,
                    businessType, businessId, description);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 积分消费
     */
    @PostMapping("/spend")
    public Result<Map<String, Object>> spendPoints(
            @RequestParam @Min(1) Integer amount,
            @RequestParam String businessType,
            @RequestParam(required = false) Integer businessId,
            @RequestParam String description) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        try {
            Map<String, Object> result = userPointsService.spendPoints(userId, amount,
                    businessType, businessId, description);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("积分消费失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日已获取积分
     */
    @GetMapping("/today-earned")
    public Result<Integer> getTodayEarned() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Integer todayEarned = userPointsService.getTodayEarnedPoints(userId);
        return Result.success(todayEarned);
    }

    /**
     * 获取积分统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Long>> getStatistics() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Map<String, Long> statistics = userPointsService.getPointsStatistics(userId);
        return Result.success(statistics);
    }

    /**
     * 检查积分是否足够
     */
    @GetMapping("/check-enough")
    public Result<Boolean> checkEnoughPoints(@RequestParam @Min(1) Integer requiredAmount) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        boolean hasEnough = userPointsService.hasEnoughPoints(userId, requiredAmount);
        return Result.success(hasEnough);
    }


    // 在UserPointsController中添加以下方法：

    /**
     * 根据交易类型查询积分流水
     */
    @GetMapping("/records/type/{transactionType}")
    public Result<List<UserPoints>> getRecordsByType(@PathVariable String transactionType) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        List<UserPoints> records = userPointsService.getRecordsByType(userId, transactionType);
        return Result.success(records);
    }

    /**
     * 根据业务类型查询积分流水
     */
    @GetMapping("/records/business/{businessType}")
    public Result<List<UserPoints>> getRecordsByBusinessType(@PathVariable String businessType) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        List<UserPoints> records = userPointsService.getRecordsByBusinessType(userId, businessType);
        return Result.success(records);
    }

    /**
     * 获取最近的积分记录
     */
    @GetMapping("/records/latest")
    public Result<UserPoints> getLatestRecord() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        UserPoints latest = userPointsService.getLatestRecord(userId);
        return Result.success(latest);
    }

    /**
     * 获取本月积分收入
     */
    @GetMapping("/monthly-earned")
    public Result<Integer> getMonthlyEarned() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Integer monthlyEarned = userPointsService.getMonthlyEarned(userId);
        return Result.success(monthlyEarned);
    }

    /**
     * 获取本周积分消费
     */
    @GetMapping("/weekly-spent")
    public Result<Integer> getWeeklySpent() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Integer weeklySpent = userPointsService.getWeeklySpent(userId);
        return Result.success(weeklySpent);
    }

}