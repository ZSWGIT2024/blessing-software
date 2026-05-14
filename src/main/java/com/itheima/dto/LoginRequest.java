package com.itheima.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {
    private String account;

    @Pattern(regexp = "^[a-zA-Z0-9_.]{5,16}$")
    private String password;

    private String captchaUuid;
    private String captchaCode;
}
