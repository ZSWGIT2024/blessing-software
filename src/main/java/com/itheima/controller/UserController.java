package com.itheima.controller;


import com.itheima.annotation.Log;
import com.itheima.annotation.LogType;
import com.itheima.annotation.Monitor;
import com.itheima.common.UserConstant;
import com.itheima.dto.UserSimpleDTO;
import com.itheima.pojo.LoginRecord;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.*;
import com.itheima.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/user")
@Validated
@Slf4j
@RequiredArgsConstructor
public class UserController {


    private final UserService userService;

    private final UserStatisticsService userStatisticsService;

    private final RedisTemplate redisTemplate;

    private final JwtUtil jwtUtil;

    private final JwtTokenService jwtTokenService;

    private final LoginSecurityService loginSecurityService;

    private final LoginRecordService loginRecordService;

    private final AsyncTaskService asyncTaskService;

    private final IpLocationUtil ipLocationUtil;

    private final AdvancedIpLocationUtil advancedIpLocationUtil;

    /**
     * 用户注册
     */
    @PostMapping(value = "/register", produces = "application/json")
    public Result<Map<String, Object>> register(
            @Pattern(regexp = "^1[3-9]\\d{9}$")
            @RequestParam("phone") String phone,
            @Pattern(regexp = "^[a-zA-Z0-9]{5,16}$")
            @RequestParam("password") String password,
            @RequestParam(required = false, value = "code") String code,
            @RequestParam(required = false, value = "registerSource", defaultValue = "web") String registerSource) {

        try {
            // 1. 验证验证码
            String cacheKey = "sms:register:" + phone;
            String cachedCode = (String) redisTemplate.opsForValue().get(cacheKey);

            if (cachedCode == null) {
                return Result.error("验证码已过期");
            }

            if (!cachedCode.equals(code)) {
                return Result.error("验证码错误");
            }

            // 验证成功后删除验证码
            redisTemplate.delete(cacheKey);

            // 查询用户是否存在
            User user = userService.findByPhone(phone);
            if (user != null) {
                return Result.error("用户已存在");
            }

            // 获取客户端IP
            String clientIp = RequestUtils.getClientIp();

            // 使用高级IP定位获取更精确的位置信息
            Map<String, String> locationInfo = advancedIpLocationUtil.getDetailedLocation(clientIp);
            String registerLocation = locationInfo.getOrDefault("city", ipLocationUtil.getLocation(clientIp));

            if (registerLocation == null || registerLocation.isEmpty()) {
                registerLocation = "未知";
            }

            // 生成用户名
            String username = "用户" + phone.substring(0, 3) + "****" + phone.substring(7, 11);

            // 注册用户（包含IP和位置信息）
            User newUser = userService.saveUser(username, phone, password, clientIp, registerLocation, registerSource);

            log.info("用户注册成功: userId={}, phone={}, registerIp={}, location={}",
                    newUser.getId(), phone, clientIp, newUser.getRegisterLocation());

            // 注册成功后记录统计事件
            Map<String, Object> registerData = new HashMap<>();
            registerData.put("phone", phone);
            registerData.put("username", username);
            registerData.put("registerSource", registerSource);
            registerData.put("registerIp", clientIp);
            registerData.put("registerLocation", registerLocation);

            userStatisticsService.recordRegisterEvent(newUser.getId(), registerData);

            log.info("用户注册成功并记录统计: userId={}, phone={}", newUser.getId(), phone);

            // 返回注册成功信息
            Map<String, Object> data = new HashMap<>();
            data.put("id", newUser.getId());
            data.put("username", newUser.getUsername());
            data.put("phone", newUser.getPhone());
            data.put("userType", newUser.getUserType());
            data.put("registerIp", clientIp);
            data.put("registerLocation", registerLocation);
            data.put("registerTime", newUser.getCreateTime());

            return Result.success(data);

        } catch (Exception e) {
            log.error("用户注册失败: phone={}", phone, e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登录（优化版：BCrypt密码验证 + 双Token + 登录安全）
     */
    @PostMapping(value = "/login", produces = "application/json")
    @Log(module = "用户认证", operation = "用户登录", type = LogType.LOGIN)
    @Monitor(threshold = 2000)
    public Result<Map<String, Object>> login(
            @Pattern(regexp = "^1[3-9]\\d{9}$")
            @RequestParam("phone") String phone,
            @Pattern(regexp = "^[a-zA-Z0-9_.]{5,16}$")
            @RequestParam("password") String password,
            HttpServletRequest request) {

        String clientIp = RequestUtils.getClientIp();

        try {
            // 1. 获取设备信息
            String userAgent = request.getHeader("User-Agent");
            String browser = RequestUtils.getBrowser(userAgent);
            String os = RequestUtils.getOS(userAgent);

            // 2. 检查登录尝试限制
            LoginSecurityService.LoginCheckResult checkResult =
                    loginSecurityService.checkLoginAttempt(phone, clientIp);

            if (!checkResult.isAllowed()) {
                // 记录失败登录
                try {
                    LoginRecord failRecord = buildLoginRecord(null, null, phone, "password",
                            clientIp, null, userAgent, browser, os, "fail", checkResult.getMessage());
                    loginRecordService.save(failRecord);
                } catch (Exception ex) {
                    log.debug("记录登录失败忽略", ex);
                }
                return Result.error(checkResult.getMessage());
            }

            // 3. 查找用户
            User loginUser = userService.findByPhone(phone);
            if (loginUser == null) {
                loginSecurityService.recordLoginFailure(phone, clientIp);
                recordFailedLogin(null, null, phone, "password", clientIp, null, userAgent, browser, os, "用户不存在");
                return Result.error("用户不存在");
            }

            // 4. 检查用户状态
            if (!UserConstant.STATUS_ACTIVE.equals(loginUser.getStatus())) {
                recordFailedLogin(loginUser.getId(), loginUser.getUsername(), phone, "password", clientIp,
                        null, userAgent, browser, os, "账号已被封禁");
                return Result.error("账号已被封禁");
            }

            // 5. BCrypt密码验证（含MD5→BCrypt迁移）
            String storedPassword = loginUser.getPassword();
            boolean passwordValid = false;

            if (BCryptUtil.isBCryptPassword(storedPassword)) {
                // 新密码：使用BCrypt验证
                passwordValid = BCryptUtil.matches(password, storedPassword);
            } else {
                // 旧密码：使用MD5验证，成功后自动迁移到BCrypt
                String md5Hash = Md5Util.md5Encrypt(password);
                if (md5Hash.equals(storedPassword)) {
                    passwordValid = true;
                    // 异步迁移密码，不阻塞登录响应
                    asyncTaskService.migratePassword(loginUser.getId(), password);
                    log.info("密码将从MD5异步迁移到BCrypt：userId={}", loginUser.getId());
                }
            }

            if (!passwordValid) {
                loginSecurityService.recordLoginFailure(phone, clientIp);
                recordFailedLogin(loginUser.getId(), loginUser.getUsername(), phone, "password", clientIp,
                        null, userAgent, browser, os, "密码错误");

                int failedAttempts = loginSecurityService.getFailedAttempts(phone);
                int remainingAttempts = 5 - failedAttempts;
                return Result.error("密码错误，还剩" + Math.max(0, remainingAttempts) + "次机会");
            }

            // 6. 记录登录成功，清除失败记录
            loginSecurityService.recordLoginSuccess(phone, clientIp);

            // 7. 获取位置信息
            String location = getFormattedLocation(clientIp);

            // 8. 更新登录信息
            updateLoginInfo(loginUser, clientIp, location);
            loginUser.setIsOnline(UserConstant.ONLINE);

            // 9. 生成双Token（Access + Refresh）
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("phone", loginUser.getPhone());
            claims.put("username", loginUser.getUsername());
            claims.put("nickname", loginUser.getNickname());
            claims.put("userType", loginUser.getUserType());
            claims.put("status", loginUser.getStatus());
            claims.put("avatar", loginUser.getAvatar());

            // 添加Token版本号
            Long tokenVersion = jwtTokenService.getTokenVersion(loginUser.getId());
            claims.put("version", tokenVersion.intValue());

            String accessToken = jwtTokenService.generateAccessToken(claims);
            String refreshToken = jwtTokenService.generateRefreshToken(claims);

            // 10. 记录登录成功
            recordSuccessLogin(loginUser.getId(), loginUser.getUsername(), phone, "password", clientIp,
                    location, userAgent, browser, os);

            // 11. 记录登录事件
            try {
                Map<String, Object> loginData = new HashMap<>();
                loginData.put("phone", phone.substring(0, 3) + "****" + phone.substring(7, 11));
                loginData.put("loginIp", clientIp);
                loginData.put("loginLocation", location);
                loginData.put("loginType", "password");
                userStatisticsService.recordLoginEvent(loginUser.getId(), loginData);
            } catch (Exception e) {
                log.debug("记录登录统计事件失败", e);
            }

            log.info("用户登录成功: userId={}, phone={}, loginIp={}, location={}",
                    loginUser.getId(), phone, clientIp, location);

            // 12. 构建返回数据
            Map<String, Object> data = buildLoginResponseData(loginUser, accessToken,
                    refreshToken, clientIp, location);

            return Result.success(data);

        } catch (Exception e) {
            log.error("用户登录失败: phone={}", phone, e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 验证码登录（优化版：双Token + 登录记录）
     */
    @PostMapping(value = "/loginByCode", produces = "application/json")
    @Log(module = "用户认证", operation = "验证码登录", type = LogType.LOGIN)
    public Result<Map<String, Object>> loginByCode(
            @Pattern(regexp = "^1[3-9]\\d{9}$")
            @RequestParam("phone") String phone,
            @RequestParam("code") String code,
            HttpServletRequest request) {

        try {
            // 1. 验证验证码
            String cacheKey = "sms:login:" + phone;
            String cachedCode = (String) redisTemplate.opsForValue().get(cacheKey);

            if (cachedCode == null) {
                return Result.error("验证码已过期");
            }

            if (!cachedCode.equals(code)) {
                return Result.error("验证码错误");
            }

            // 验证成功后删除验证码
            redisTemplate.delete(cacheKey);

            // 2. 查找用户
            User loginUser = userService.findByPhone(phone);
            if (loginUser == null) {
                return Result.error("用户不存在");
            }

            // 3. 检查用户状态
            if (!UserConstant.STATUS_ACTIVE.equals(loginUser.getStatus())) {
                return Result.error("账号已被封禁");
            }

            // 4. 获取客户端信息
            String clientIp = RequestUtils.getClientIp();
            String userAgent = request.getHeader("User-Agent");
            String browser = RequestUtils.getBrowser(userAgent);
            String os = RequestUtils.getOS(userAgent);
            String location = getFormattedLocation(clientIp);

            // 5. 更新登录信息
            updateLoginInfo(loginUser, clientIp, location);
            loginUser.setIsOnline(UserConstant.ONLINE);

            // 6. 记录登录成功
            loginSecurityService.recordLoginSuccess(phone, clientIp);

            // 7. 生成双Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("phone", loginUser.getPhone());
            claims.put("username", loginUser.getUsername());
            claims.put("nickname", loginUser.getNickname());
            claims.put("userType", loginUser.getUserType());
            claims.put("status", loginUser.getStatus());
            claims.put("avatar", loginUser.getAvatar());

            String accessToken = jwtTokenService.generateAccessToken(claims);
            String refreshToken = jwtTokenService.generateRefreshToken(claims);

            // 8. 记录登录记录
            recordSuccessLogin(loginUser.getId(), loginUser.getUsername(), phone, "code", clientIp, location,
                    userAgent, browser, os);

            // 9. 记录统计事件
            try {
                Map<String, Object> loginData = new HashMap<>();
                loginData.put("phone", phone.substring(0, 3) + "****" + phone.substring(7, 11));
                loginData.put("loginIp", clientIp);
                loginData.put("loginLocation", location);
                loginData.put("loginType", "code");
                userStatisticsService.recordLoginEvent(loginUser.getId(), loginData);
            } catch (Exception e) {
                log.debug("记录登录统计事件失败", e);
            }

            log.info("验证码登录成功: userId={}, phone={}, loginIp={}", loginUser.getId(), phone, clientIp);

            // 10. 构建返回数据
            Map<String, Object> data = buildLoginResponseData(loginUser, accessToken,
                    refreshToken, clientIp, location);

            return Result.success(data);

        } catch (Exception e) {
            log.error("验证码登录失败: phone={}", phone, e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 发送验证码（支持多种类型）
     */
    @PostMapping("/sendSMSCode")
    public Result<Void> sendSMSCode(
            @Pattern(regexp = "^1[3-9]\\d{9}$")
            @RequestParam(value = "phone") String phone,
            @RequestParam(value = "type", required = false, defaultValue = "login") String type) {

        try {
            // 验证type参数
            if (!Arrays.asList("login", "register", "reset").contains(type)) {
                return Result.error("验证码类型错误");
            }

            // 1. 检查发送频率（防止刷验证码）
            String rateLimitKey = "sms:rate:" + phone + ":" + type;
            Long sendCount = redisTemplate.opsForValue().increment(rateLimitKey);
            if (sendCount == 1) {
                redisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
            }
            if (sendCount > 3) {
                return Result.error("发送过于频繁，请稍后再试");
            }

            // 2. 生成验证码（6位数字）
            String code = String.format("%06d", new Random().nextInt(999999));

            // 3. 存储到Redis（不同类型不同有效期）
            String cacheKey = "sms:" + type + ":" + phone;
            int expireMinutes = 5; // 默认5分钟
            if ("register".equals(type)) expireMinutes = 10; // 注册验证码10分钟
            if ("reset".equals(type)) expireMinutes = 10;    // 重置密码10分钟

            redisTemplate.opsForValue().set(cacheKey, code, expireMinutes, TimeUnit.MINUTES);

            // 4. 调用短信服务发送验证码
            boolean success = sendSMS(phone, code, type);

            if (success) {
                log.info("发送验证码成功: phone={}, type={}, code={}", phone, type, code);
                return Result.success();
            } else {
                return Result.error("验证码发送失败");
            }

        } catch (Exception e) {
            log.error("发送验证码失败: phone={}, type={}", phone, type, e);
            return Result.error("验证码发送失败");
        }
    }

    /**
     * 发送短信（实际短信服务调用）
     */
    private boolean sendSMS(String phone, String code, String type) {
        try {
            // 根据type生成不同的短信内容
            String content = "";
            switch (type) {
                case "login":
                    content = String.format("【惠天下】您的登录验证码是：%s，5分钟内有效。请勿泄露给他人。", code);
                    break;
                case "register":
                    content = String.format("【惠天下】您的注册验证码是：%s，10分钟内有效。请勿泄露给他人。", code);
                    break;
                case "reset":
                    content = String.format("【惠天下】您正在重置密码，验证码是：%s，10分钟内有效。", code);
                    break;
            }

            // 这里调用实际的短信服务
            // 例如：阿里云短信、腾讯云短信等
            // return smsClient.send(phone, content);

            // 开发测试阶段，直接打印到控制台
            log.info("发送短信到 {}: {}", phone, content);
            return true;

        } catch (Exception e) {
            log.error("调用短信服务失败", e);
            return false;
        }
    }

    /**
     * 更新用户登录信息（修正版）
     */
    private void updateLoginInfo(User user, String clientIp, String location) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // 设置登录IP和位置
        user.setLastLoginIp(clientIp);
        user.setLastLoginLocation(location);

        // 设置最后登录时间和最后活跃时间
        user.setLastLoginTime(now);
        user.setLastActiveTime(now);

        // 获取上次登录日期
        LocalDate lastLoginDate = null;
        if (user.getLastLoginDate() != null) {
            lastLoginDate = user.getLastLoginDate();
        }

        // 计算连续登录天数
        if (lastLoginDate == null) {
            // 第一次登录
            user.setLoginDays(1);
        } else {
            LocalDate yesterday = today.minusDays(1);

            if (lastLoginDate.equals(yesterday)) {
                // 连续登录（昨天登录过）
                user.setLoginDays(user.getLoginDays() + 1);
            } else if (!lastLoginDate.equals(today)) {
                // 非连续登录（不是昨天也不是今天）
                user.setLoginDays(1);
            }
            // 如果是今天已经登录过，loginDays不变
        }

        // 更新最后登录日期
        user.setLastLoginDate(today);

        // 更新累计登录天数（如果今天还没登录过）
        if (lastLoginDate == null || !lastLoginDate.equals(today)) {
            user.setTotalLoginDays(user.getTotalLoginDays() + 1);

            // 异步给予每日登录经验奖励
            asyncTaskService.addDailyLoginExp(user.getId(), user.getLoginDays());

            log.info("用户每日登录奖励已提交: userId={}, loginDays={}, totalLoginDays={}",
                    user.getId(), user.getLoginDays(), user.getTotalLoginDays());
        }

        // 保存到数据库
        userService.updateLoginInfo(user);
    }

    //根据id获取用户信息
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable(name = "id") Integer id) {
        User user = userService.findUserById(id);
        if (user != null) {
            return Result.success(user);
        } else {
            return Result.error("用户不存在");
        }
    }

    //根据手机号获取用户信息
    @GetMapping(value = "/userInfo", produces = "application/json")
    public Result<User> userInfo(/*@RequestHeader(name = "Authorization") String token*/) {
        //根据token令牌获取用户信息
//         Map<String, Object> claims = JwtUtil.parseToken(token);
//        String username = (String) claims.get("username");


        //根据线程获取用户id，从而获取用户信息
        Map<String, Object> claims = ThreadLocalUtil.get();
        String phone = (String) claims.get("phone");
        User user = userService.findByPhone(phone);
        return Result.success(user);
    }


    //更新用户信息
    @PutMapping(value = "/update", produces = "application/json")
    public Result update(@RequestBody @Validated UserSimpleDTO user) {
        //更新用户信息
        userService.updateUser(user);
        return Result.success();
    }

    //更新用户名信息
    @PatchMapping("/updateUsername")  // 仅修改注解，其他不变
    public Result updateUsername(@RequestParam(name = "username") String username) {
        userService.updateUsername(username);
        return Result.success();
    }

    //更新用户头像
    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam(name = "avatarUrl") @URL String avatarUrl) {
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }


    //根据用户手机号验证码更新用户密码
    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String, String> params,
                            @RequestHeader(name = "Authorization") String token) {
        //1.校验参数
        String newPwd = params.get("new_pwd");
        String rePwd = params.get("re_pwd");
        String code = params.get("code");
        //判断参数是否为空
        if (!StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)) {
            return Result.error("参数不能为空");
        }
        //判断原密码是否正确
        //获取用户信息
        Map<String, Object> claims = ThreadLocalUtil.get();
        String phone = (String) claims.get("phone");
        try {
            // 1. 验证验证码
            String cacheKey = "sms:reset:" + phone;
            String cachedCode = (String) redisTemplate.opsForValue().get(cacheKey);

            if (cachedCode == null) {
                return Result.error("验证码已过期");
            }

            if (!cachedCode.equals(code)) {
                return Result.error("验证码错误");
            }
            // 验证成功后删除验证码
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            return Result.error("验证码错误");
        }

        User user = userService.findByPhone(phone);
        //判断新密码和旧密码是否相同（BCrypt验证）
        if (BCryptUtil.matches(newPwd, user.getPassword())) {
            return Result.error("新密码不能和旧密码相同");
        }
        //判断两次密码是否一致
        if (!newPwd.equals(rePwd)) {
            return Result.error("两次密码不一致");
        }
        //2.调用service完成密码更新
        userService.updatePwd(newPwd);
        //当用户密码更新后，将旧token加入黑名单，并撤销Refresh Token
        Integer userId = (Integer) claims.get("id");
        jwtTokenService.addToBlacklist(token);
        jwtTokenService.incrementTokenVersion(userId);
        jwtTokenService.revokeRefreshToken(userId);
        return Result.success();
    }

    /**
     * 刷新Access Token
     */
    @PostMapping("/refreshToken")
    public Result<Map<String, Object>> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        try {
            // 使用JwtTokenService刷新Access Token
            String newAccessToken = jwtTokenService.refreshAccessToken(refreshToken);
            if (newAccessToken == null) {
                return Result.error("Refresh Token无效或已过期");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", newAccessToken);
            data.put("tokenType", "Bearer");

            log.info("Access Token刷新成功");
            return Result.success(data);

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
            // 将当前token加入黑名单
            jwtTokenService.addToBlacklist(token);

            // 撤销Refresh Token
            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims != null) {
                Integer userId = (Integer) claims.get("id");
                if (userId != null) {
                    jwtTokenService.revokeRefreshToken(userId);

                    // 设置用户离线状态
                    User user = userService.findUserById(userId);
                    if (user != null) {
                        user.setIsOnline(UserConstant.OFFLINE);
                        userService.updateLoginInfo(user);
                    }
                    //记录用户登出记录
                    LoginRecord loginRecord = loginRecordService.getLatestRecord(userId);
                    if (loginRecord != null) {
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime loginTime = loginRecord.getLoginTime();
                        Duration duration = Duration.between(loginTime, now);
                        long minutes = duration.toMinutes();
                        loginRecord.setLogoutTime(now);
                        loginRecord.setOnlineDuration(minutes);
                    }
                }
            }

            log.info("用户登出成功");
            return Result.success();

        } catch (Exception e) {
            log.error("登出失败", e);
            return Result.error("登出失败");
        }
    }

    // ==================== 辅助方法 ====================

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
     * 记录失败登录
     */
    private void recordFailedLogin(Integer userId, String username, String phone, String loginType,
                                    String loginIp, String loginLocation,
                                    String userAgent, String browser, String os,
                                    String failReason) {
        try {
            LoginRecord record = buildLoginRecord(userId, username, phone, loginType, loginIp,
                    loginLocation, userAgent, browser, os, "fail", failReason);
            loginRecordService.save(record);
        } catch (Exception e) {
            log.debug("记录登录失败日志忽略", e);
        }
    }

    /**
     * 记录成功登录
     */
    private void recordSuccessLogin(Integer userId,String username, String phone, String loginType,
                                     String loginIp, String loginLocation,
                                     String userAgent, String browser, String os) {
        try {
            LoginRecord record = buildLoginRecord(userId, username, phone, loginType, loginIp,
                    loginLocation, userAgent, browser, os, "success", null);
            loginRecordService.save(record);
        } catch (Exception e) {
            log.debug("记录登录成功日志忽略", e);
        }
    }

    /**
     * 构建登录响应数据
     */
    private Map<String, Object> buildLoginResponseData(User loginUser, String accessToken,
                                                        String refreshToken, String clientIp,
                                                        String location) {
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("tokenType", "Bearer");
        data.put("accessTokenExpireIn", 1800); // 30分钟
        data.put("userId", loginUser.getId());
        data.put("phone", loginUser.getPhone().substring(0, 3) + "****" + loginUser.getPhone().substring(7, 11));
        data.put("username", loginUser.getUsername());
        data.put("nickname", loginUser.getNickname());
        data.put("avatar", loginUser.getAvatar());
        data.put("userType", loginUser.getUserType());
        data.put("status", loginUser.getStatus());
        data.put("location", location);
        data.put("lastLoginTime", loginUser.getLastLoginTime());
        data.put("lastLoginIp", clientIp);
        data.put("loginDays", loginUser.getLoginDays());
        data.put("totalLoginDays", loginUser.getTotalLoginDays());
        data.put("level", loginUser.getLevel());
        data.put("exp", loginUser.getExp());
        data.put("isOnline", loginUser.getIsOnline());

        if (loginUser.isVip()) {
            data.put("vipType", loginUser.getVipType());
            data.put("vipExpireTime", loginUser.getVipExpireTime());
            data.put("vipRemainingDays", loginUser.getVipRemainingDays());
        }

        return data;
    }

}
