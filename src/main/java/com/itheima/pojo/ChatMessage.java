package com.itheima.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Data
public class ChatMessage {
    private Long id;

    private String messageId;      // 消息唯一ID
    private Integer senderId;      // 发送者ID
    private Integer receiverId;    // 接收者ID
    @Pattern(regexp = "^friend|stranger$")
    private String chatType;       // 聊天类型：friend/stranger
    @Pattern(regexp = "^text|image|video|file|audio|system$")
    private String contentType;    // 消息类型：text/image/video/file/audio/system
    private String content;        // 消息内容
    private String originalContent;// 原始内容

    @Pattern(regexp = "^sending|sent|failed|read|withdrawn$")
    private String status;         // 消息状态：sending/sent/failed/read/withdrawn
    private Boolean isDeleted;     // 是否删除
    private Boolean isRead;        // 是否已读
    private LocalDateTime readTime;         // 阅读时间
    private LocalDateTime deliveredTime;    // 送达时间
    private LocalDateTime withdrawTime;     // 撤回时间
    private String withdrawReason; // 撤回原因

    private Boolean isFiltered;    // 是否被过滤
    private String filteredWords;  // 过滤的敏感词

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Long fileId; // 关联的文件ID

    // 如果有其他扩展数据（如地理位置、引用消息等）
    private String extraData; // JSON格式，只存储非文件类的扩展数据

    // 解析后的扩展数据
    private Map<String, Object> extraDataMap;

    // 关联的文件信息
    private ChatFile chatFile;

    // 解析 extraData 的方法
    public Map<String, Object> getExtraDataMap() {
        if (extraDataMap == null && extraData != null && !extraData.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                extraDataMap = mapper.readValue(extraData, Map.class);
            } catch (Exception e) {
                extraDataMap = new HashMap<>();
            }
        }
        return extraDataMap;
    }
}
