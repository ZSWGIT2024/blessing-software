package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmojiPack {
    private Long id;
    private String packName;
    private String coverUrl;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}