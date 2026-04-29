package com.itheima.dto;

import lombok.Data;

@Data
public class RecentEmojiDTO {
    private Long id;
    private Integer type;      // 1:Emoji 2:表情包图片
    private String emojiCode;  // Emoji代码（type=1时）
    private String emojiName;  // Emoji名称（type=1时）
    private Long packItemId;   // 表情包项ID（type=2时）
    private String imageUrl;   // 图片URL（type=2时）
    private String description;// 描述
    private Integer useCount;
    private String lastUseTime;
}
