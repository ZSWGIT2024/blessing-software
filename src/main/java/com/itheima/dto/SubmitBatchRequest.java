package com.itheima.dto;

import lombok.Data;
import java.util.List;

@Data
public class SubmitBatchRequest {
    private List<SubmitItem> items;
    private String uploadIp;

    @Data
    public static class SubmitItem {
        private String fileUrl;
        private String fileName;        // 用户编辑的标题
        private String originalName;    // 原始文件名（含后缀，如 123.jpg）
        private String mimeType;        // MIME 类型（如 image/jpeg, video/mp4）
        private String description;
        private String category;
        private Boolean isPublic;
        private String mediaType;       // image / video
        private Long fileSize;
        private Integer width;
        private Integer height;
        private Double duration;        // 视频时长（秒）
        private String wall;            // left / right
    }
}
