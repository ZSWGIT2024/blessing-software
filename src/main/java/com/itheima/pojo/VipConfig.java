package com.itheima.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class VipConfig {
    private Integer id;
    private Integer vipType;          // VIP类型：1-月度，2-季度，3-年度，4-终身
    private String vipName;           // VIP名称
    private BigDecimal originalPrice; // 原价
    private BigDecimal currentPrice;  // 现价
    private Integer durationDays;     // 有效期天数（终身为0）
    private Integer maxDailyUpload;   // 每日最大上传数
    private Boolean adFree;           // 是否去广告
    private String exclusiveBadge;    // 专属徽章
    private Boolean priorityReview;   // 优先审核
    private String iconUrl;           // 图标URL
    private String description;       // 特权描述
    private Map<String, Object> highlightFeatures; // 突出特点（JSON对象）
    private Boolean isActive;         // 是否启用
    private String discountTag;       // 折扣标签：如"限时8折"
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    //额外字段
    private LocalDateTime expireTime;//过期时间
}