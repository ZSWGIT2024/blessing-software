package com.itheima.service;

import java.time.LocalDate;
import java.util.Map;

public interface ExpService {

    /**
     * 增加用户经验
     */
    Map<String, Object> addExperience(Integer userId, String actionType, String description);

    /**
     * 计算应获得的经验值
     */
    Integer calculateExpValue(Integer userId, String actionType);

    /**
     * 检查动作是否可执行（每日上限）
     */
    boolean canExecuteAction(Integer userId, String actionType);

    /**
     * 获取用户今日经验统计
     */
    Map<String, Object> getTodayExpStats(Integer userId);

    /**
     * 获取用户等级信息
     */
    Map<String, Object> getUserLevelInfo(Integer userId);

    /**
     * 处理用户升级
     */
    Map<String, Object> processLevelUp(Integer userId, Integer currentLevel, Long currentExp);
}
