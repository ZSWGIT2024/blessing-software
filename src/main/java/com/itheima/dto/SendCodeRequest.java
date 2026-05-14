package com.itheima.dto;

import lombok.Data;

@Data
public class SendCodeRequest {
    private String target;
    private String type;
}
