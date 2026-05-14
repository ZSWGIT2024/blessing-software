package com.itheima.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegisterResult {
    private Integer id;
    private String username;
    private String phone;
    private Integer userType;
    private String registerIp;
    private String registerLocation;
    private LocalDateTime registerTime;
}
