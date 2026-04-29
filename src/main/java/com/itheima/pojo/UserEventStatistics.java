package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserEventStatistics {
    private Long id;
    private Integer userId;
    private String eventType;  // register/login/vip_purchase/vip_renew/vip_expire/ban/unban/coin_change/level_up
    private LocalDateTime eventTime;
    private String eventData;  // JSON格式
    private String ipAddress;
    private String location;
    private String deviceInfo;
    private LocalDateTime createdAt;
}