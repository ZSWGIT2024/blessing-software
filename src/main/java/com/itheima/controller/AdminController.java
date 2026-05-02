package com.itheima.controller;

import com.itheima.annotation.Log;
import com.itheima.annotation.LogType;
import com.itheima.annotation.Monitor;
import com.itheima.annotation.RequireAdmin;
import com.itheima.annotation.RequirePermission;
import com.itheima.pojo.Result;
import com.itheima.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 后台管理控制器
 * 提供管理员专用的系统管理接口
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@Validated
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final OperationLogService operationLogService;
    private final LoginRecordService loginRecordService;

    // ==================== 系统监控 ====================

    /**
     * 获取系统仪表盘数据
     */
    @RequireAdmin
    @Monitor(threshold = 3000)
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {

        Map<String, Object> dashboard = new HashMap<>();

        // 用户统计
        Map<String, Object> userStats = userService.getUserStatistics();
        dashboard.put("userStats", userStats);

        // 登录统计
        Map<String, Object> loginStats = loginRecordService.getLoginStatistics();
        dashboard.put("loginStats", loginStats);

        // 操作日志统计
        Map<String, Object> logStats = operationLogService.getLogStatistics();
        dashboard.put("logStats", logStats);

        // 今日数据
        Map<String, Object> todayData = new HashMap<>();
        todayData.put("todayLogins", loginRecordService.getTodayLoginCount());
        todayData.put("timestamp", LocalDateTime.now().toString());
        dashboard.put("todayData", todayData);

        return Result.success(dashboard);
    }

    /**
     * 获取用户统计数据
     */
    @RequirePermission(value = "user:statistics", description = "查看用户统计")
    @Monitor(threshold = 2000)
    @GetMapping("/statistics/users")
    public Result<Map<String, Object>> getUserStatistics() {
        Map<String, Object> stats = userService.getUserStatistics();
        return Result.success(stats);
    }

    /**
     * 获取登录统计数据
     */
    @RequirePermission(value = "system:monitor", description = "查看登录统计")
    @GetMapping("/statistics/login")
    public Result<Map<String, Object>> getLoginStatistics() {
        Map<String, Object> stats = loginRecordService.getLoginStatistics();
        return Result.success(stats);
    }

    // ==================== 操作日志 ====================

    /**
     * 分页查询操作日志
     */
    @RequirePermission(value = "system:log", description = "查看操作日志")
    @GetMapping("/logs/operation")
    public Result<Map<String, Object>> getOperationLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        Map<String, Object> filters = new HashMap<>();
        filters.put("module", module);
        filters.put("type", type);
        filters.put("userId", userId);
        filters.put("status", status);
        filters.put("startTime", startTime);
        filters.put("endTime", endTime);

        Map<String, Object> data = operationLogService.getLogList(page, size, filters);
        return Result.success(data);
    }

    /**
     * 查看操作日志详情
     */
    @RequirePermission(value = "system:log", description = "查看操作日志详情")
    @GetMapping("/logs/operation/{id}")
    public Result<?> getOperationLogDetail(@PathVariable("id") Long id) {
        Object log = operationLogService.getById(id);
        if (log == null) {
            return Result.error("日志不存在");
        }
        return Result.success(log);
    }

    /**
     * 清理旧日志
     */
    @RequirePermission(value = "system:log", description = "清理操作日志")
    @Log(module = "系统维护", operation = "清理操作日志", type = LogType.DELETE)
    @DeleteMapping("/logs/operation/clean")
    public Result<Integer> cleanOldLogs(@RequestParam String beforeDate) {
        int deleted = operationLogService.deleteBeforeDate(beforeDate);
        log.info("清理操作日志：beforeDate={}, deleted={}", beforeDate, deleted);
        return Result.success(deleted);
    }

    // ==================== 登录记录 ====================

    /**
     * 分页查询登录记录
     */
    @RequirePermission(value = "system:log", description = "查看登录记录")
    @GetMapping("/logs/login")
    public Result<Map<String, Object>> getLoginRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        Map<String, Object> filters = new HashMap<>();
        if (userId != null) filters.put("userId", userId);
        filters.put("loginType", loginType);
        filters.put("status", status);
        filters.put("startTime", startTime);
        filters.put("endTime", endTime);

        Map<String, Object> data = loginRecordService.getRecordList(page, size, filters);
        return Result.success(data);
    }

    /**
     * 查看用户登录记录
     */
    @RequirePermission(value = "system:log", description = "查看用户登录记录")
    @GetMapping("/logs/login/user/{userId}")
    public Result<?> getUserLoginRecords(
            @PathVariable("userId") Integer userId,
            @RequestParam(defaultValue = "20") Integer limit) {

        Object records = loginRecordService.getRecordsByUserId(userId, limit);
        return Result.success(records);
    }

    /**
     * 检测异常登录
     */
    @RequirePermission(value = "system:log", description = "检测异常登录")
    @GetMapping("/logs/login/abnormal/{userId}")
    public Result<?> detectAbnormalLogins(@PathVariable("userId") Integer userId) {
        Object abnormalLogins = loginRecordService.detectAbnormalLogins(userId);
        return Result.success(abnormalLogins);
    }

    // ==================== 系统信息 ====================

    /**
     * 获取系统运行时信息
     */
    @RequireAdmin
    @GetMapping("/system/info")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        // JVM信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + "MB");
        jvm.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + "MB");
        jvm.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + "MB");
        jvm.put("availableProcessors", runtime.availableProcessors());
        info.put("jvm", jvm);

        // 系统属性
        Map<String, Object> system = new HashMap<>();
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("serverTime", LocalDateTime.now().toString());
        info.put("system", system);

        return Result.success(info);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        return Result.success(health);
    }
}
