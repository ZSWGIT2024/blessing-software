// com.itheima.task.ScheduledTask.java
package com.itheima.task;

import com.itheima.mapper.UserDailyUploadMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTask {

    private final UserDailyUploadMapper userDailyUploadMapper;

    private final UserMapper userMapper;

    /**
     * 每天凌晨3点清理100天前的上传记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldUploadRecords() {
        try {
            LocalDate threeMonthsAgo = LocalDate.now().minusDays(100);
            int deletedCount = userDailyUploadMapper.deleteByDateBefore(threeMonthsAgo);
            log.info("已清理 {} 之前的每日上传记录，共清理 {} 条", threeMonthsAgo, deletedCount);
        } catch (Exception e) {
            log.error("清理上传记录失败", e);
        }
    }

    /**
     * 每天凌晨1点检查VIP过期状态（你说的checkExpiredVip）
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void checkExpiredVip() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // 查询所有已过期的VIP用户（除了终身VIP）
            List<User> expiredUsers = userMapper.selectExpiredVipUsers(now);

            if (!expiredUsers.isEmpty()) {
                int updatedCount = 0;

                for (User user : expiredUsers) {
                    // 记录日志
                    log.info("用户 {} 的VIP已过期，原VIP类型: {}, 过期时间: {}",
                            user.getId(), user.getVipType(), user.getVipExpireTime());

                    // 执行VIP降级
                    user.setVipType(0);  // 降级为普通用户
                    user.setUpdateTime(LocalDateTime.now());

                    // 更新用户信息
                    if (userMapper.updateById(user) > 0) {
                        updatedCount++;
                        log.info("用户 {} VIP降级成功，现为普通用户", user.getId());
                    }
                }

                log.info("VIP过期检查完成：共发现 {} 个过期VIP用户，已降级 {} 个",
                        expiredUsers.size(), updatedCount);
            } else {
                log.info("未发现过期VIP用户");
            }

        } catch (Exception e) {
            log.error("检查VIP过期状态失败", e);
            throw new RuntimeException("检查VIP过期状态失败", e);
        }
    }

    /**
     * 每月1号凌晨2点清理超过3个月的登录记录（可选）
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void cleanupOldLoginRecords() {
        try {
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            // 这里假设有登录记录表，根据实际情况实现
            log.info("清理超过3个月的登录记录，时间点: {}", threeMonthsAgo);
        } catch (Exception e) {
            log.error("清理登录记录失败", e);
        }
    }
}