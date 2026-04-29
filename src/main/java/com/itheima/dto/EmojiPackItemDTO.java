package com.itheima.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class EmojiPackItemDTO {
    private Long id;
    private Long packId;
    private String imageUrl;
    private String description;
    private Boolean isFavorite; // 是否已收藏
    private List<MultipartFile> files;
}

