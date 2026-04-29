package com.itheima.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ChatMessageStatusHistory {
    private Long id;

    private String messageId;
    private String status;
    private String reason;
    private Integer operatorId;

    private Date createTime;
}
