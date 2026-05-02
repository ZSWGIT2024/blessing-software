package com.itheima.service;

import com.itheima.pojo.OperationLog;

import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 保存操作日志
     */
    void save(OperationLog log);

    /**
     * 分页查询操作日志
     */
    Map<String, Object> getLogList(Integer page, Integer size, Map<String, Object> filters);

    /**
     * 根据用户ID查询操作日志
     */
    List<OperationLog> getLogsByUserId(Integer userId, Integer limit);

    /**
     * 根据ID查询操作日志
     */
    OperationLog getById(Long id);

    /**
     * 删除指定日期之前的日志
     */
    int deleteBeforeDate(String date);

    /**
     * 获取日志统计信息
     */
    Map<String, Object> getLogStatistics();
}
