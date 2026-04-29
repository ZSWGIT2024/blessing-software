package com.itheima.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 媒体文件VO - 通用返回格式
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)  // 空值不返回
public class MediaVO {

    // 基础信息
    private Integer id;
    private Integer userId;
    private String mediaType;// "image"、"video"
    private String mediaTypeCN;      // 中文类型："图片"、"视频"

    // 文件信息
    private String filename;
    private String originalName;
    private String filePath;              // 访问URL
    private String thumbnailPath;     // 缩略图URL
    private String fileSize; // 格式化的文件大小："2.5 MB"
    private String mimeType;// 文件类型："image/jpeg"
    private String tags;

    // 媒体属性
    private Integer width;
    private Integer height;
    private String resolution;       // "1920x1080"
    private Double duration;        // 视频时长（秒）
//    private String formattedDuration;// "03:25"

    // 业务信息
    private String description;
    private String category;
    private Boolean isPublic;
    private Integer viewCount;
    private Boolean isLiked;
    private Integer likeCount;
    private String wall;// 墙位置 'left' 或 'right'

    // 时间信息
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private String uploadDate;       // 仅日期，用于分组显示

    private String status;

    // 扩展字段（可选）
    private Boolean liked;           // 当前用户是否点赞
//    private Boolean canEdit;         // 是否有编辑权限
}