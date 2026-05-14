package com.itheima.dto;

import lombok.Data;

@Data
public class CodeLoginRequest {
    private String account;
    private String code;
    private String captchaUuid;
    private String captchaCode;
}
