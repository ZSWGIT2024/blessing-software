package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class Notification {
    private Long id;
    private Integer userId;           // 接收用户ID
    private String type;              // 通知类型：like-点赞，comment-评论，follow-关注，friend_apply-好友申请，friend_apply_result-好友申请结果，system-系统
    private String title;             // 通知标题
    private String content;           // 通知内容

    @JsonProperty("data")
    private String data;              // 附加数据（JSON格式）

    private Integer relatedId;        // 关联ID（如作品ID、评论ID）
    private String relatedType;       // 关联类型
    private Integer relatedUserId;    // 关联用户ID
    private Boolean isRead;           // 是否已读
    private Boolean isDeleted;        // 是否删除

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;   // 阅读时间

    // 关联用户信息
    private String messageId; // 新增：消息唯一ID
    private String status; // 新增：消息状态 sending/sent/delivered/read/withdrawn
    private User relatedUser;

    // 添加方法获取扩展数据
    public Map<String, Object> getDataMap() {
        if (data == null || data.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}