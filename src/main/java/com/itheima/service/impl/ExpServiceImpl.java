package com.itheima.service.impl;

import com.itheima.common.UserConstant;
import com.itheima.mapper.UserExpRecordMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.User;
import com.itheima.pojo.UserExpRecord;
import com.itheima.service.ExpService;
import com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpServiceImpl implements ExpService {

    private final UserMapper userMapper;

    private final UserExpRecordMapper userExpRecordMapper;


    @Override
    @Transactional
    public Map<String, Object> addExperience(Integer userId, String actionType, String description) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return doAddExperience(userId, actionType, description);
            } catch (RuntimeException e) {
                attempt++;
                if (attempt == maxRetries) {
                    log.error("增加经验失败，达到最大重试次数", e);
                    throw e;
                }
                try {
                    Thread.sleep(100 * attempt); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("线程被中断", ie);
                }
            }
        }
        throw new RuntimeException("无法完成经验增加操作");
    }

    private Map<String, Object> doAddExperience(Integer userId, String actionType, String description) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 检查是否可执行
            if (!canExecuteAction(userId, actionType)) {
                result.put("success", false);
                result.put("message", "今日已达上限");
                return result;
            }

            // 2. 获取用户信息
            User user = userMapper.findUserById(userId);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 3. 计算经验值
            Integer expToAdd = calculateExpValue(userId, actionType);

            // 4. 记录经验记录
            UserExpRecord record = new UserExpRecord();
            record.setUserId(userId);
            record.setActionType(actionType);
            record.setExpValue(expToAdd);
            record.setDescription(description);

            userExpRecordMapper.insert(record);

            // 5. 更新用户经验
            Long newExp = user.getExp() + expToAdd;
            Integer currentLevel = user.getLevel();
            Long nextLevelExp = user.getNextLevelExp();
            boolean leveledUp = false;

            // 检查升级
            while (newExp >= nextLevelExp) {
                newExp = newExp - nextLevelExp;
                currentLevel++;
                leveledUp = true;
                nextLevelExp = UserConstant.getNextLevelRequiredExp(currentLevel);
            }

            // 更新用户信息
            userMapper.updateExperience(userId, newExp, currentLevel, nextLevelExp);

            // 6. 返回结果
            result.put("success", true);
            result.put("message", "经验增加成功");
            result.put("userId", userId);
            result.put("actionType", actionType);
            result.put("expAdded", expToAdd);
            result.put("newExp", newExp);
            result.put("newLevel", currentLevel);
            result.put("leveledUp", leveledUp);
            result.put("recordId", record.getId());

            log.info("经验增加成功：用户 {} 执行 {} 获得 {} 经验，新等级: {}", userId, actionType, expToAdd, currentLevel);

        } catch (Exception e) {
            log.error("增加经验失败", e);
            result.put("success", false);
            result.put("message", "增加经验失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Integer calculateExpValue(Integer userId, String actionType) {
        // 获取基础经验值
        Integer baseExp = UserConstant.ExpAction.getBaseExp(actionType);
        if (baseExp == null || baseExp <= 0) {
            return 0;
        }

        // 获取用户信息
        User user = userMapper.findUserById(userId);
        if (user == null) {
            return baseExp;
        }

        // 计算加成
        double multiplier = 1.0;

        // VIP加成
        if (user.isVip()) {
            multiplier *= UserConstant.VIP_EXP_MULTIPLIER;
        }

        // 等级加成（等级越高，加成越多）
        if (user.getLevel() > 1) {
            multiplier *= (1 + (user.getLevel() - 1) * 0.05); // 每级增加5%
        }

        return (int) Math.round(baseExp * multiplier);
    }

    @Override
    public boolean canExecuteAction(Integer userId, String actionType) {
        // 获取每日上限
        Integer dailyLimit = UserConstant.ExpAction.getDailyLimit(actionType);
        if (dailyLimit == 0) {
            return true; // 无限制
        }

        // 统计今日执行次数
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        Integer todayCount = userExpRecordMapper.countByActionToday(userId, actionType, startOfDay, endOfDay);

        return todayCount < dailyLimit;
    }

    @Override
    public Map<String, Object> getTodayExpStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // 今日总经验
        Integer todayExp = userExpRecordMapper.sumExpToday(userId, startOfDay, endOfDay);

        // 今日各动作经验
        List<Object[]> actionExpArray = userExpRecordMapper.groupByActionToday(userId, startOfDay, endOfDay);
        Map<String, Integer> actionExp = new HashMap<>();

        if (actionExpArray != null) {
            for (Object[] row : actionExpArray) {
                String actionType = (String) row[0];
                Long totalExp = (Long) row[1]; // MySQL SUM返回Long
                actionExp.put(actionType, totalExp != null ? totalExp.intValue() : 0);
            }
        }

        stats.put("date", today.toString());
        stats.put("totalExp", todayExp != null ? todayExp : 0);
        stats.put("actionExp", actionExp);

        // 计算剩余可执行次数
        Map<String, Map<String, Object>> actionLimits = new HashMap<>();
        Map<String, String> allActions = UserConstant.ExpAction.getAllActions();

        for (Map.Entry<String, String> entry : allActions.entrySet()) {
            String actionType = entry.getKey();
            String actionName = entry.getValue();

            Integer dailyLimit = UserConstant.ExpAction.getDailyLimit(actionType);
            if (dailyLimit > 0) {
                Integer todayCount = userExpRecordMapper.countByActionToday(userId, actionType, startOfDay, endOfDay);
                Integer remaining = Math.max(0, dailyLimit - (todayCount != null ? todayCount : 0));

                Map<String, Object> limitInfo = new HashMap<>();
                limitInfo.put("actionName", actionName);
                limitInfo.put("dailyLimit", dailyLimit);
                limitInfo.put("todayCount", todayCount != null ? todayCount : 0);
                limitInfo.put("remaining", remaining);

                actionLimits.put(actionType, limitInfo);
            }
        }

        stats.put("actionLimits", actionLimits);

        return stats;
    }

    @Override
    public Map<String, Object> getUserLevelInfo(Integer userId) {
        Map<String, Object> info = new HashMap<>();

        User user = userMapper.findUserById(userId);
        if (user == null) {
            info.put("exists", false);
            return info;
        }

        info.put("exists", true);
        info.put("userId", userId);
        info.put("level", user.getLevel());
        info.put("exp", user.getExp());
        info.put("nextLevelExp", user.getNextLevelExp());

        // 计算进度
        long currentExp = user.getExp();
        long requiredExp = user.getNextLevelExp();
        double progress = requiredExp > 0 ? (currentExp * 100.0 / requiredExp) : 0;

        info.put("progress", Math.round(progress * 100) / 100.0);
        info.put("needExp", Math.max(0, requiredExp - currentExp));

        return info;
    }

    @Override
    @Transactional
    public Map<String, Object> processLevelUp(Integer userId, Integer currentLevel, Long currentExp) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 计算下一级所需经验
            Long nextLevelExp = UserConstant.getNextLevelRequiredExp(currentLevel);

            // 检查是否需要升级
            boolean leveledUp = false;
            Integer newLevel = currentLevel;

            while (currentExp >= nextLevelExp) {
                currentExp = currentExp - nextLevelExp;
                newLevel++;
                leveledUp = true;
                nextLevelExp = UserConstant.getNextLevelRequiredExp(newLevel);
            }

            if (leveledUp) {
                // 更新用户等级和经验
                userMapper.updateExperience(userId, currentExp, newLevel, nextLevelExp);

                result.put("success", true);
                result.put("leveledUp", true);
                result.put("oldLevel", currentLevel);
                result.put("newLevel", newLevel);
                result.put("newExp", currentExp);
                result.put("nextLevelExp", nextLevelExp);
                result.put("message", "恭喜升级到 " + newLevel + " 级！");

                log.info("用户 {} 从 {} 级升级到 {} 级", userId, currentLevel, newLevel);
            } else {
                result.put("success", false);
                result.put("leveledUp", false);
                result.put("message", "经验不足，无法升级");
            }

        } catch (Exception e) {
            log.error("处理升级失败", e);
            result.put("success", false);
            result.put("message", "处理升级失败: " + e.getMessage());
        }

        return result;
    }
}