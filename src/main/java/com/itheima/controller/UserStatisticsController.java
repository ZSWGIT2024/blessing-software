package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.service.UserStatisticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/statistics")
@Validated
@RequiredArgsConstructor
public class UserStatisticsController {

    private final UserStatisticsService userStatisticsService;

    /**
     * 获取实时统计信息
     */
    @GetMapping("/realtime")
    public Result<Map<String, Object>> getRealTimeStatistics() {
        try {
            Map<String, Object> stats = userStatisticsService.getRealTimeStatistics();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取实时统计信息失败", e);
            return Result.error("获取统计信息失败");
        }
    }

    /**
     * 获取用户增长趋势
     */
    @GetMapping("/growth-trend")
    public Result<Map<String, Object>> getUserGrowthTrend(
            @RequestParam(defaultValue = "30", name = "days") @Min(1) @Max(365) Integer days) {
        try {
            Map<String, Object> trend = userStatisticsService.getUserGrowthTrend(days);
            return Result.success(trend);
        } catch (Exception e) {
            log.error("获取用户增长趋势失败: days={}", days, e);
            return Result.error("获取趋势数据失败");
        }
    }

    /**
     * 获取活跃用户趋势
     */
    @GetMapping("/active-trend")
    public Result<Map<String, Object>> getActiveUserTrend(
            @RequestParam(defaultValue = "30", name = "days") @Min(1) @Max(365) Integer days) {
        try {
            Map<String, Object> trend = userStatisticsService.getActiveUserTrend(days);
            return Result.success(trend);
        } catch (Exception e) {
            log.error("获取活跃用户趋势失败: days={}", days, e);
            return Result.error("获取趋势数据失败");
        }
    }

    /**
     * 获取指定日期统计
     */
    @GetMapping("/by-date")
    public Result<Map<String, Object>> getStatisticsByDate(
            @RequestParam(name = "date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            Map<String, Object> stats = userStatisticsService.getStatisticsByDate(date);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取日期统计失败: date={}", date, e);
            return Result.error("获取统计信息失败");
        }
    }

    /**
     * 获取最近N天的统计列表
     */
    @GetMapping("/recent")
    public Result<Map<String, Object>> getRecentStatistics(
            @RequestParam(defaultValue = "7", name = "days") @Min(1) @Max(90) Integer days) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("days", days);

            // 这里可以获取最近days天的详细统计数据
            // 实际实现时可以从 daily_user_statistics 表查询

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取最近统计列表失败: days={}", days, e);
            return Result.error("获取统计列表失败");
        }
    }

    /**
     * 手动触发统计生成
     */
    @PostMapping("/generate")
    public Result<String> generateStatistics() {
        try {
            userStatisticsService.generateDailyStatistics();
            return Result.success("统计生成任务已触发");
        } catch (Exception e) {
            log.error("手动触发统计生成失败", e);
            return Result.error("统计生成失败");
        }
    }
}
