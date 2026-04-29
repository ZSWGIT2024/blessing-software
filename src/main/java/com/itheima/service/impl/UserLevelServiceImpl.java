package com.itheima.service.impl;

import com.itheima.common.UserConstant;
import com.itheima.mapper.UserLevelMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.User;
import com.itheima.dto.LevelInfoDTO;
import com.itheima.service.UserLevelService;
import com.itheima.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLevelServiceImpl implements UserLevelService {

    private final UserMapper userMapper;

    private final UserLevelMapper userLevelMapper;

    private final UserService userService;

    @Override
    public LevelInfoDTO getUserLevelInfo(Integer userId) {
        try {
            User user = userMapper.findUserById(userId);
            if (user == null) {
                return null;
            }

            // 获取基础等级信息
            Map<String, Object> levelInfo = UserConstant.getLevelInfo(user.getExp());

            // 转换为DTO
            LevelInfoDTO dto = new LevelInfoDTO();
            dto.setLevel(user.getLevel());
            dto.setMaxLevel(UserConstant.MAX_LEVEL);
            dto.setExp(user.getExp());
            dto.setCurrentLevelExp((Long) levelInfo.get("currentLevelExp"));
            dto.setNextLevelExp((Long) levelInfo.get("nextLevelExp"));
            dto.setRemainingExp((Long) levelInfo.get("remainingExp"));
            dto.setExpProgress((Double) levelInfo.get("expProgress"));
            dto.setFrameUrl((String) levelInfo.get("frameUrl"));
            dto.setCanLiveStream((Boolean) levelInfo.get("canLiveStream"));
            dto.setIsMaxLevel((Boolean) levelInfo.get("isMaxLevel"));
            dto.setPrivileges(Arrays.asList((String[]) levelInfo.get("privileges")));
            dto.setUpgradeDetails(UserConstant.getUpgradeDetails(user.getExp(), user.getLevel()));

            // 设置升级提示
            if (!dto.getIsMaxLevel()) {
                dto.setUpgradeHint(String.format("还需%d经验升级到%d级",
                        dto.getRemainingExp(), user.getLevel() + 1));
                dto.setNextLevelHint("升级后可获得：" + String.join("、",
                        UserConstant.getLevelPrivileges(user.getLevel() + 1)));
            } else {
                dto.setUpgradeHint("已达到最高等级！");
                dto.setNextLevelHint("享受所有特权，继续积累经验保持活跃！");
            }

            return dto;

        } catch (Exception e) {
            log.error("获取用户等级信息失败: userId={}", userId, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getUpgradeDetails(Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            User user = userMapper.findUserById(userId);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 获取升级详情
            Map<String, Long> details = UserConstant.getUpgradeDetails(user.getExp(), user.getLevel());
            double progress = UserConstant.getExpProgress(user.getExp(), user.getLevel());

            result.put("success", true);
            result.put("level", user.getLevel());
            result.put("exp", user.getExp());
            result.put("progress", progress);
            result.put("currentLevelExp", details.get("current"));
            result.put("nextLevelExp", details.get("next"));
            result.put("needExp", details.get("need"));

            // 计算按当前活跃度升级所需天数
            if (details.get("need") > 0) {
                int dailyExp = calculateDailyExp(user);
                if (dailyExp > 0) {
                    long days = details.get("need") / dailyExp;
                    result.put("estimatedDays", days);
                    result.put("dailyExp", dailyExp);
                }
            }

        } catch (Exception e) {
            log.error("获取升级详情失败: userId={}", userId, e);
            result.put("success", false);
            result.put("message", "获取升级详情失败");
        }

        return result;
    }

    /**
     * 计算用户每日平均经验获取
     */
    private int calculateDailyExp(User user) {
        // 这里可以根据用户的历史行为估算每日经验获取
        // 简化版：假设用户每天能获取的基础经验
        int baseDailyExp = 50;

        // VIP加成
        if (user.isVip()) {
            baseDailyExp *= UserConstant.VIP_EXP_MULTIPLIER;
        }

        return baseDailyExp;
    }

    @Override
    public Map<String, Object> checkCanUpgrade(Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            User user = userMapper.findUserById(userId);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            int currentLevel = user.getLevel();
            long currentExp = user.getExp();

            if (currentLevel >= UserConstant.MAX_LEVEL) {
                result.put("success", true);
                result.put("canUpgrade", false);
                result.put("reason", "已达到最高等级");
                result.put("currentLevel", currentLevel);
                result.put("isMaxLevel", true);
                return result;
            }

            long nextLevelExp = UserConstant.getRequiredExp(currentLevel + 1);
            boolean canUpgrade = currentExp >= nextLevelExp;

            result.put("success", true);
            result.put("canUpgrade", canUpgrade);
            result.put("currentLevel", currentLevel);
            result.put("currentExp", currentExp);
            result.put("nextLevelExp", nextLevelExp);
            result.put("remainingExp", Math.max(0, nextLevelExp - currentExp));

            if (canUpgrade) {
                result.put("message", "可以升级到 " + (currentLevel + 1) + " 级");
                result.put("newPrivileges", UserConstant.getLevelPrivileges(currentLevel + 1));
            } else {
                result.put("message", "经验不足，还需努力");
            }

        } catch (Exception e) {
            log.error("检查升级状态失败: userId={}", userId, e);
            result.put("success", false);
            result.put("message", "检查升级状态失败");
        }

        return result;
    }

    @Override
    public Map<String, Object> getUserRanking(Integer userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            User user = userMapper.findUserById(userId);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // TODO: 这里需要实现实际的排名查询
            // 简化版：返回模拟数据

            int totalUsers = 10000; // 假设总用户数
            int rank = 1500; // 假设排名

            double topPercent = (double) rank / totalUsers * 100;
            String rankLevel = "青铜";
            if (topPercent <= 1) rankLevel = "王者";
            else if (topPercent <= 5) rankLevel = "钻石";
            else if (topPercent <= 15) rankLevel = "黄金";
            else if (topPercent <= 30) rankLevel = "白银";

            result.put("success", true);
            result.put("rank", rank);
            result.put("totalUsers", totalUsers);
            result.put("topPercent", String.format("%.2f", topPercent) + "%");
            result.put("rankLevel", rankLevel);
            result.put("level", user.getLevel());
            result.put("exp", user.getExp());

        } catch (Exception e) {
            log.error("获取用户排行失败: userId={}", userId, e);
            result.put("success", false);
            result.put("message", "获取排行信息失败");
        }

        return result;
    }

    @Override
    public Map<String, Object> getLevelStatistics(Integer level) {
        Map<String, Object> result = new HashMap<>();

        try {
            // TODO: 这里需要查询数据库统计
            // 简化版：返回模拟数据

            result.put("success", true);
            result.put("totalUsers", 10000);
            result.put("levelDistribution", new HashMap<Integer, Integer>() {{
                put(1, 3500);
                put(2, 2500);
                put(3, 1500);
                put(4, 1000);
                put(5, 800);
                put(6, 500);
                put(7, 200);
            }});
            result.put("avgExp", 8500);
            result.put("topLevelUserCount", 200);
            result.put("recentUpgradeCount", 150); // 最近7天升级人数

        } catch (Exception e) {
            log.error("获取等级统计失败", e);
            result.put("success", false);
            result.put("message", "获取统计信息失败");
        }

        return result;
    }

    @Override
    @Transactional
    public boolean addUserExp(Integer userId, Integer exp, String actionType, String remark) {
        try {
            if (exp <= 0) {
                return false;
            }

            // 获取用户
            User user = userMapper.findUserById(userId);
            if (user == null) {
                return false;
            }

            // VIP经验加成
            int finalExp = exp;
            if (user.isVip()) {
                finalExp = (int) (exp * UserConstant.VIP_EXP_MULTIPLIER);
            }

            // 更新用户经验
            long newExp = user.getExp() + finalExp;
            user.setExp(newExp);

            // 重新计算等级
            int newLevel = UserConstant.calculateLevel(newExp);

            // 如果等级有变化，更新等级
            boolean levelUp = newLevel > user.getLevel();
            if (levelUp) {
                user.setLevel(newLevel);
            }

            // 更新数据库
            int updateResult = userLevelMapper.updateExpAndLevel(userId, newExp, newLevel);
            if (updateResult <= 0) {
                return false;
            }

            // 记录经验日志
            log.info("用户经验增加: userId={}, exp={}, finalExp={}, action={}, remark={}, levelUp={}, oldLevel={}, newLevel={}",
                    userId, exp, finalExp, actionType, remark, levelUp, user.getLevel(), newLevel);

            return true;

        } catch (Exception e) {
            log.error("增加用户经验失败: userId={}, exp={}", userId, exp, e);
            return false;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> batchAddUserExp(Map<Integer, Integer> userExpMap, String actionType, String remark) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;

        try {
            for (Map.Entry<Integer, Integer> entry : userExpMap.entrySet()) {
                Integer userId = entry.getKey();
                Integer exp = entry.getValue();

                boolean success = addUserExp(userId, exp, actionType, remark);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            result.put("success", true);
            result.put("total", userExpMap.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("message", String.format("批量增加经验完成，成功：%d，失败：%d", successCount, failCount));

        } catch (Exception e) {
            log.error("批量增加用户经验失败", e);
            result.put("success", false);
            result.put("message", "批量增加经验失败");
            result.put("successCount", successCount);
            result.put("failCount", failCount);
        }

        return result;
    }
}
