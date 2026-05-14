package com.itheima.service;

import com.itheima.dto.*;

import java.util.Map;

public interface AuthService {

    RegisterResult register(RegisterRequest request);

    LoginResult login(LoginRequest request, String clientIp, String userAgent);

    LoginResult loginByCode(CodeLoginRequest request, String clientIp, String userAgent);

    void sendVerificationCode(SendCodeRequest request);

    void resetPassword(ResetPasswordRequest request);

    void logout(Integer userId, String token);

    Map<String, Object> refreshToken(String refreshToken);
}
