package com.itheima.service;

import com.itheima.pojo.UserPoints;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserPointsService {

    /**
     * 获取用户积分余额
     */
    Integer getUserCoinBalance(Integer userId);

    /**
     * 获取用户积分流水记录
     */
    Object getPointsRecords(Integer userId, Integer page, Integer size);

    /**
     * 添加积分（赚取）
     */
    Map<String, Object> earnPoints(Integer userId, Integer amount, String businessType,
                                   Integer businessId, String description);

    /**
     * 消费积分
     */
    Map<String, Object> spendPoints(Integer userId, Integer amount, String businessType,
                                    Integer businessId, String description);

    /**
     * 系统奖励积分
     */
    Map<String, Object> rewardPoints(Integer userId, Integer amount, String description);

    /**
     * 检查积分是否足够
     */
    boolean hasEnoughPoints(Integer userId, Integer requiredAmount);

    /**
     * 获取今日已获取积分
     */
    Integer getTodayEarnedPoints(Integer userId);

    /**
     * 获取积分统计
     */
    Map<String, Long> getPointsStatistics(Integer userId);


    // 在UserPointsService接口中添加：
    /**
     * 根据交易类型查询流水
     */
    List<UserPoints> getRecordsByType(Integer userId, String transactionType);

    /**
     * 根据业务类型查询流水
     */
    List<UserPoints> getRecordsByBusinessType(Integer userId, String businessType);

    /**
     * 查询时间段内的流水
     */
    List<UserPoints> getRecordsByTimeRange(Integer userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取最近的积分记录
     */
    UserPoints getLatestRecord(Integer userId);

    /**
     * 获取用户本月积分收入
     */
    Integer getMonthlyEarned(Integer userId);

    /**
     * 获取用户本周积分消费
     */
    Integer getWeeklySpent(Integer userId);
}