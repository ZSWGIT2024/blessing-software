// pojo/UserDailyUpload.java
package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserDailyUpload {
    private Integer id;
    private Integer userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate uploadDate;

    private Integer uploadCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}