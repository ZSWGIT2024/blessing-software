package com.itheima.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorldMessageDTO {
    private Long id;
    private String messageId;
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
    private String contentType;
    private String content;
    private String originalContent;
    private String fileUrl;
    private String extraData;
    private String status;
    private Boolean isDeleted;
    private Boolean isFiltered;
    private String filteredWords;
    private LocalDateTime withdrawTime;
    private String withdrawReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
