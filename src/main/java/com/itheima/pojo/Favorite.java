package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Favorite {

    private Integer id;
    private Integer userId;
    private Integer mediaId;
    private Integer folderId;
    private String folderName;
    private String tags;
    private String remark;
    private Boolean isPrivate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // ---- 以下为联表查询时的冗余字段，非数据库列 ----

    private String mediaFilePath;
    private String mediaThumbPath;
    private String mediaFilename;
    private String mediaType;
}
