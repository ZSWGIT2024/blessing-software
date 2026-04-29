package com.itheima.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MediaUpdateDTO {
    @NotNull(message = "用户ID不能为空")
    private Integer userId;
    private String filename;
    private String description;
    private Boolean isPublic;

}
