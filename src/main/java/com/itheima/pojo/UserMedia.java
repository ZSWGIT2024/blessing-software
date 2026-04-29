package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户媒体文件实体类
 */
@Data
public class UserMedia {

    @NotNull(groups = UpdateGroup.class)
    private Integer id;

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotEmpty(message = "媒体类型不能为空")
    @Pattern(regexp = "^(image|video)$", message = "媒体类型必须是image或video")
    private String mediaType;

    @NotEmpty(message = "文件名不能为空")
    private String filename;

    @NotEmpty(message = "原始文件名不能为空")
    private String originalName;

    @NotEmpty(message = "文件路径不能为空")
    private String filePath;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @NotEmpty(message = "文件类型不能为空")
    private String mimeType;

    private Integer width;

    private Integer height;

    private Double duration;// 视频时长（秒）

    private String thumbnailPath;

    private String description;

    @Pattern(regexp = "^.{0,50}$", message = "分类长度不能超过50个字符")
    private String category;

    private String tags;

    private String wall;

    private Boolean isPublic = true;

    private Integer viewCount = 0;

    private Boolean isLiked = false;

    private Integer likeCount = 0;

    private String uploadIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Pattern(regexp = "^(active|pending|hidden)$", message = "状态必须是active、pending或hidden")
    private String status = "pending";// 状态：active、pending、hidden

    // 分组验证接口
    public interface AddGroup {
    }

    public interface UpdateGroup {
    }

    // 枚举类，用于更好的类型安全
    public enum MediaType {
        IMAGE("image"),
        VIDEO("video");

        private final String value;

        MediaType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum MediaStatus {
        ACTIVE("active"),
        PENDING("pending"),
        HIDDEN("hidden");

        private final String value;

        MediaStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // 以下是一些业务逻辑方法（可选）

    /**
     * 获取文件大小（带单位）
     */
    public String getFileSizeWithUnit() {
        if (fileSize == null) return "0 B";

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 是否为图片类型
     */
    public boolean isImage() {
        return MediaType.IMAGE.getValue().equals(mediaType);
    }

    /**
     * 是否为视频类型
     */
    public boolean isVideo() {
        return MediaType.VIDEO.getValue().equals(mediaType);
    }

    /**
     * 获取媒体类型对应的中文名称
     */
    public String getMediaTypeCN() {
        if (isImage()) {
            return "图片";
        } else if (isVideo()) {
            return "视频";
        }
        return "未知";
    }

    /**
     * 获取缩略图路径，如果不存在则返回原始路径
     */
    public String getThumbnailOrDefault() {
        return thumbnailPath != null && !thumbnailPath.isEmpty() ? thumbnailPath : filePath;
    }

    /**
     * 获取分辨率字符串
     */
    public String getResolution() {
        if (width != null && height != null) {
            return width + "×" + height;
        }
        return "未知";
    }

    /**
     * 获取视频时长（格式化）
     */
    public String getFormattedDuration() {
        if (duration == null || duration <= 0) {
            return "00:00";
        }
        int hours = (int)(duration / 3600);
        int minutes = (int)((duration % 3600) / 60);
        int seconds = (int)(duration % 60);

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

}
