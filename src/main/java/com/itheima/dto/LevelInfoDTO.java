package com.itheima.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LevelInfoDTO {
    private Integer level;                  // 当前等级
    private Integer maxLevel;              // 最大等级
    private Long exp;                      // 当前经验值
    private Long currentLevelExp;          // 当前等级所需经验
    private Long nextLevelExp;             // 下一等级所需经验
    private Long remainingExp;             // 还需多少经验升级
    private Double expProgress;            // 经验进度百分比
    private String frameUrl;               // 头像框URL（7级有彩虹头像框）
    private Boolean canLiveStream;         // 是否可以直播
    private Boolean isMaxLevel;            // 是否满级
    private List<String> privileges;       // 等级特权
    private Map<String, Long> upgradeDetails; // 升级详情

    // 可选：升级奖励提示
    private String upgradeHint;
    private String nextLevelHint;
}
