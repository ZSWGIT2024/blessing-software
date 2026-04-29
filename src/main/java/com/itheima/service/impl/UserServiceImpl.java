package com.itheima.service.impl;

import com.itheima.common.CacheConstant;
import com.itheima.dto.UserSimpleDTO;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.ExpService;
import com.itheima.service.UserService;
import com.itheima.utils.Md5Util;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final ExpService expService;

    @Override
    public User findByPhone(String phone) {
        return userMapper.findByPhone(phone);
    }

    // 需要修改这个方法，创建完整的User对象
    @Override
    public User saveUser(String username, String phone, String password, String clientIp, String registerLocation, String registerSource) {
        User user = new User();
        String defaultAvataUrl = "https://web-aliyun-zsw.oss-cn-shanghai.aliyuncs.com/%E6%83%A0%E5%A4%A9%E4%B8%8BLOGO.png";
        user.setUsername(username);
        user.setPhone(phone);
        user.setNickname(username); // 初始昵称同用户名
        user.setAvatar(defaultAvataUrl);
        user.setEmail(phone + "@temp.com"); // 临时邮箱
        user.setPassword(Md5Util.md5Encrypt(password));
        user.setUserType(0); // 普通用户
        user.setStatus("active"); // 正常状态
        user.setLevel(1);
        user.setExp(0L);
        user.setNextLevelExp(500L);
        user.setVipType(0); // 非VIP
        user.setRegisterIp(clientIp);
        user.setRegisterLocation(registerLocation);
        user.setRegisterSource(registerSource); // 默认注册来源

        userMapper.addUser(user);
        return user;
    }

    /**
     * 更新登录信息
     */
    public void updateLoginInfo(User user) {
        try {
            int result = userMapper.updateLoginInfo(user);
            if (result > 0) {
                log.info("更新用户登录信息成功: userId={}", user.getId());
            }
        } catch (Exception e) {
            log.error("更新用户登录信息失败: userId={}", user.getId(), e);
        }
    }

    @Override
    @Transactional
    public void updateUser(UserSimpleDTO user) {
        userMapper.updateUser(user);
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        userMapper.updateAvatar(id, avatarUrl);
    }

    @Override
    public void updatePwd(String newPwd) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        String newPwdX = Md5Util.md5Encrypt(newPwd);
        userMapper.updatePwd(id, newPwdX);
    }

    @Override
    public void updateUsername(String username) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer id = (Integer) claims.get("id");
        userMapper.updateUsername(id, username);
    }

    @Override
    public User findUserById(Integer id) {
        return userMapper.findUserById(id);
    }

    // ===== 新增的管理员方法实现 =====

    @Override
    public boolean updateUserStatus(Integer id, String status) {
        try {
            int result = userMapper.updateStatus(id, status);
            return result > 0;
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return false;
        }
    }

    @Override
    public boolean updateUserVip(Integer id, Integer vipType, LocalDateTime expireTime) {
        try {
            int result;

            if (expireTime != null) {
                // 更新VIP类型和过期时间
                result = userMapper.updateVipInfo(id, vipType, expireTime);
            } else {
                // 只更新VIP类型（取消VIP）
                result = userMapper.updateVipType(id, vipType, LocalDateTime.now());
            }

            return result > 0;
        } catch (Exception e) {
            log.error("更新用户VIP失败", e);
            return false;
        }
    }

    @Override
    public boolean updateCoinBalance(Integer id, Integer coinBalance) {
        try {
            int result = userMapper.updateCoinBalance(id, coinBalance);
            return result > 0;
        } catch (Exception e) {
            log.error("更新用户积分失败", e);
            return false;
        }
    }


    @Override
    public boolean updateUserLevel(Integer id, Integer level, Long exp, Long nextLevelExp) {
        try {
            int result = userMapper.updateExperience(id, exp, level, nextLevelExp);
            return result > 0;
        } catch (Exception e) {
            log.error("更新用户等级失败", e);
            return false;
        }
    }

    @Override
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 这里先简单实现，后面可以扩展更复杂的搜索
        // 使用分页查询方法，限制返回20条结果
        return userMapper.findPage(0, 20, keyword, null, null, null, null);
    }

    @Override
    public Map<String, Object> getUserList(Integer page, Integer size, Map<String, Object> filters) {
        Map<String, Object> result = new HashMap<>();

        // 计算分页参数
        int offset = (page - 1) * size;

        // 从filters中获取参数
        String keyword = (String) filters.get("keyword");
        Integer vipType = (Integer) filters.get("vipType");
        String status = (String) filters.get("status");
        Integer minLevel = (Integer) filters.get("minLevel");
        Integer maxLevel = (Integer) filters.get("maxLevel");

        // 查询总数
        Long total = userMapper.countUsers(keyword, vipType, status);
        if (total == null) {
            total = 0L;
        }

        // 查询分页数据
        List<User> users = userMapper.findPage(offset, size, keyword, vipType, status, minLevel, maxLevel);

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / size);

        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", totalPages);
        result.put("users", users);

        return result;
    }

    @Override
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 获取基本统计信息
        Map<String, Object> basicStats = userMapper.getUserStats();
        if (basicStats != null) {
            convertStatsToInt(basicStats, stats);
        }

        // VIP类型分布
        Map<String, Object> vipStats = userMapper.getVipTypeStats();
        if (vipStats != null) {
            for (Map.Entry<String, Object> entry : vipStats.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    stats.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                }
            }
        }

        // 活跃用户统计
        Long monthlyActiveUsers = userMapper.countMonthlyActiveUsers();
        stats.put("monthlyActiveUsers", monthlyActiveUsers != null ? monthlyActiveUsers : 0);

        Long todayActiveUsers = userMapper.countTodayActiveUsers();
        stats.put("todayActiveUsers", todayActiveUsers != null ? todayActiveUsers : 0);

        // 用户等级分布
        List<Map<String, Object>> levelDistribution = userMapper.getUserLevelDistribution();
        stats.put("levelDistribution", levelDistribution);

        // 用户增长趋势
        List<Map<String, Object>> growthTrend = userMapper.getUserGrowthTrend();
        stats.put("growthTrend", growthTrend);

        // 上传统计
        Map<String, Object> uploadStats = userMapper.getUserUploadStats();
        if (uploadStats != null) {
            stats.put("totalUploads", getIntValue(uploadStats.get("total_uploads")));
            stats.put("avgUploads", getDoubleValue(uploadStats.get("avg_uploads")));
            stats.put("maxUploads", getIntValue(uploadStats.get("max_uploads")));
        }

        // 积分统计
        Map<String, Object> coinStats = userMapper.getUserCoinStats();
        if (coinStats != null) {
            stats.put("totalCoins", getIntValue(coinStats.get("total_coins")));
            stats.put("avgCoins", getDoubleValue(coinStats.get("avg_coins")));
            stats.put("maxCoins", getIntValue(coinStats.get("max_coins")));
        }

        // 用户行为统计
        Map<String, Object> behaviorStats = userMapper.getUserBehaviorStats();
        if (behaviorStats != null) {
            stats.putAll(behaviorStats);
        }

        return stats;
    }

    /**
     * 转换统计结果为int类型
     */
    private void convertStatsToInt(Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() instanceof Number) {
                target.put(entry.getKey(), ((Number) entry.getValue()).intValue());
            } else {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 获取int值
     */
    private Integer getIntValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * 获取double值
     */
    private Double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * 增加用户经验
     */
    public void addExperience(Integer userId, String actionType, String remark) {
        try {
            // 这里调用expService
            expService.addExperience(userId, actionType, remark);
            log.info("增加用户经验成功: userId={}, action={}", userId, actionType);
        } catch (Exception e) {
            log.error("增加用户经验失败: userId={}, action={}", userId, actionType, e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstant.CACHE_USER,
            key = "T(com.itheima.common.CacheConstant).getUserKey(#userId)")
    public void updateLastActiveTime(Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        userMapper.updateLastActiveTime(userId, now);
    }
}