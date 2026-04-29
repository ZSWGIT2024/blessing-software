package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DailyUserStatistics {
    private Long id;
    private LocalDate statDate;
    private Integer totalUsers;
    private Integer newUsers;
    private Integer activeUsers;
    private Integer vipUsers;
    private Integer bannedUsers;
    private Integer totalLogins;
    private Integer uniqueLogins;
    private Integer vipPurchases;
    private Integer vipRenews;
    private Integer vipExpires;
    private Double avgLevel;
    private Double avgCoins;
    private Double userRetentionRate;
    private Double vipConversionRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}