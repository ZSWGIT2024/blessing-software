package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserExpRecord {
    private Integer id;
    private Integer userId;
    private String actionType;      // 动作类型
    private Integer expValue;       // 获得经验值
    private String description;     // 描述

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
