package com.itheima.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    private String account;

    @Pattern(regexp = "^[a-zA-Z0-9]{5,16}$")
    private String password;

    private String code;

    private String registerSource;
}
