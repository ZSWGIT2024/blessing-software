package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserFeedback {
    private Long id;
    private Integer userId;
    private String username;
    private String type;        // suggestion/bug/complaint/other
    private String content;     // max 500 chars
    private String images;      // 截图URL列表，JSON数组字符串: ["url1","url2"]
    private String contact;     // 联系方式
    private String status;      // pending/processing/resolved/closed
    private String adminReply;
    private Integer adminId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;
    private Boolean isDeleted;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
