package com.itheima.service.impl;

import com.itheima.common.UserConstant;
import com.itheima.dto.*;
import com.itheima.pojo.LoginRecord;
import com.itheima.pojo.User;
import com.itheima.service.AuthService;
import com.itheima.service.LoginRecordService;
import com.itheima.service.UserService;
import com.itheima.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtTokenService jwtTokenService;
    private final LoginSecurityService loginSecurityService;
    private final LoginRecordService loginRecordService;
    private final AsyncTaskService asyncTaskService;
    private final IpLocationUtil ipLocationUtil;
    private final AdvancedIpLocationUtil advancedIpLocationUtil;
    private final CaptchaService captchaService;
    private final SmsService smsService;
    private final EmailService emailService;

    // ==================== 注册 ====================

    @Override
    @Transactional
    public RegisterResult register(RegisterRequest request) {
        String account = normalizeAccount(request.getAccount());
        String password = request.getPassword();
        String code = request.getCode();
        String registerSource = request.getRegisterSource() != null ? request.getRegisterSource() : "web";
        boolean isEmailReg = isEmail(account);

        // 1. 验证验证码
        verifyCode(account, "register", code);

        // 2. 查询用户是否已存在
        if (isEmailReg) {
            if (userService.findByEmail(account) != null) {
                throw new BusinessException("该邮箱已被注册");
            }
        } else {
            if (userService.findByPhone(account) != null) {
                throw new BusinessException("该手机号已被注册");
            }
        }

        // 3. 获取客户端IP及位置
        String clientIp = RequestUtils.getClientIp();
        Map<String, String> locationInfo = advancedIpLocationUtil.getDetailedLocation(clientIp);
        String registerLocation = locationInfo.getOrDefault("city", ipLocationUtil.getLocation(clientIp));
        if (registerLocation == null || registerLocation.isEmpty()) {
            registerLocation = "未知";
        }

        // 4. 生成用户名
        String maskedAccount = isEmailReg
                ? account.substring(0, Math.min(3, account.indexOf("@"))) + "***" + account.substring(account.indexOf("@"))
                : account.substring(0, 3) + "****" + account.substring(7, 11);
        String username = "用户" + maskedAccount;

        // 5. 注册
        String phone = isEmailReg ? account : account;
        User newUser = userService.saveUser(username, phone, password, clientIp, registerLocation, registerSource);
        if (isEmailReg) {
            userService.updateUserEmail(newUser.getId(), account);
        }

        log.info("用户注册成功: userId={}, account={}, registerIp={}", newUser.getId(), maskedAccount, clientIp);

        // 6. 异步非关键操作
        Map<String, Object> registerData = new HashMap<>();
        registerData.put("phone", maskedAccount);
        registerData.put("username", username);
        registerData.put("registerSource", registerSource);
        registerData.put("registerIp", clientIp);
        registerData.put("registerLocation", registerLocation);
        asyncTaskService.recordRegisterEvent(newUser.getId(), registerData);
        asyncTaskService.unlockDefaultFrames(newUser.getId());

        return RegisterResult.builder()
                .id(newUser.getId())
                .username(newUser.getUsername())
                .phone(newUser.getPhone())
                .userType(newUser.getUserType())
                .registerIp(clientIp)
                .registerLocation(registerLocation)
                .registerTime(newUser.getCreateTime())
                .build();
    }

    // ==================== 密码登录 ====================

    @Override
    @Transactional
    public LoginResult login(LoginRequest request, String clientIp, String userAgent) {
        String account = normalizeAccount(request.getAccount());
        String identifier = account;
        String password = request.getPassword();
        String captchaUuid = request.getCaptchaUuid();
        String captchaCode = request.getCaptchaCode();

        String browser = RequestUtils.getBrowser(userAgent);
        String os = RequestUtils.getOS(userAgent);

        // 1. 如需图形验证码，先校验
        if (loginSecurityService.isCaptchaRequired(identifier)) {
            if (captchaUuid == null || captchaCode == null
                    || !captchaService.verify(captchaUuid, captchaCode)) {
                throw new BusinessException("图形验证码错误，请重新输入");
            }
        }

        // 2. 检查登录尝试限制
        LoginSecurityService.LoginCheckResult checkResult =
                loginSecurityService.checkLoginAttempt(identifier, clientIp);
        if (!checkResult.isAllowed()) {
            asyncTaskService.saveLoginRecord(buildLoginRecord(null, null, identifier, "password",
                    clientIp, null, userAgent, browser, os, "fail", checkResult.getMessage()));
            throw new BusinessException(checkResult.getMessage());
        }

        // 3. 查找用户
        User loginUser = isEmail(account) ? userService.findByEmail(account) : userService.findByPhone(account);
        if (loginUser == null) {
            loginSecurityService.recordLoginFailure(identifier, clientIp);
            loginSecurityService.requireCaptcha(identifier);
            asyncTaskService.saveLoginRecord(buildLoginRecord(null, null, identifier, "password",
                    clientIp, null, userAgent, browser, os, "fail", "用户不存在"));
            int nowFailed = loginSecurityService.getFailedAttempts(identifier);
            throw new BusinessException("用户不存在" + getRemainingHint(identifier, nowFailed));
        }

        // 4. 检查用户状态
        if (!UserConstant.STATUS_ACTIVE.equals(loginUser.getStatus())) {
            asyncTaskService.saveLoginRecord(buildLoginRecord(loginUser.getId(), loginUser.getUsername(),
                    identifier, "password", clientIp, null, userAgent, browser, os, "fail", "账号已被封禁"));
            throw new BusinessException("账号已被封禁");
        }

        // 5. 密码验证（含MD5→BCrypt迁移）
        if (!verifyPassword(password, loginUser)) {
            loginSecurityService.recordLoginFailure(identifier, clientIp);
            loginSecurityService.requireCaptcha(identifier);
            asyncTaskService.saveLoginRecord(buildLoginRecord(loginUser.getId(), loginUser.getUsername(),
                    identifier, "password", clientIp, null, userAgent, browser, os, "fail", "密码错误"));
            int nowFailed = loginSecurityService.getFailedAttempts(identifier);
            throw new BusinessException("密码错误" + getRemainingHint(identifier, nowFailed));
        }

        // 6. 登录成功
        loginSecurityService.recordLoginSuccess(identifier, clientIp);
        loginSecurityService.clearCaptchaRequired(identifier);

        String location = getFormattedLocation(clientIp);
        updateLoginInfo(loginUser, clientIp, location);
        loginUser.setIsOnline(UserConstant.ONLINE);

        // 7. 生成双Token
        String accessToken = generateAccessToken(loginUser);
        String refreshToken = jwtTokenService.generateRefreshToken(buildTokenClaims(loginUser));

        // 8. 异步记录
        LoginRecord successRecord = buildLoginRecord(loginUser.getId(), loginUser.getUsername(),
                identifier, "password", clientIp, location, userAgent, browser, os, "success", null);
        asyncTaskService.saveLoginRecord(successRecord);

        log.info("用户登录成功: userId={}, account={}, loginIp={}", loginUser.getId(), identifier, clientIp);

        return buildLoginResult(loginUser, accessToken, refreshToken, clientIp, location);
    }

    // ==================== 验证码登录 ====================

    @Override
    @Transactional
    public LoginResult loginByCode(CodeLoginRequest request, String clientIp, String userAgent) {
        String account = normalizeAccount(request.getAccount());
        String identifier = account;
        String code = request.getCode();
        String captchaUuid = request.getCaptchaUuid();
        String captchaCode = request.getCaptchaCode();

        String browser = RequestUtils.getBrowser(userAgent);
        String os = RequestUtils.getOS(userAgent);

        // 1. 如需图形验证码，先校验
        if (loginSecurityService.isCaptchaRequired(identifier)) {
            if (captchaUuid == null || captchaCode == null
                    || !captchaService.verify(captchaUuid, captchaCode)) {
                throw new BusinessException("图形验证码错误，请重新输入");
            }
        }

        // 2. 检查登录尝试限制
        LoginSecurityService.LoginCheckResult checkResult =
                loginSecurityService.checkLoginAttempt(identifier, clientIp);
        if (!checkResult.isAllowed()) {
            asyncTaskService.saveLoginRecord(buildLoginRecord(null, null, identifier, "code",
                    clientIp, null, userAgent, browser, os, "fail", checkResult.getMessage()));
            throw new BusinessException(checkResult.getMessage());
        }

        // 3. 验证验证码
        try {
            verifyCode(identifier, "login", code);
        } catch (BusinessException e) {
            loginSecurityService.recordLoginFailure(identifier, clientIp);
            loginSecurityService.requireCaptcha(identifier);
            int nowFailed = loginSecurityService.getFailedAttempts(identifier);
            String suffix = code == null ? "验证码已过期" : e.getMessage();
            throw new BusinessException(suffix + getRemainingHint(identifier, nowFailed));
        }

        // 4. 查找用户
        User loginUser = isEmail(account) ? userService.findByEmail(account) : userService.findByPhone(account);
        if (loginUser == null) {
            loginSecurityService.recordLoginFailure(identifier, clientIp);
            loginSecurityService.requireCaptcha(identifier);
            asyncTaskService.saveLoginRecord(buildLoginRecord(null, null, identifier, "code",
                    clientIp, null, userAgent, browser, os, "fail", "用户不存在"));
            int nowFailed = loginSecurityService.getFailedAttempts(identifier);
            throw new BusinessException("用户不存在" + getRemainingHint(identifier, nowFailed));
        }

        // 5. 检查用户状态
        if (!UserConstant.STATUS_ACTIVE.equals(loginUser.getStatus())) {
            throw new BusinessException("账号已被封禁");
        }

        // 6. 获取位置，更新登录信息
        String location = getFormattedLocation(clientIp);
        updateLoginInfo(loginUser, clientIp, location);
        loginUser.setIsOnline(UserConstant.ONLINE);

        // 7. 记录登录成功
        loginSecurityService.recordLoginSuccess(identifier, clientIp);
        loginSecurityService.clearCaptchaRequired(identifier);

        // 8. 生成双Token
        String accessToken = generateAccessToken(loginUser);
        String refreshToken = jwtTokenService.generateRefreshToken(buildTokenClaims(loginUser));

        // 9. 异步记录
        asyncTaskService.saveLoginRecord(buildLoginRecord(loginUser.getId(), loginUser.getUsername(),
                identifier, "code", clientIp, location, userAgent, browser, os, "success", null));

        Map<String, Object> loginData = new HashMap<>();
        loginData.put("phone", loginUser.getPhone() != null ? loginUser.getPhone() : loginUser.getEmail());
        loginData.put("loginIp", clientIp);
        loginData.put("loginLocation", location);
        loginData.put("loginType", "code");
        asyncTaskService.recordLoginEvent(loginUser.getId(), loginData);

        log.info("验证码登录成功: userId={}, account={}, loginIp={}", loginUser.getId(), identifier, clientIp);

        return buildLoginResult(loginUser, accessToken, refreshToken, clientIp, location);
    }

    // ==================== 发送验证码 ====================

    @Override
    public void sendVerificationCode(SendCodeRequest request) {
        String target = request.getTarget();
        String type = request.getType() != null ? request.getType() : "login";

        if (!Arrays.asList("login", "register", "reset").contains(type)) {
            throw new BusinessException("验证码类型错误");
        }

        boolean isEmailTarget = isEmail(target);

        // 1. 频率限制
        String rateLimitKey = "code:rate:" + target + ":" + type;
        Long sendCount = redisTemplate.opsForValue().increment(rateLimitKey);
        if (sendCount == 1) {
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
        }
        if (sendCount > 3) {
            throw new BusinessException("发送过于频繁，请稍后再试");
        }

        // 2. 生成6位数字验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // 3. 存储到Redis
        String cacheKey = "code:" + type + ":" + target;
        int expireMinutes = "register".equals(type) || "reset".equals(type) ? 10 : 5;
        redisTemplate.opsForValue().set(cacheKey, code, expireMinutes, TimeUnit.MINUTES);

        // 4. 发送
        boolean success;
        if (isEmailTarget) {
            success = emailService.send(target, code, type);
        } else {
            success = smsService.send(target, code, type);
        }

        if (!success) {
            throw new BusinessException("验证码发送失败");
        }

        log.info("验证码发送成功: target={}, type={}", target, type);
    }

    // ==================== 重置密码 ====================

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String newPwd = request.getNewPwd();
        String rePwd = request.getRePwd();
        String code = request.getCode();
        String token = request.getToken();

        // 1. 参数校验
        if (newPwd == null || newPwd.isEmpty() || rePwd == null || rePwd.isEmpty()) {
            throw new BusinessException("参数不能为空");
        }
        if (!newPwd.equals(rePwd)) {
            throw new BusinessException("两次密码不一致");
        }

        // 2. 获取当前用户
        Map<String, Object> claims = ThreadLocalUtil.get();
        String phone = (String) claims.get("phone");
        Integer userId = (Integer) claims.get("id");

        // 3. 验证验证码
        verifyCode(phone, "reset", code);

        // 4. 检查新旧密码是否相同
        User user = userService.findByPhone(phone);
        if (BCryptUtil.matches(newPwd, user.getPassword())) {
            throw new BusinessException("新密码不能和旧密码相同");
        }

        // 5. 更新密码
        userService.updatePwd(newPwd);

        // 6. Token失效处理
        jwtTokenService.addToBlacklist(token);
        jwtTokenService.incrementTokenVersion(userId);
        jwtTokenService.revokeRefreshToken(userId);
    }

    // ==================== 登出 ====================

    @Override
    public void logout(Integer userId, String token) {
        jwtTokenService.addToBlacklist(token);

        if (userId != null) {
            updateLoginRecord(userId);
            jwtTokenService.revokeRefreshToken(userId);
        }

        log.info("用户登出成功");
    }

    // ==================== 刷新Token ====================

    @Override
    public Map<String, Object> refreshToken(String refreshToken) {
        String newAccessToken = jwtTokenService.refreshAccessToken(refreshToken);
        if (newAccessToken == null) {
            throw new BusinessException("Refresh Token无效或已过期");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", newAccessToken);
        data.put("tokenType", "Bearer");

        log.info("Access Token刷新成功");
        return data;
    }

    // ==================== 私有辅助方法 ====================

    private String normalizeAccount(String account) {
        if (account == null) return "";
        return account.trim().toLowerCase();
    }

    private boolean isEmail(String input) {
        return input != null && input.contains("@") && input.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
    }

    /**
     * 验证验证码（支持 code 和 sms 两种前缀的key，验证成功后删除）
     */
    private void verifyCode(String target, String type, String code) {
        String cacheKey = "code:" + type + ":" + target;
        String cachedCode = (String) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null) {
            cacheKey = "sms:" + type + ":" + target;
            cachedCode = (String) redisTemplate.opsForValue().get(cacheKey);
        }

        if (cachedCode == null) {
            throw new BusinessException("验证码已过期");
        }

        if (!cachedCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }

        redisTemplate.delete(cacheKey);
    }

    private String getRemainingHint(String identifier, int failedCount) {
        int remaining = LoginSecurityService.MAX_FAIL_COUNT - failedCount;
        if (remaining <= 0) return "，账号已被临时锁定";
        if (remaining <= 3) return "，还剩" + remaining + "次机会";
        return "";
    }

    /**
     * 验证密码（支持BCrypt和MD5→BCrypt迁移）
     */
    private boolean verifyPassword(String rawPassword, User user) {
        String storedPassword = user.getPassword();

        if (BCryptUtil.isBCryptPassword(storedPassword)) {
            return BCryptUtil.matches(rawPassword, storedPassword);
        }

        // MD5兼容：验证后异步迁移到BCrypt
        String md5Hash = Md5Util.md5Encrypt(rawPassword);
        if (md5Hash.equals(storedPassword)) {
            asyncTaskService.migratePassword(user.getId(), rawPassword);
            log.info("密码将从MD5异步迁移到BCrypt：userId={}", user.getId());
            return true;
        }

        return false;
    }

    /**
     * 获取格式化位置信息
     */
    private String getFormattedLocation(String clientIp) {
        try {
            Map<String, String> locationInfo = advancedIpLocationUtil.getDetailedLocation(clientIp);
            String country = locationInfo.getOrDefault("country", "未知");
            String region = locationInfo.getOrDefault("region", "未知");
            String city = locationInfo.getOrDefault("city", "未知");

            if ("中国".equals(country)) {
                if (region.equals(city) || (city != null && city.contains("市辖区"))) {
                    return region;
                }
                return region + "·" + city;
            } else if (!"未知".equals(country)) {
                return country;
            }
        } catch (Exception e) {
            log.debug("获取位置信息失败", e);
        }
        return ipLocationUtil.getLocation(clientIp);
    }

    /**
     * 更新用户登录信息（含连续登录天数计算）
     */
    private void updateLoginInfo(User user, String clientIp, String location) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        user.setLastLoginIp(clientIp);
        user.setLastLoginLocation(location);
        user.setLastLoginTime(now);
        user.setLastActiveTime(now);

        LocalDate lastLoginDate = user.getLastLoginDate();

        if (lastLoginDate == null) {
            user.setLoginDays(1);
        } else {
            LocalDate yesterday = today.minusDays(1);
            if (lastLoginDate.equals(yesterday)) {
                user.setLoginDays(user.getLoginDays() + 1);
            } else if (!lastLoginDate.equals(today)) {
                user.setLoginDays(1);
            }
        }

        user.setLastLoginDate(today);

        if (lastLoginDate == null || !lastLoginDate.equals(today)) {
            user.setTotalLoginDays(user.getTotalLoginDays() + 1);
            asyncTaskService.addDailyLoginExp(user.getId(), user.getLoginDays());
            log.info("用户每日登录奖励已提交: userId={}, loginDays={}, totalLoginDays={}",
                    user.getId(), user.getLoginDays(), user.getTotalLoginDays());
        }

        userService.updateLoginInfo(user);
    }

    /**
     * 生成Access Token并组装claims
     */
    private String generateAccessToken(User user) {
        return jwtTokenService.generateAccessToken(buildTokenClaims(user));
    }

    private Map<String, Object> buildTokenClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("phone", user.getPhone());
        claims.put("username", user.getUsername());
        claims.put("nickname", user.getNickname());
        claims.put("userType", user.getUserType());
        claims.put("status", user.getStatus());
        claims.put("avatar", user.getAvatar());
        Long tokenVersion = jwtTokenService.getTokenVersion(user.getId());
        claims.put("version", tokenVersion.intValue());
        return claims;
    }

    /**
     * 构建登录记录
     */
    private LoginRecord buildLoginRecord(Integer userId, String username, String phone, String loginType,
                                         String loginIp, String loginLocation,
                                         String userAgent, String browser, String os,
                                         String status, String failReason) {
        LoginRecord record = new LoginRecord();
        record.setUserId(userId);
        record.setUsername(username);
        record.setPhone(phone);
        record.setLoginType(loginType);
        record.setLoginIp(loginIp);
        record.setLoginLocation(loginLocation);
        record.setDeviceInfo(userAgent);
        record.setBrowser(browser);
        record.setOs(os);
        record.setStatus(status);
        record.setFailReason(failReason);
        record.setLoginTime(LocalDateTime.now());
        return record;
    }

    /**
     * 登出时更新登录记录
     */
    private void updateLoginRecord(Integer userId) {
        LoginRecord loginRecord = loginRecordService.getLatestRecord(userId);
        log.info("用户登出，更新登录记录: {}", loginRecord);
        if (loginRecord != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime loginTime = loginRecord.getLoginTime();
            Duration duration = Duration.between(loginTime, now);
            long minutes = duration.toMinutes();
            loginRecordService.updateLogoutTime(loginRecord.getId(), minutes);
        }
    }

    /**
     * 构建登录响应
     */
    private LoginResult buildLoginResult(User loginUser, String accessToken, String refreshToken,
                                         String clientIp, String location) {
        LoginResult.LoginResultBuilder builder = LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpireIn(1800)
                .userId(loginUser.getId())
                .phone(loginUser.getPhone().substring(0, 3) + "****" + loginUser.getPhone().substring(7, 11))
                .username(loginUser.getUsername())
                .nickname(loginUser.getNickname())
                .avatar(loginUser.getAvatar())
                .userType(loginUser.getUserType())
                .status(loginUser.getStatus())
                .location(location)
                .lastLoginTime(loginUser.getLastLoginTime())
                .lastLoginIp(clientIp)
                .loginDays(loginUser.getLoginDays())
                .totalLoginDays(loginUser.getTotalLoginDays())
                .level(loginUser.getLevel())
                .exp(loginUser.getExp())
                .isOnline(loginUser.getIsOnline());

        if (loginUser.isVip()) {
            builder.vipType(loginUser.getVipType())
                    .vipExpireTime(loginUser.getVipExpireTime())
                    .vipRemainingDays(loginUser.getVipRemainingDays());
        }

        return builder.build();
    }

    /**
     * 业务异常（Service层内部使用，由Controller统一转换为Result.error）
     */
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}
