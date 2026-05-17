package com.itheima.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Data
public class ChatMessageDTO {
    private Long id;
    private String messageId;
    private Integer senderId;
    private Integer receiverId;
    private String chatType; // friend/stranger
    private String contentType; // text/image/video/file/audio/system
    private String content;
    private String status; // sending/sent/failed/read/withdrawn
    private Boolean isRead;
    private Boolean offline; // 标记是否为离线消息
    private LocalDateTime createTime;
    private Long fileId; // 关联的文件ID

    // 扩展字段
    private Boolean isSent; // 是否是当前用户发送的
    private Map<String, Object> extraData; // 扩展数据
    private ChatFileDTO fileInfo; // 文件信息
    private UserSimpleDTO receiverInfo; // 接收者信息
}
