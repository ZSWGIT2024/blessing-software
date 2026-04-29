// MediaLike.java - 点赞记录实体类
package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MediaLike {
    private Integer id;
    private Integer mediaId;
    private Integer userId;
    private LocalDateTime createTime;
}