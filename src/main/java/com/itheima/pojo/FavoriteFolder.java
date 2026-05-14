package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteFolder {

    private Integer id;
    private Integer userId;
    private String folderName;
    private String description;
    private String coverImage;
    private Boolean isDefault;
    private Boolean isPrivate;
    private Integer sortOrder;
    private Integer itemCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** COUNT 查询填充的收藏数量，非数据库字段 */
    private Integer favoriteCount;
}
