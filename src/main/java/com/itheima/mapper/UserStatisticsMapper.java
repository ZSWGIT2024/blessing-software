package com.itheima.mapper;

import com.itheima.pojo.DailyUserStatistics;
import com.itheima.pojo.UserEventStatistics;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserStatisticsMapper {

    // 插入用户事件
    @Insert("INSERT INTO user_event_statistics (user_id, event_type, event_time, event_data, ip_address, location, device_info) " +
            "VALUES (#{userId}, #{eventType}, #{eventTime}, #{eventData}, #{ipAddress}, #{location}, #{deviceInfo})")
    int insertUserEvent(UserEventStatistics event);

    // 获取最近N天的统计数据
    @Select("SELECT * FROM daily_user_statistics WHERE stat_date >= #{startDate} ORDER BY stat_date DESC")
    List<DailyUserStatistics> getRecentStatistics(LocalDate startDate);

    // 获取指定日期的统计数据
    @Select("SELECT * FROM daily_user_statistics WHERE stat_date = #{date}")
    DailyUserStatistics getStatisticsByDate(LocalDate date);

    // 更新或插入每日统计
    @Insert("INSERT INTO daily_user_statistics (stat_date, total_users, new_users, active_users, vip_users, banned_users, " +
            "total_logins, unique_logins, vip_purchases, vip_renews, vip_expires, avg_level, avg_coins, " +
            "user_retention_rate, vip_conversion_rate) " +
            "VALUES (#{statDate}, #{totalUsers}, #{newUsers}, #{activeUsers}, #{vipUsers}, #{bannedUsers}, " +
            "#{totalLogins}, #{uniqueLogins}, #{vipPurchases}, #{vipRenews}, #{vipExpires}, #{avgLevel}, #{avgCoins}, " +
            "#{userRetentionRate}, #{vipConversionRate}) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_users = VALUES(total_users), " +
            "new_users = VALUES(new_users), " +
            "active_users = VALUES(active_users), " +
            "vip_users = VALUES(vip_users), " +
            "banned_users = VALUES(banned_users), " +
            "total_logins = VALUES(total_logins), " +
            "unique_logins = VALUES(unique_logins), " +
            "vip_purchases = VALUES(vip_purchases), " +
            "vip_renews = VALUES(vip_renews), " +
            "vip_expires = VALUES(vip_expires), " +
            "avg_level = VALUES(avg_level), " +
            "avg_coins = VALUES(avg_coins), " +
            "user_retention_rate = VALUES(user_retention_rate), " +
            "vip_conversion_rate = VALUES(vip_conversion_rate), " +
            "updated_at = CURRENT_TIMESTAMP")
    int upsertDailyStatistics(DailyUserStatistics statistics);

    // 统计今日新增用户
    @Select("SELECT COUNT(*) FROM user WHERE DATE(create_time) = CURDATE()")
    int countTodayNewUsers();

    // 统计今日活跃用户（登录过的）
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_event_statistics " +
            "WHERE event_type = 'login' AND DATE(event_time) = CURDATE()")
    int countTodayActiveUsers();

    // 统计今日VIP购买
    @Select("SELECT COUNT(*) FROM user_event_statistics " +
            "WHERE event_type IN ('vip_purchase', 'vip_renew') AND DATE(event_time) = CURDATE()")
    int countTodayVipTransactions();

    // 统计用户增长趋势
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM user " +
            "WHERE create_time >= #{startDate} " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date")
    List<Map<String, Object>> getUserGrowthTrend(LocalDate startDate);

    // 统计活跃用户趋势
    @Select("SELECT DATE(event_time) as date, COUNT(DISTINCT user_id) as count FROM user_event_statistics " +
            "WHERE event_type = 'login' AND event_time >= #{startDate} " +
            "GROUP BY DATE(event_time) " +
            "ORDER BY date")
    List<Map<String, Object>> getActiveUserTrend(LocalDate startDate);

    // 获取VIP类型分布
    @Select("SELECT vip_type, COUNT(*) as count FROM user WHERE vip_type > 0 GROUP BY vip_type")
    List<Map<String, Object>> getVipTypeDistribution();

    // 获取用户状态分布
    @Select("SELECT status, COUNT(*) as count FROM user GROUP BY status")
    List<Map<String, Object>> getUserStatusDistribution();

    // 计算平均等级和积分
    @Select("SELECT AVG(level) as avg_level, AVG(coin_balance) as avg_coins FROM user WHERE status = 'active'")
    Map<String, Object> getAverageMetrics();


    /**
     * 统计留存用户数
     * @param registerDate 注册日期
     */
    @Select("SELECT COUNT(DISTINCT u.id) FROM user u " +
            "INNER JOIN user_event_statistics e ON u.id = e.user_id " +
            "WHERE DATE(u.create_time) = #{registerDate} " +
            "AND e.event_type = 'login' " +
            "AND e.event_time BETWEEN #{registerDate} AND DATE_ADD(#{registerDate}, INTERVAL 7 DAY)")
    Integer countRetainedUsers(@Param("registerDate") LocalDate registerDate);

}