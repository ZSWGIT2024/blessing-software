package com.itheima.pojo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 群组/世界聊天文件实体类
 */
@Data
public class GroupChatFile {
    private Long id;
    private Integer uploaderId;
    private String groupId;
    private String chatType;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String thumbnailPath;
    private String messageId;
    private Boolean isDeleted;
    private LocalDateTime createTime;
}
