package com.itheima.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 媒体文件上传DTO
 */
@Data
public class MediaUploadDTO {

    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    @NotEmpty(message = "文件不能为空")
    private List<MultipartFile> files;

    private String filename;
    private String description;
    private String category;
    private String wall;

    private Integer width;
    private Integer height;
    private Double duration;
    private String resolution;

    private Boolean isPublic = true;
    private String uploadIp;

    public interface Upload {
    }

}