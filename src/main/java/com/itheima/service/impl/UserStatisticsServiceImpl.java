package com.itheima.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.mapper.UserStatisticsMapper;
import com.itheima.pojo.DailyUserStatistics;
import com.itheima.pojo.UserEventStatistics;
import com.itheima.service.UserStatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private final UserStatisticsMapper userStatisticsMapper;

    private final UserMapper userMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final HttpServletRequest request;

    // 使用Jackson的ObjectMapper来序列化JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存相关常量
    private static final String STATS_CACHE_KEY = "admin:user:stats";
    private static final String GROWTH_TREND_CACHE_KEY = "admin:user:growth:trend";
    private static final String ACTIVE_TREND_CACHE_KEY = "admin:user:active:trend";

    @Override
    @Transactional
    public void recordUserEvent(Integer userId, String eventType, Map<String, Object> eventData) {
        try {
            UserEventStatistics event = new UserEventStatistics();
            event.setUserId(userId);
            event.setEventType(eventType);
            event.setEventTime(LocalDateTime.now());
            event.setEventData(objectMapper.writeValueAsString(eventData));

            // 获取客户端信息
            if (request != null) {
                String ip = getClientIp();
                event.setIpAddress(ip);
                event.setLocation("未知");
                event.setDeviceInfo(request.getHeader("User-Agent"));
            }

            userStatisticsMapper.insertUserEvent(event);

            // 清除缓存，保证数据实时性
            clearStatisticsCache();

            log.info("记录用户事件: userId={}, eventType={}", userId, eventType);
        } catch (JsonProcessingException e) {
            log.error("序列化事件数据失败: userId={}, eventType={}", userId, eventType, e);
        } catch (Exception e) {
            log.error("记录用户事件失败: userId={}, eventType={}", userId, eventType, e);
        }
    }

    @Override
    public void recordRegisterEvent(Integer userId, Map<String, Object> registerData) {
        recordUserEvent(userId, "register", registerData);
    }

    @Override
    public void recordLoginEvent(Integer userId, Map<String, Object> loginData) {
        recordUserEvent(userId, "login", loginData);
    }

    @Override
    public void recordVipPurchaseEvent(Integer userId, Map<String, Object> vipData) {
        recordUserEvent(userId, "vip_purchase", vipData);
    }

    @Override
    public void recordVipRenewEvent(Integer userId, Map<String, Object> vipData) {
        recordUserEvent(userId, "vip_renew", vipData);
    }

    @Override
    public void recordBanEvent(Integer userId, Map<String, Object> banData) {
        recordUserEvent(userId, "ban", banData);
    }

    @Override
    public void recordUnbanEvent(Integer userId, Map<String, Object> unbanData) {
        recordUserEvent(userId, "unban", unbanData);
    }

    @Override
    public Map<String, Object> getRealTimeStatistics() {
        // 先从缓存获取
        Map<String, Object> cachedStats = (Map<String, Object>) redisTemplate.opsForValue().get(STATS_CACHE_KEY);
        if (cachedStats != null) {
            return cachedStats;
        }

        // 计算实时统计数据
        Map<String, Object> stats = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        try {
            // 总用户数
            Integer totalUsers = userMapper.countTotalUsers();
            stats.put("totalUsers", totalUsers != null ? totalUsers : 0);

            // 今日新增用户
            Integer todayNewUsers = userStatisticsMapper.countTodayNewUsers();
            stats.put("todayNewUsers", todayNewUsers != null ? todayNewUsers : 0);
            stats.put("monthNewUsers", totalUsers); // 这里简化处理，实际应该查询本月新增

            // 今日活跃用户
            Integer todayActiveUsers = userStatisticsMapper.countTodayActiveUsers();
            stats.put("todayActiveUsers", todayActiveUsers != null ? todayActiveUsers : 0);

            // 7日活跃用户
            Integer weeklyActiveUsers = userMapper.countActiveUsers(7);
            stats.put("weeklyActiveUsers", weeklyActiveUsers != null ? weeklyActiveUsers : 0);

            // 30日活跃用户
            Integer monthlyActiveUsers = userMapper.countActiveUsers(30);
            stats.put("monthlyActiveUsers", monthlyActiveUsers != null ? monthlyActiveUsers : 0);
            stats.put("activeUsers", monthlyActiveUsers); // 总活跃用户使用30日活跃

            // VIP用户数
            Integer vipUsers = userMapper.countVipUsers();
            stats.put("vipUsers", vipUsers != null ? vipUsers : 0);

            // 封禁用户数
            Integer bannedUsers = userMapper.countBannedUsers();
            stats.put("bannedUsers", bannedUsers != null ? bannedUsers : 0);

            // 今日VIP交易
            Integer todayVipTransactions = userStatisticsMapper.countTodayVipTransactions();
            stats.put("todayVipTransactions", todayVipTransactions != null ? todayVipTransactions : 0);

            // 今日封禁用户
            stats.put("todayBanned", 0); // 这个需要单独统计

            // 本月封禁用户
            stats.put("monthBanned", 0); // 这个需要单独统计

            // 用户增长率（今日/昨日）
            DailyUserStatistics yesterdayStats = userStatisticsMapper.getStatisticsByDate(yesterday);
            int yesterdayNew = yesterdayStats != null ? yesterdayStats.getNewUsers() : 0;
            double userGrowthRate = yesterdayNew > 0 ?
                    ((double) (todayNewUsers - yesterdayNew) / yesterdayNew) * 100 :
                    (todayNewUsers > 0 ? 100 : 0);
            stats.put("userGrowthRate", Math.round(userGrowthRate * 100) / 100.0);

            // 活跃用户率
            double activeUserRate = totalUsers > 0 ?
                    ((double) monthlyActiveUsers / totalUsers) * 100 : 0;
            stats.put("activeUserRate", Math.round(activeUserRate * 100) / 100.0);

            // VIP转化率
            double vipConversionRate = totalUsers > 0 ?
                    ((double) vipUsers / totalUsers) * 100 : 0;
            stats.put("vipConversionRate", Math.round(vipConversionRate * 100) / 100.0);

            // VIP增长率
            int yesterdayVip = yesterdayStats != null ? yesterdayStats.getVipUsers() : 0;
            double vipGrowthRate = yesterdayVip > 0 ?
                    ((double) (vipUsers - yesterdayVip) / yesterdayVip) * 100 :
                    (vipUsers > 0 ? 100 : 0);
            stats.put("vipGrowthRate", Math.round(vipGrowthRate * 100) / 100.0);

            // 封禁变化率
            int yesterdayBanned = yesterdayStats != null ? yesterdayStats.getBannedUsers() : 0;
            double bannedChange = yesterdayBanned > 0 ?
                    ((double) (bannedUsers - yesterdayBanned) / yesterdayBanned) * 100 : 0;
            stats.put("bannedChange", Math.round(bannedChange * 100) / 100.0);

            // 平均等级和积分
            Map<String, Object> avgMetrics = userStatisticsMapper.getAverageMetrics();
            if (avgMetrics != null) {
                stats.put("avgLevel", ((Number) avgMetrics.getOrDefault("avg_level", 0)).doubleValue());
                stats.put("avgCoins", ((Number) avgMetrics.getOrDefault("avg_coins", 0)).doubleValue());
            } else {
                stats.put("avgLevel", 0.0);
                stats.put("avgCoins", 0.0);
            }

            // 用户留存率（7日） - 这里调用实际的留存率计算方法
            Double retentionRate = calculateRealRetentionRate();
            stats.put("userRetention", retentionRate != null ? Math.round(retentionRate * 100) / 100.0 : 0);

            // VIP类型分布
            List<Map<String, Object>> vipDistribution = userStatisticsMapper.getVipTypeDistribution();
            Map<Integer, Integer> vipTypeCounts = new HashMap<>();
            for (Map<String, Object> item : vipDistribution) {
                Integer vipType = (Integer) item.get("vip_type");
                Long count = (Long) item.get("count");
                vipTypeCounts.put(vipType, count.intValue());
            }
            stats.put("vipTypeCounts", vipTypeCounts);

            // 用户状态分布
            List<Map<String, Object>> statusDistribution = userStatisticsMapper.getUserStatusDistribution();
            Map<String, Integer> statusCounts = new HashMap<>();
            for (Map<String, Object> item : statusDistribution) {
                String status = (String) item.get("status");
                Long count = (Long) item.get("count");
                statusCounts.put(status, count.intValue());
            }
            stats.put("statusDistribution", statusCounts);

            // 以下为StatisticsPanel.vue需要的其他字段
            stats.put("dailyActiveAvg", Math.round(weeklyActiveUsers / 7.0)); // 平均日活跃
            stats.put("dailyActiveChange", 0.0); // 需要计算
            stats.put("retentionChange", 0.0); // 需要计算
            stats.put("conversionChange", 0.0); // 需要计算
            stats.put("levelChange", 0.0); // 需要计算
            stats.put("coinsChange", 0.0); // 需要计算

            // 存入缓存，有效期5分钟
            redisTemplate.opsForValue().set(STATS_CACHE_KEY, stats, 5, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("计算实时统计数据失败", e);
            return getDefaultStats();
        }

        return stats;
    }

    /**
     * 计算真实的留存率（7日留存）
     */
    private Double calculateRealRetentionRate() {
        try {
            // 获取7天前的注册用户
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);

            // 统计7天前的注册用户数
            Integer registerCount = userMapper.countNewUsersByDate(sevenDaysAgo);

            if (registerCount == null || registerCount == 0) {
                return 0.0;
            }

            // 统计这些用户在注册后7天内登录过的数量
            Integer retainedCount = userStatisticsMapper.countRetainedUsers(sevenDaysAgo);

            if (retainedCount == null) {
                return 0.0;
            }

            return (retainedCount.doubleValue() / registerCount) * 100;
        } catch (Exception e) {
            log.error("计算留存率失败", e);
            return 0.0;
        }
    }

    @Override
    public Map<String, Object> getUserGrowthTrend(Integer days) {
        String cacheKey = GROWTH_TREND_CACHE_KEY + ":" + days;
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Map<String, Object>> trendData = userStatisticsMapper.getUserGrowthTrend(startDate);

        Map<String, Object> result = new HashMap<>();
        result.put("days", days);
        result.put("trendData", trendData);

        // 缓存30分钟
        redisTemplate.opsForValue().set(cacheKey, result, 30, TimeUnit.MINUTES);

        return result;
    }

    @Override
    public Map<String, Object> getActiveUserTrend(Integer days) {
        String cacheKey = ACTIVE_TREND_CACHE_KEY + ":" + days;
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LocalDate startDate = LocalDate.now().minusDays(days);
        List<Map<String, Object>> trendData = userStatisticsMapper.getActiveUserTrend(startDate);

        // 处理数据格式，转换为前端需要的格式
        List<Map<String, Object>> formattedData = formatActiveTrendData(trendData, days);

        Map<String, Object> result = new HashMap<>();
        result.put("days", days);
        result.put("trendData", formattedData);

        // 缓存30分钟
        redisTemplate.opsForValue().set(cacheKey, result, 30, TimeUnit.MINUTES);

        return result;
    }

    /**
     * 格式化活跃趋势数据
     */
    private List<Map<String, Object>> formatActiveTrendData(List<Map<String, Object>> rawData, int days) {
        // 这里可以将数据库查询的数据格式化为前端需要的格式
        // 例如：将日期格式化为 YYYY-MM-DD，确保数据连续性等
        return rawData;
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void generateDailyStatistics() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);

            DailyUserStatistics stats = new DailyUserStatistics();
            stats.setStatDate(yesterday);

            // 统计昨日数据
            stats.setTotalUsers(userMapper.countTotalUsersByDate(yesterday));
            stats.setNewUsers(userMapper.countNewUsersByDate(yesterday));
            stats.setActiveUsers(userMapper.countActiveUsersByDate(yesterday));
            stats.setVipUsers(userMapper.countVipUsersByDate(yesterday));
            stats.setBannedUsers(userMapper.countBannedUsersByDate(yesterday));
            stats.setTotalLogins(userMapper.countTotalLoginsByDate(yesterday));
            stats.setUniqueLogins(userMapper.countUniqueLoginsByDate(yesterday));

            // VIP购买和续费统计需要单独实现
            stats.setVipPurchases(0); // 需要实现
            stats.setVipRenews(0); // 需要实现
            stats.setVipExpires(0); // 需要实现

            // 计算平均值
            Map<String, Object> avgMetrics = userMapper.getAverageMetricsByDate(yesterday);
            if (avgMetrics != null) {
                stats.setAvgLevel(((Number) avgMetrics.getOrDefault("avg_level", 0)).doubleValue());
                stats.setAvgCoins(((Number) avgMetrics.getOrDefault("avg_coins", 0)).doubleValue());
            }

            // 计算真实的留存率
            Double retentionRate = calculateRealRetentionRate();
            stats.setUserRetentionRate(retentionRate != null ? retentionRate : 0.0);

            // 计算真实的VIP转化率
            Double vipConversionRate = calculateRealVipConversionRate(yesterday);
            stats.setVipConversionRate(vipConversionRate != null ? vipConversionRate : 0.0);

            userStatisticsMapper.upsertDailyStatistics(stats);

            log.info("生成每日统计快照成功: date={}", yesterday);

            // 清除相关缓存
            clearStatisticsCache();
        } catch (Exception e) {
            log.error("生成每日统计快照失败", e);
        }
    }

    /**
     * 计算真实的VIP转化率
     */
    private Double calculateRealVipConversionRate(LocalDate date) {
        try {
            // 统计截止该日期的总用户数
            Integer totalUsers = userMapper.countTotalUsersByDate(date);
            if (totalUsers == null || totalUsers == 0) {
                return 0.0;
            }

            // 统计截止该日期的VIP用户数
            Integer vipUsers = userMapper.countVipUsersByDate(date);
            if (vipUsers == null) {
                return 0.0;
            }

            return (vipUsers.doubleValue() / totalUsers) * 100;
        } catch (Exception e) {
            log.error("计算VIP转化率失败: date={}", date, e);
            return 0.0;
        }
    }

    @Override
    public Map<String, Object> getStatisticsByDate(LocalDate date) {
        DailyUserStatistics stats = userStatisticsMapper.getStatisticsByDate(date);
        if (stats == null) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("statDate", stats.getStatDate());
        result.put("totalUsers", stats.getTotalUsers());
        result.put("newUsers", stats.getNewUsers());
        result.put("activeUsers", stats.getActiveUsers());
        result.put("vipUsers", stats.getVipUsers());
        result.put("bannedUsers", stats.getBannedUsers());
        result.put("avgLevel", stats.getAvgLevel());
        result.put("avgCoins", stats.getAvgCoins());
        result.put("userRetentionRate", stats.getUserRetentionRate());
        result.put("vipConversionRate", stats.getVipConversionRate());

        return result;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理时，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 清除统计缓存
     */
    private void clearStatisticsCache() {
        redisTemplate.delete(STATS_CACHE_KEY);

        // 使用keys命令需要谨慎，生产环境建议使用SCAN
        try {
            redisTemplate.delete(redisTemplate.keys(GROWTH_TREND_CACHE_KEY + ":*"));
            redisTemplate.delete(redisTemplate.keys(ACTIVE_TREND_CACHE_KEY + ":*"));
        } catch (Exception e) {
            log.warn("清除统计缓存时出错", e);
        }
    }

    /**
     * 获取默认统计信息
     */
    private Map<String, Object> getDefaultStats() {
        Map<String, Object> defaultStats = new HashMap<>();
        defaultStats.put("totalUsers", 0);
        defaultStats.put("todayNewUsers", 0);
        defaultStats.put("monthNewUsers", 0);
        defaultStats.put("todayActiveUsers", 0);
        defaultStats.put("weeklyActiveUsers", 0);
        defaultStats.put("monthlyActiveUsers", 0);
        defaultStats.put("activeUsers", 0);
        defaultStats.put("vipUsers", 0);
        defaultStats.put("bannedUsers", 0);
        defaultStats.put("todayVipTransactions", 0);
        defaultStats.put("todayBanned", 0);
        defaultStats.put("monthBanned", 0);
        defaultStats.put("userGrowthRate", 0.0);
        defaultStats.put("activeUserRate", 0.0);
        defaultStats.put("vipConversionRate", 0.0);
        defaultStats.put("vipGrowthRate", 0.0);
        defaultStats.put("bannedChange", 0.0);
        defaultStats.put("avgLevel", 0.0);
        defaultStats.put("avgCoins", 0.0);
        defaultStats.put("userRetention", 0.0);
        defaultStats.put("dailyActiveAvg", 0.0);
        defaultStats.put("dailyActiveChange", 0.0);
        defaultStats.put("retentionChange", 0.0);
        defaultStats.put("conversionChange", 0.0);
        defaultStats.put("levelChange", 0.0);
        defaultStats.put("coinsChange", 0.0);
        defaultStats.put("vipTypeCounts", new HashMap<>());
        defaultStats.put("statusDistribution", new HashMap<>());

        return defaultStats;
    }
}