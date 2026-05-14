package com.itheima.config;

import com.itheima.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动后自动配置 OSS 防盗链 Referer 白名单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssRefererInitializer {

    private final AliOssUtil aliOssUtil;
    private final OssProperties ossProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void initReferer() {
        try {
            if (ossProperties.getRefererAllowList() != null
                    && !ossProperties.getRefererAllowList().isEmpty()) {
                aliOssUtil.setBucketReferer(
                        ossProperties.getRefererAllowList(),
                        ossProperties.isRefererAllowEmpty());
                log.info("OSS防盗链已初始化");
            }
        } catch (Exception e) {
            log.warn("OSS防盗链初始化失败（非致命）: {}", e.getMessage());
        }
    }
}
