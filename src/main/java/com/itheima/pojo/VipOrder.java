package com.itheima.pojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VipOrder {
    private Integer id;
    private String orderNo;          // 订单号
    private Integer userId;          // 用户ID
    private Integer vipType;         // VIP类型
    private BigDecimal amount;       // 支付金额
    private String payType;          // 支付方式：alipay/wechat/balance
    private Integer status;          // 订单状态：0-待支付，1-支付成功，2-支付失败，3-已取消
    private LocalDateTime payTime;   // 支付时间
    private LocalDateTime expireTime; // VIP到期时间
    private String transactionId;    // 第三方交易ID
    private String paymentData;      // 支付数据（JSON格式）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String username;
    private String phone;
    private String vipName;
}