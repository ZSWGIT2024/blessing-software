package com.itheima.service.impl;

import com.itheima.mapper.LoginRecordMapper;
import com.itheima.pojo.LoginRecord;
import com.itheima.service.LoginRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRecordServiceImpl implements LoginRecordService {

    private final LoginRecordMapper loginRecordMapper;

    @Override
    public void save(LoginRecord record) {
        try {
            loginRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存登录记录失败", e);
        }
    }

    @Override
    public void updateLogoutTime(Long recordId, Long onlineDuration) {
        try {
            String logoutTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            loginRecordMapper.updateLogoutTime(recordId, logoutTime, onlineDuration);
        } catch (Exception e) {
            log.error("更新登出时间失败", e);
        }
    }

    @Override
    public Map<String, Object> getRecordList(Integer page, Integer size, Map<String, Object> filters) {
        Map<String, Object> result = new HashMap<>();

        int offset = (page - 1) * size;

        Integer userId = filters.get("userId") != null ? Integer.parseInt(filters.get("userId").toString()) : null;
        String loginType = (String) filters.get("loginType");
        String status = (String) filters.get("status");
        String startTime = (String) filters.get("startTime");
        String endTime = (String) filters.get("endTime");

        Long total = loginRecordMapper.countTotal(userId, loginType, status, startTime, endTime);
        List<LoginRecord> records = loginRecordMapper.selectPage(offset, size, userId, loginType, status, startTime, endTime);

        int totalPages = (int) Math.ceil((double) total / size);

        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("records", records);

        return result;
    }

    @Override
    public List<LoginRecord> getRecordsByUserId(Integer userId, Integer limit) {
        return loginRecordMapper.selectByUserId(userId, limit);
    }

    @Override
    public LoginRecord getLatestRecord(Integer userId) {
        return loginRecordMapper.selectLatestByUserId(userId);
    }

    @Override
    public Map<String, Object> getLoginStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("weekStats", loginRecordMapper.selectLoginStats());
        stats.put("dailyStats", loginRecordMapper.selectDailyStats());
        stats.put("locationStats", loginRecordMapper.selectLocationStats());
        stats.put("todayLoginCount", loginRecordMapper.countTodayLogin());
        stats.put("abnormalLogins", loginRecordMapper.selectMultiIpLogins());

        return stats;
    }

    @Override
    public Long getTodayLoginCount() {
        return loginRecordMapper.countTodayLogin();
    }

    @Override
    public List<Map<String, Object>> detectAbnormalLogins(Integer userId) {
        // 获取最近IP和异常登录信息
        List<Map<String, Object>> recentIps = loginRecordMapper.selectRecentIps(userId, 10);
        List<Map<String, Object>> multiIps = loginRecordMapper.selectMultiIpLogins();

        Map<String, Object> result = new HashMap<>();
        result.put("recentIps", recentIps);
        result.put("multiIpLogins", multiIps);

        return List.of(result);
    }
}
