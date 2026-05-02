package com.itheima.service;

import com.itheima.common.UserConstant;
import com.itheima.mapper.UserMapper;
import com.itheima.utils.BCryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步任务服务
 * 处理登录流程中的非关键操作（密码迁移、经验奖励、统计记录等），
 * 不阻塞登录响应返回
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final UserMapper userMapper;
    private final ExpService expService;

    /**
     * 异步将MD5密码迁移为BCrypt
     */
    @Async
    public void migratePassword(Integer userId, String rawPassword) {
        try {
            String hashedPassword = BCryptUtil.encode(rawPassword);
            userMapper.updatePwd(userId, hashedPassword);
            log.info("密码已从MD5异步迁移到BCrypt：userId={}", userId);
        } catch (Exception e) {
            log.error("异步密码迁移失败: userId={}", userId, e);
        }
    }

    /**
     * 异步记录每日登录奖励
     */
    @Async
    public void addDailyLoginExp(Integer userId, int loginDays) {
        try {
            expService.addExperience(userId, UserConstant.ExpAction.DAILY_LOGIN,
                    "每日登录奖励，连续登录" + loginDays + "天");
        } catch (Exception e) {
            log.error("异步每日登录奖励失败: userId={}", userId, e);
        }
    }
}
