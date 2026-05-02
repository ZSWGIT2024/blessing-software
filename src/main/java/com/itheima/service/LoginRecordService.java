package com.itheima.service;

import com.itheima.pojo.LoginRecord;

import java.util.List;
import java.util.Map;

/**
 * 登录记录服务接口
 */
public interface LoginRecordService {

    /**
     * 保存登录记录
     */
    void save(LoginRecord record);

    /**
     * 更新登出时间
     */
    void updateLogoutTime(Long recordId, Long onlineDuration);

    /**
     * 分页查询登录记录
     */
    Map<String, Object> getRecordList(Integer page, Integer size, Map<String, Object> filters);

    /**
     * 根据用户ID查询登录记录
     */
    List<LoginRecord> getRecordsByUserId(Integer userId, Integer limit);

    /**
     * 获取用户最近一次登录记录
     */
    LoginRecord getLatestRecord(Integer userId);

    /**
     * 获取登录统计信息
     */
    Map<String, Object> getLoginStatistics();

    /**
     * 获取今日登录人数
     */
    Long getTodayLoginCount();

    /**
     * 检测异常登录（多地登录、频繁登录等）
     */
    List<Map<String, Object>> detectAbnormalLogins(Integer userId);
}
