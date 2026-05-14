package com.itheima.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String newPwd;
    private String rePwd;
    private String code;
    private String token;
}
