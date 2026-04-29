package com.itheima.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatFileDTO {
    private Long id;
    private Integer uploaderId;
    private String uploaderUsername;
    private String uploaderNickname;
    private String uploaderAvatar;
    private String uploaderAvatarFrame;
    private String uploaderRemark;
    private Integer receiverId;
    private String fileName;
    private String fileUrl;
    private String filePath;
    private String fileType;
    private String fileSize;
    private String thumbnailUrl;
    private String thumbnailPath;
    private String messageId;
    private LocalDateTime createTime;
}