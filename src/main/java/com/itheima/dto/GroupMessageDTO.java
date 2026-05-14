package com.itheima.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class GroupMessageDTO {
    private Long id;
    private String messageId;
    private String groupId;
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
    private String contentType;
    private String content;
    private Long fileId;
    private ChatFileDTO fileInfo;
    private Map<String, Object> extraData;
    private String status;
    private Boolean mentionAll;
    private List<Integer> mentionUserIds;
    private Integer readCount;
    private Integer totalMembers;
    private Boolean isRead;
    private LocalDateTime createTime;
}
