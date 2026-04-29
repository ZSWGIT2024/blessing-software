package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmojiPackItem {
    private Long id;
    private Long packId;
    private String imageUrl;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}