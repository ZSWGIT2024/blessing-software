package com.itheima.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 媒体文件查询DTO
 */
@Data
public class MediaQueryDTO {

    private Integer userId;

    private String mediaType;  // "image" 或 "video"

    private String category;   // 分类

    private String tags;      // 标签

    private String status;  // 状态

    private Boolean isPublic;

    private String keyword;    // 关键词搜索（文件名、描述）

    // 分页参数
    private Integer pageNum = 1;

    private Integer pageSize = 20;

    // 排序
    private String orderBy = "upload_time";  // upload_time, view_count, like_count
    private String order = "DESC";           // ASC, DESC

    // 日期范围
    private String startDate;
    private String endDate;
}