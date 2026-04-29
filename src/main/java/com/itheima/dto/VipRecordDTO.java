// VipRecordDTO.java
package com.itheima.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VipRecordDTO {
    private Long id;
    private Integer userId;
    private Integer vipType;
    private String orderNo;
    private Integer actionType;
    private LocalDateTime oldExpireTime;
    private LocalDateTime newExpireTime;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private String createTimeStr;
}