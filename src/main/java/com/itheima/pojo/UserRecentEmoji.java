package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRecentEmoji {
    private Long id;
    private Integer userId;
    private String emojiCode;
    private String emojiName;
    private Long packItemId;
    private Integer useType; // 1:Emoji 2:表情包图片
    private Integer useCount;
    private LocalDateTime lastUseTime;
    private LocalDateTime createTime;
}
