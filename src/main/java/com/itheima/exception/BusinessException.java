package com.itheima.exception;

// 继承 RuntimeException 以支持 unchecked 异常
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    // 可选：支持异常链
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
