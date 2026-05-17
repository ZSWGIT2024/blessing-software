package com.itheima.controller;

import com.itheima.annotation.Log;
import com.itheima.annotation.LogType;
import com.itheima.annotation.Monitor;
import com.itheima.dto.*;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.AuthService;
import com.itheima.service.UserService;
import com.itheima.service.impl.AuthServiceImpl;
import com.itheima.service.impl.CaptchaService;
import com.itheima.utils.RequestUtils;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Validated
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserService userService;
    private final CaptchaService captchaService;

    /**
     * 用户注册（支持手机号或邮箱 + 密码 + 验证码）
     */
    @PostMapping(value = "/register", produces = "application/json")
    public Result<Map<String, Object>> register(
            @RequestParam("account") String account,
            @Pattern(regexp = "^[a-zA-Z0-9]{5,16}$")
            @RequestParam("password") String password,
            @RequestParam(required = false, value = "code") String code,
            @RequestParam(required = false, value = "registerSource", defaultValue = "web") String registerSource) {

        RegisterRequest request = new RegisterRequest();
        request.setAccount(account);
        request.setPassword(password);
        request.setCode(code);
        request.setRegisterSource(registerSource);

        try {
            RegisterResult result = authService.register(request);
            Map<String, Object> data = new HashMap<>();
            data.put("id", result.getId());
            data.put("username", result.getUsername());
            data.put("phone", result.getPhone());
            data.put("userType", result.getUserType());
            data.put("registerIp", result.getRegisterIp());
            data.put("registerLocation", result.getRegisterLocation());
            data.put("registerTime", result.getRegisterTime());
            return Result.success(data);
        } catch (AuthServiceImpl.BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户注册失败: account={}", account, e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录（支持手机号/邮箱 + 密码，含图形验证码 + 递增封禁）
     */
    @PostMapping(value = "/login", produces = "application/json")
    @Log(module = "用户认证", operation = "用户登录", type = LogType.LOGIN)
    @Monitor(threshold = 2000)
    public Result<Map<String, Object>> login(
            @RequestParam("account") String account,
            @Pattern(regexp = "^[a-zA-Z0-9_.]{5,16}$")
            @RequestParam("password") String password,
            @RequestParam(required = false, value = "captchaUuid") String captchaUuid,
            @RequestParam(required = false, value = "captchaCode") String captchaCode,
            HttpServletRequest request) {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount(account);
        loginRequest.setPassword(password);
        loginRequest.setCaptchaUuid(captchaUuid);
        loginRequest.setCaptchaCode(captchaCode);

        String clientIp = RequestUtils.getClientIp();
        String userAgent = request.getHeader("User-Agent");

        try {
            LoginResult result = authService.login(loginRequest, clientIp, userAgent);
            return Result.success(toMap(result));
        } catch (AuthServiceImpl.BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("用户登录失败: account={}", account, e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 验证码登录（支持手机号/邮箱 + 验证码，含登录安全限制）
     */
    @PostMapping(value = "/loginByCode", produces = "application/json")
    @Log(module = "用户认证", operation = "验证码登录", type = LogType.LOGIN)
    public Result<Map<String, Object>> loginByCode(
            @RequestParam("account") String account,
            @RequestParam("code") String code,
            @RequestParam(required = false, value = "captchaUuid") String captchaUuid,
            @RequestParam(required = false, value = "captchaCode") String captchaCode,
            HttpServletRequest request) {

        CodeLoginRequest loginRequest = new CodeLoginRequest();
        loginRequest.setAccount(account);
        loginRequest.setCode(code);
        loginRequest.setCaptchaUuid(captchaUuid);
        loginRequest.setCaptchaCode(captchaCode);

        String clientIp = RequestUtils.getClientIp();
        String userAgent = request.getHeader("User-Agent");

        try {
            LoginResult result = authService.loginByCode(loginRequest, clientIp, userAgent);
            return Result.success(toMap(result));
        } catch (AuthServiceImpl.BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("验证码登录失败: account={}", account, e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 获取图形验证码
     */
    /**
     * 检查用户名是否可用（实时校验）
     */
    @GetMapping("/checkUsername")
    public Result<Map<String, Object>> checkUsername(
            @RequestParam("username") String username,
            @RequestParam(value = "excludeUserId", required = false) Integer excludeUserId) {
        Map<String, Object> data = userService.checkUsernameAvailability(username, excludeUserId);
        return Result.success(data);
    }

    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        Map<String, String> data = captchaService.generateCaptcha();
        return Result.success(data);
    }

    /**
     * 发送验证码（支持手机号和邮箱）
     */
    @PostMapping("/sendSMSCode")
    public Result<Void> sendSMSCode(
            @RequestParam(value = "target") String target,
            @RequestParam(value = "type", required = false, defaultValue = "login") String type) {

        SendCodeRequest request = new SendCodeRequest();
        request.setTarget(target);
        request.setType(type);

        try {
            authService.sendVerificationCode(request);
            return Result.success();
        } catch (AuthServiceImpl.BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("发送验证码失败: target={}, type={}", target, type, e);
            return Result.error("验证码发送失败");
        }
    }

    /**
     * 根据id获取用户信息
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable(name = "id") Integer id) {
        User user = userService.findUserById(id);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.error("用户不存在");
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping(value = "/userInfo", produces = "application/json")
    public Result<User> userInfo() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        String phone = (String) claims.get("phone");
        User user = userService.findByPhone(phone);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PutMapping(value = "/update", produces = "application/json")
    public Result<Void> update(@RequestBody @Validated UserSimpleDTO user) {
        userService.updateUser(user);
        return Result.success();
    }

    /**
     * 修改用户名
     */
    @PatchMapping("/updateUsername")
    public Result<Void> updateUsername(@RequestParam(name = "username") String username) {
        userService.updateUsername(username);
        return Result.success();
    }

    /**
     * 修改头像
     */
    @PatchMapping("/updateAvatar")
    public Result<Void> updateAvatar(@RequestParam(name = "avatarUrl") @URL String avatarUrl) {
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    /**
     * 通过验证码重置密码
     */
    @PatchMapping("/updatePwd")
    public Result<Void> updatePwd(@RequestBody Map<String, String> params,
                               @RequestHeader(name = "Authorization") String token) {

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPwd(params.get("new_pwd"));
        request.setRePwd(params.get("re_pwd"));
        request.setCode(params.get("code"));
        request.setToken(token);

        try {
            authService.resetPassword(request);
            return Result.success();
        } catch (AuthServiceImpl.BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return Result.error("重置密码失败，请稍后重试");
        }
    }

    /**
     * 刷新Access Token
     */
    @PostMapping("/refreshToken")
    public Result<Map<String, Object>> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        try {
            Map<String, Object> data = authService.refreshToken(refreshToken);
            return Result.success(data);
        } catch (AuthServiceImpl.BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            return Result.error("刷新Token失败");
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Log(module = "用户认证", operation = "用户登出", type = LogType.LOGOUT)
    public Result<Void> logout(@RequestHeader("Authorization") String token) {

        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = claims != null ? (Integer) claims.get("id") : null;
            authService.logout(userId, token);
            return Result.success();
        } catch (Exception e) {
            log.error("登出失败", e);
            return Result.error("登出失败");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 将LoginResult DTO转换为API响应的Map格式（保持与旧接口完全兼容）
     */
    private Map<String, Object> toMap(LoginResult r) {
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", r.getAccessToken());
        data.put("refreshToken", r.getRefreshToken());
        data.put("tokenType", r.getTokenType());
        data.put("accessTokenExpireIn", r.getAccessTokenExpireIn());
        data.put("userId", r.getUserId());
        data.put("phone", r.getPhone());
        data.put("username", r.getUsername());
        data.put("nickname", r.getNickname());
        data.put("avatar", r.getAvatar());
        data.put("userType", r.getUserType());
        data.put("status", r.getStatus());
        data.put("location", r.getLocation());
        data.put("lastLoginTime", r.getLastLoginTime());
        data.put("lastLoginIp", r.getLastLoginIp());
        data.put("loginDays", r.getLoginDays());
        data.put("totalLoginDays", r.getTotalLoginDays());
        data.put("level", r.getLevel());
        data.put("exp", r.getExp());
        data.put("isOnline", r.getIsOnline());

        if (r.getVipType() != null) {
            data.put("vipType", r.getVipType());
            data.put("vipExpireTime", r.getVipExpireTime());
            data.put("vipRemainingDays", r.getVipRemainingDays());
        }

        return data;
    }
}
