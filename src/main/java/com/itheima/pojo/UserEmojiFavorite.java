package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserEmojiFavorite {
    private Long id;
    private Integer userId;
    private String emojiCode;
    private String emojiName;
    private Long packItemId;
    private Integer favoriteType; // 1:Emoji 2:表情包图片
    private LocalDateTime createTime;
}
