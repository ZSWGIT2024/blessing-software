package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPoints {
    private Integer id;
    private Integer userId;
    private String transactionType;  // earn-赚取，spend-消费，reward-奖励
    private Integer amount;          // 正数为收入，负数为支出
    private Integer balanceAfter;    // 交易后余额
    private String businessType;     // upload-上传，invite-邀请，exchange-兑换
    private Integer businessId;      // 业务ID
    private String description;
    private Map<String, Object> extraData;  // JSON格式数据

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}