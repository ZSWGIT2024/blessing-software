package com.itheima.dto;

import lombok.Data;

@Data
public class EmojiPackDTO {
    private Long id;
    private String packName;
    private String coverUrl;
    private String description;
    private Integer itemCount;
}