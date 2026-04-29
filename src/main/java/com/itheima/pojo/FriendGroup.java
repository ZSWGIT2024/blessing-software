package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FriendGroup {
    private Long id;
    private Integer userId;          // 用户ID
    private String groupName;        // 分组名称
    private Integer sortOrder;       // 排序
    private String color;            // 分组颜色
    private String description;      // 分组描述
    private Integer friendCount;     // 好友数量

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}