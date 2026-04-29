package com.itheima.task;

import com.itheima.service.FriendApplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendApplyCleanupTask {

    private final FriendApplyService friendApplyService;

    /**
     * 每天凌晨3点清理过期申请
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredApplies() {
        log.info("开始清理过期好友申请...");
        try {
            int count = friendApplyService.cleanupExpiredApplies();
            log.info("成功清理 {} 个过期好友申请", count);
        } catch (Exception e) {
            log.error("清理过期好友申请失败", e);
        }
    }
}