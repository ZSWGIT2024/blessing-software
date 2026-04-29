package com.itheima.dto;

import lombok.Data;

@Data
public class EmojiDTO {
    private String code;      // Emoji代码
    private String name;      // 名称
    private String category;  // 分类
    private String type;      // 类型：emoji/pack
    private Boolean isFavorite;
}
