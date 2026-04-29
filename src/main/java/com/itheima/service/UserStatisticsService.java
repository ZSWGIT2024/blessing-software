package com.itheima.service;

import java.time.LocalDate;
import java.util.Map;

public interface UserStatisticsService {

    /**
     * 记录用户事件
     * @param userId 用户ID
     * @param eventType 事件类型
     * @param eventData 事件数据
     */
    void recordUserEvent(Integer userId, String eventType, Map<String, Object> eventData);

    /**
     * 记录用户注册事件
     * @param userId 用户ID
     * @param registerData 注册数据
     */
    void recordRegisterEvent(Integer userId, Map<String, Object> registerData);

    /**
     * 记录用户登录事件
     * @param userId 用户ID
     * @param loginData 登录数据
     */
    void recordLoginEvent(Integer userId, Map<String, Object> loginData);

    /**
     * 记录VIP购买事件
     * @param userId 用户ID
     * @param vipData VIP数据
     */
    void recordVipPurchaseEvent(Integer userId, Map<String, Object> vipData);

    /**
     * 记录VIP续费事件
     * @param userId 用户ID
     * @param vipData VIP数据
     */
    void recordVipRenewEvent(Integer userId, Map<String, Object> vipData);

    /**
     * 记录用户封禁事件
     * @param userId 用户ID
     * @param banData 封禁数据
     */
    void recordBanEvent(Integer userId, Map<String, Object> banData);

    /**
     * 记录用户解封事件
     * @param userId 用户ID
     * @param unbanData 解封数据
     */
    void recordUnbanEvent(Integer userId, Map<String, Object> unbanData);

    /**
     * 获取实时统计信息
     * @return 统计信息
     */
    Map<String, Object> getRealTimeStatistics();

    /**
     * 获取用户增长趋势
     * @param days 天数
     * @return 趋势数据
     */
    Map<String, Object> getUserGrowthTrend(Integer days);

    /**
     * 获取活跃用户趋势
     * @param days 天数
     * @return 趋势数据
     */
    Map<String, Object> getActiveUserTrend(Integer days);

    /**
     * 生成每日统计快照
     */
    void generateDailyStatistics();

    /**
     * 获取指定日期的统计快照
     * @param date 日期
     * @return 统计快照
     */
    Map<String, Object> getStatisticsByDate(LocalDate date);
}