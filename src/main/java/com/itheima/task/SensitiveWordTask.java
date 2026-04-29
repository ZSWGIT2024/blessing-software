package com.itheima.task;

import com.itheima.service.impl.SensitiveFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveWordTask {

    private final SensitiveFilterService sensitiveFilterService;

    /**
     * 每小时重新加载一次敏感词库（确保Redis和内存同步）
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void reloadSensitiveWords() {
        try {
            log.info("开始定时重新加载敏感词库...");
            sensitiveFilterService.reloadSensitiveWords();
            log.info("敏感词库重新加载完成");
        } catch (Exception e) {
            log.error("定时重新加载敏感词库失败", e);
        }
    }

    /**
     * 每天凌晨3点清理过期的敏感词缓存
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanSensitiveWordCache() {
        try {
            log.info("开始清理敏感词缓存...");
            // 这里可以添加清理逻辑
            log.info("敏感词缓存清理完成");
        } catch (Exception e) {
            log.error("清理敏感词缓存失败", e);
        }
    }
}