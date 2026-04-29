package com.itheima.dto;

import lombok.Data;
//表情包收藏
@Data
public class FavoriteDTO {
    private Long id;           // 收藏记录ID
    private Integer type;      // 1:Emoji 2:表情包图片
    private String emojiCode;  // Emoji代码（type=1时）
    private Long packItemId;   // 表情包项ID（type=2时）
    private String imageUrl;   // 图片URL（type=2时）
    private String description;// 描述
    private Long favoriteId;   // 收藏ID
}
