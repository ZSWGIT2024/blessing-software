package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatFile {
    private Long id;
    private Integer uploaderId;
    private Integer receiverId;
    private String fileName;
    private String filePath;
    private String fileType; // image, video, document, etc.
    private Long fileSize;
    private String thumbnailPath; // 缩略图路径（如果是图片/视频）
    private String messageId; // 关联的消息ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

