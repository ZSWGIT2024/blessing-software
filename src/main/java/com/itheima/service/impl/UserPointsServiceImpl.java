package com.itheima.service.impl;

import com.itheima.mapper.UserMapper;
import com.itheima.mapper.UserPointsMapper;
import com.itheima.pojo.User;
import com.itheima.pojo.UserPoints;
import com.itheima.service.UserPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPointsServiceImpl implements UserPointsService {

    private final UserMapper userMapper;

    private final UserPointsMapper userPointsMapper;

    @Override
    public Integer getUserCoinBalance(Integer userId) {
        return userMapper.getCoinBalance(userId);
    }

    @Override
    public Object getPointsRecords(Integer userId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        int offset = (page - 1) * size;

        // 查询总记录数
        List<UserPoints> allRecords = userPointsMapper.findByUserId(userId);
        int total = allRecords.size();

        // 分页查询
        List<UserPoints> records = userPointsMapper.findPage(userId, offset, size);

        // 统计信息
        Map<String, Long> stats = userPointsMapper.getStatistics(userId);

        return Map.of(
                "page", page,
                "size", size,
                "total", total,
                "totalPages", (int) Math.ceil((double) total / size),
                "records", records,
                "statistics", stats
        );
    }

    @Override
    @Transactional
    public Map<String, Object> earnPoints(Integer userId, Integer amount,
                                          String businessType, Integer businessId, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("积分数量必须大于0");
        }

        // 获取当前余额
        Integer oldBalance = userMapper.getCoinBalance(userId);
        if (oldBalance == null) {
            throw new RuntimeException("用户不存在");
        }

        int newBalance = oldBalance + amount;

        // 原子操作增加积分
        int affected = userMapper.addCoinBalance(userId, amount);
        if (affected == 0) {
            throw new RuntimeException("增加积分失败");
        }

        // 记录积分流水
        UserPoints record = new UserPoints();
        record.setUserId(userId);
        record.setTransactionType("earn");
        record.setAmount(amount);
        record.setBalanceAfter(newBalance);
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setDescription(description);

        userPointsMapper.insert(record);

        log.info("用户 {} 赚取积分: {}，业务类型: {}，描述: {}，旧余额: {}，新余额: {}",
                userId, amount, businessType, description, oldBalance, newBalance);

        return Map.of(
                "success", true,
                "userId", userId,
                "amount", amount,
                "oldBalance", oldBalance,
                "newBalance", newBalance,
                "recordId", record.getId(),
                "message", "积分赚取成功"
        );
    }

    @Override
    @Transactional
    public Map<String, Object> spendPoints(Integer userId, Integer amount,
                                           String businessType, Integer businessId, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("消费积分数量必须大于0");
        }

        // 获取当前余额
        Integer oldBalance = userMapper.getCoinBalance(userId);
        if (oldBalance == null) {
            throw new RuntimeException("用户不存在");
        }

        if (oldBalance < amount) {
            throw new RuntimeException("积分不足，当前余额：" + oldBalance);
        }

        int newBalance = oldBalance - amount;

        // 原子操作扣除积分（带检查）
        int affected = userMapper.deductCoinBalance(userId, amount);
        if (affected == 0) {
            throw new RuntimeException("扣除积分失败，可能积分不足");
        }

        // 记录积分流水
        UserPoints record = new UserPoints();
        record.setUserId(userId);
        record.setTransactionType("spend");
        record.setAmount(-amount);
        record.setBalanceAfter(newBalance);
        record.setBusinessType(businessType);
        record.setBusinessId(businessId);
        record.setDescription(description);

        userPointsMapper.insert(record);

        log.info("用户 {} 消费积分: {}，业务类型: {}，描述: {}，旧余额: {}，新余额: {}",
                userId, amount, businessType, description, oldBalance, newBalance);

        return Map.of(
                "success", true,
                "userId", userId,
                "amount", amount,
                "oldBalance", oldBalance,
                "newBalance", newBalance,
                "recordId", record.getId(),
                "message", "积分消费成功"
        );
    }

    @Override
    @Transactional
    public Map<String, Object> rewardPoints(Integer userId, Integer amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("奖励积分数量必须大于0");
        }

        // 获取当前余额
        Integer oldBalance = userMapper.getCoinBalance(userId);
        if (oldBalance == null) {
            throw new RuntimeException("用户不存在");
        }

        int newBalance = oldBalance + amount;

        // 原子操作增加积分
        int affected = userMapper.addCoinBalance(userId, amount);
        if (affected == 0) {
            throw new RuntimeException("奖励积分失败");
        }

        // 记录积分流水
        UserPoints record = new UserPoints();
        record.setUserId(userId);
        record.setTransactionType("reward");
        record.setAmount(amount);
        record.setBalanceAfter(newBalance);
        record.setBusinessType("system");
        record.setDescription(description);

        userPointsMapper.insert(record);

        log.info("用户 {} 获得系统奖励积分: {}，描述: {}，旧余额: {}，新余额: {}",
                userId, amount, description, oldBalance, newBalance);

        return Map.of(
                "success", true,
                "userId", userId,
                "amount", amount,
                "oldBalance", oldBalance,
                "newBalance", newBalance,
                "recordId", record.getId(),
                "message", "积分奖励成功"
        );
    }

    @Override
    public boolean hasEnoughPoints(Integer userId, Integer requiredAmount) {
        Integer balance = userMapper.getCoinBalance(userId);
        return balance != null && balance >= requiredAmount;
    }

    @Override
    public Integer getTodayEarnedPoints(Integer userId) {
        return userPointsMapper.sumTodayEarned(userId);
    }

    @Override
    public Map<String, Long> getPointsStatistics(Integer userId) {
        return userPointsMapper.getStatistics(userId);
    }

    // ===== 新增的实用方法，使用之前未用的Mapper =====

    /**
     * 根据交易类型查询流水
     */
    public List<UserPoints> getRecordsByType(Integer userId, String transactionType) {
        return userPointsMapper.findByTransactionType(userId, transactionType);
    }

    /**
     * 根据业务类型查询流水
     */
    public List<UserPoints> getRecordsByBusinessType(Integer userId, String businessType) {
        return userPointsMapper.findByBusinessType(userId, businessType);
    }

    /**
     * 查询时间段内的流水
     */
    public List<UserPoints> getRecordsByTimeRange(Integer userId, LocalDateTime startTime, LocalDateTime endTime) {
        return userPointsMapper.findByTimeRange(userId, startTime, endTime);
    }

    /**
     * 获取最近的积分记录
     */
    public UserPoints getLatestRecord(Integer userId) {
        return userPointsMapper.findLatest(userId);
    }

    /**
     * 获取用户本月积分收入
     */
    public Integer getMonthlyEarned(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.getMonth().length(now.toLocalDate().isLeapYear()))
                .withHour(23).withMinute(59).withSecond(59);

        List<UserPoints> records = userPointsMapper.findByTimeRange(userId, startOfMonth, endOfMonth);

        return records.stream()
                .filter(r -> r.getAmount() > 0)
                .mapToInt(UserPoints::getAmount)
                .sum();
    }

    /**
     * 获取用户本周积分消费
     */
    public Integer getWeeklySpent(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(DayOfWeek.MONDAY)
                .withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfWeek = startOfWeek.plusDays(6)
                .withHour(23).withMinute(59).withSecond(59);

        List<UserPoints> records = userPointsMapper.findByTimeRange(userId, startOfWeek, endOfWeek);

        return records.stream()
                .filter(r -> r.getAmount() < 0)
                .mapToInt(r -> Math.abs(r.getAmount()))
                .sum();
    }
}