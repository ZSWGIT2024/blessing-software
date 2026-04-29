package com.itheima.service;

import com.itheima.dto.LevelInfoDTO;

import java.util.Map;

public interface UserLevelService {

    /**
     * 获取用户等级信息
     */
    LevelInfoDTO getUserLevelInfo(Integer userId);

    /**
     * 获取用户升级详情
     */
    Map<String, Object> getUpgradeDetails(Integer userId);

    /**
     * 检查用户是否可以升级
     */
    Map<String, Object> checkCanUpgrade(Integer userId);

    /**
     * 获取用户等级排行信息
     */
    Map<String, Object> getUserRanking(Integer userId);

    /**
     * 获取等级统计信息
     */
    Map<String, Object> getLevelStatistics(Integer level);

    /**
     * 增加用户经验
     */
    boolean addUserExp(Integer userId, Integer exp, String actionType, String remark);

    /**
     * 批量增加用户经验
     */
    Map<String, Object> batchAddUserExp(Map<Integer, Integer> userExpMap, String actionType, String remark);
}
