package com.itheima.service.impl;

import com.itheima.mapper.OperationLogMapper;
import com.itheima.pojo.OperationLog;
import com.itheima.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void save(OperationLog operationLog) {
        try {
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    @Override
    public Map<String, Object> getLogList(Integer page, Integer size, Map<String, Object> filters) {
        Map<String, Object> result = new HashMap<>();

        int offset = (page - 1) * size;

        String module = (String) filters.get("module");
        String type = (String) filters.get("type");
        Integer userId = (Integer) filters.get("userId");
        String status = (String) filters.get("status");
        String startTime = (String) filters.get("startTime");
        String endTime = (String) filters.get("endTime");

        Long total = operationLogMapper.countTotal(module, type, userId, status, startTime, endTime);
        List<OperationLog> logs = operationLogMapper.selectPage(offset, size, module, type, userId, status, startTime, endTime);

        int totalPages = (int) Math.ceil((double) total / size);

        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("logs", logs);

        return result;
    }

    @Override
    public List<OperationLog> getLogsByUserId(Integer userId, Integer limit) {
        return operationLogMapper.selectByUserId(userId, limit);
    }

    @Override
    public OperationLog getById(Long id) {
        return operationLogMapper.selectById(id);
    }

    @Override
    public int deleteBeforeDate(String date) {
        return operationLogMapper.deleteBeforeDate(date);
    }

    @Override
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 模块统计
        stats.put("moduleStats", operationLogMapper.selectModuleStats());

        // 类型统计
        stats.put("typeStats", operationLogMapper.selectTypeStats());

        // 每日统计
        stats.put("dailyStats", operationLogMapper.selectDailyStats());

        // 失败统计
        stats.put("failStats", operationLogMapper.selectFailStats());

        return stats;
    }
}
