package com.itheima.task;

import com.itheima.mapper.ChatGroupEventMapper;
import com.itheima.mapper.ChatWorldMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupChatCleanupTask {

    private final ChatWorldMessageMapper worldMessageMapper;
    private final ChatGroupEventMapper groupEventMapper;

    /**
     * 每5分钟清理世界聊天消息，仅保留最近100条
     */
    @Scheduled(fixedDelay = 300000)
    public void cleanupWorldMessages() {
        try {
            int count = worldMessageMapper.count();
            if (count > 100) {
                int deleted = worldMessageMapper.deleteExcess(100);
                log.info("清理世界聊天消息完成，共删除 {} 条，保留最近100条", deleted);
            }
        } catch (Exception e) {
            log.error("清理世界聊天消息失败", e);
        }
    }

    /**
     * 每天凌晨3点清理30天前的群事件日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldGroupEvents() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            int deleted = groupEventMapper.deleteOldEvents(thirtyDaysAgo);
            log.info("已清理 {} 之前的群事件日志，共清理 {} 条", thirtyDaysAgo, deleted);
        } catch (Exception e) {
            log.error("清理群事件日志失败", e);
        }
    }

    /**
     * 每天凌晨4点清理过期的系统消息
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupExpiredSystemMessages() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("检查过期系统消息完成，时间点: {}", now);
        } catch (Exception e) {
            log.error("清理过期系统消息失败", e);
        }
    }
}
