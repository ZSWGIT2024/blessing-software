package com.itheima.controller;


import com.itheima.common.UserConstant;
import com.itheima.dto.UserSimpleDTO;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import com.itheima.service.UserStatisticsService;
import com.itheima.utils.*;
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

    private final JwtUtil jwtUtil;  // 注入JwtUtil

    private final IpLocationUtil ipLocationUtil;


    private final AdvancedIpLocationUtil advancedIpLocationUtil;  // 使用更精确的IP定位

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
     * 用户登录
     */
    @PostMapping(value = "/login", produces = "application/json")
    public Result<Map<String, Object>> login(
            @Pattern(regexp = "^1[3-9]\\d{9}$")
            @RequestParam("phone") String phone,
            @Pattern(regexp = "^[a-zA-Z0-9_.]{5,16}$")
            @RequestParam("password") String password) {

        //记录登录所用时间秒单位
        long start = System.currentTimeMillis();
        System.out.println("登录开始时间：" + start);
        try {
            User loginUser = userService.findByPhone(phone);
            if (loginUser == null) {
                return Result.error("用户不存在");
            }

            // 检查用户状态
            if (!UserConstant.STATUS_ACTIVE.equals(loginUser.getStatus())) {
                return Result.error("账号已被封禁");
            }

            // 验证密码
            if (!Md5Util.md5Encrypt(password).equals(loginUser.getPassword())) {
                return Result.error("密码错误");
            }

            //在这里用时
            long cost1 = System.currentTimeMillis();
            System.out.println("登录结束时间：" + cost1);
            System.out.println("登录用时：" + (cost1 - start) / 1000 + "秒");

            // 获取客户端IP
            String clientIp = RequestUtils.getClientIp();

            // 使用高级IP定位获取详细位置信息
            Map<String, String> locationInfo = advancedIpLocationUtil.getDetailedLocation(clientIp);
            String country = locationInfo.getOrDefault("country", "未知");
            String region = locationInfo.getOrDefault("region", "未知");
            String city = locationInfo.getOrDefault("city", "未知");
            String isp = locationInfo.getOrDefault("isp", "未知");

            // 格式化位置信息
            String location;
            if ("中国".equals(country)) {
                if (region.equals(city) || city.contains("市辖区")) {
                    location = region;  // 直辖市只显示城市
                } else {
                    location = region + "·" + city;
                }
            } else if (!"未知".equals(country)) {
                location = country;
            } else {
                location = ipLocationUtil.getLocation(clientIp);
            }

            // 更新登录信息（包含IP和位置）
            updateLoginInfo(loginUser, clientIp, location);

            //在这里用时
            long cost2 = System.currentTimeMillis();
            System.out.println("登录结束时间2：" + cost2);
            System.out.println("登录2用时：" + (cost2 - start) / 1000 + "秒");

            // 生成JWT token
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("phone", loginUser.getPhone());
            claims.put("username", loginUser.getUsername());
            claims.put("nickname", loginUser.getNickname());
            claims.put("userType", loginUser.getUserType());
            claims.put("status", loginUser.getStatus());
            claims.put("avatar", loginUser.getAvatar());
            claims.put("clientIp", clientIp);
            claims.put("lastLoginLocation", location);
            claims.put("loginDays", loginUser.getLoginDays());
            claims.put("totalLoginDays", loginUser.getTotalLoginDays());

            String token = jwtUtil.generateToken(claims);

            // 存储token到Redis（有效期7天）
            redisTemplate.opsForValue().set(token, token, 7, TimeUnit.DAYS);

            log.info("用户登录成功: userId={}, phone={}, loginIp={}, location={}, country={}, region={}, city={}, isp={}",
                    loginUser.getId(), phone, clientIp, location, country, region, city, isp);

            //在这里用时
            long cost3 = System.currentTimeMillis();
            System.out.println("登录结束时间3：" + cost3);
            System.out.println("登录用时3：" + (cost3 - start) / 1000 + "秒");
            //设置用户在线状态
            loginUser.setIsOnline(UserConstant.ONLINE);

            // 登录成功后记录统计事件
            Map<String, Object> loginData = new HashMap<>();
            loginData.put("phone", phone.substring(0, 3) + "****" + phone.substring(7, 11));
            loginData.put("loginIp", clientIp);
            loginData.put("loginLocation", location);
            loginData.put("country", country);
            loginData.put("region", region);
            loginData.put("city", city);
            loginData.put("isp", isp);
            loginData.put("loginType", "password");

            //在这里用时
            long cost5 = System.currentTimeMillis();
            System.out.println("登录结束时间4：" + cost5);
            System.out.println("登录用时4：" + (cost5 - start) / 1000 + "秒");

            userStatisticsService.recordLoginEvent(loginUser.getId(), loginData);

            log.info("用户登录成功并记录统计: userId={}, phone={}", loginUser.getId(), phone);

            //在这里用时
            long cost4 = System.currentTimeMillis();
            System.out.println("登录结束时间5：" + cost4);
            System.out.println("登录用时5：" + (cost4 - start) / 1000 + "秒");
            // 返回完整的用户信息
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", loginUser.getId());
            data.put("phone", loginUser.getPhone().substring(0, 3) + "****" + loginUser.getPhone().substring(7, 11));
            data.put("username", loginUser.getUsername());
            data.put("nickname", loginUser.getNickname());
            data.put("avatar", loginUser.getAvatar());
            data.put("userType", loginUser.getUserType());
            data.put("status", loginUser.getStatus());
            data.put("location", location);
            data.put("locationDetail", locationInfo);  // 返回详细的位置信息
            data.put("lastLoginTime", loginUser.getLastLoginTime());
            data.put("lastLoginIp", clientIp);
            data.put("loginDays", loginUser.getLoginDays());  // 连续登录天数
            data.put("totalLoginDays", loginUser.getTotalLoginDays());  // 累计登录天数
            data.put("level", loginUser.getLevel());  // 用户等级
            data.put("exp", loginUser.getExp());  // 经验值
            data.put("isOnline", loginUser.getIsOnline());

            // 如果是VIP，返回VIP信息
            if (loginUser.isVip()) {
                data.put("vipType", loginUser.getVipType());
                data.put("vipExpireTime", loginUser.getVipExpireTime());
                data.put("vipRemainingDays", loginUser.getVipRemainingDays());
            }

            //在这里用时
            long cost6 = System.currentTimeMillis();
            System.out.println("登录结束时间6：" + cost6);
            System.out.println("登录6用时：" + (cost6 - start) / 1000 + "秒");
            return Result.success(data);

        } catch (Exception e) {
            log.error("用户登录失败: phone={}", phone, e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 验证码登录
     */
    @PostMapping(value = "/loginByCode", produces = "application/json")
    public Result<Map<String, Object>> loginByCode(
            @Pattern(regexp = "^1[3-9]\\d{9}$")
            @RequestParam("phone") String phone,
            @RequestParam("code") String code) {

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
                // 如果用户不存在，可以自动注册（可选）
                // return autoRegisterByPhone(phone);
                return Result.error("用户不存在");
            }

            // 3. 检查用户状态
            if (!UserConstant.STATUS_ACTIVE.equals(loginUser.getStatus())) {
                return Result.error("账号已被封禁");
            }

            // 获取客户端IP
            String clientIp = RequestUtils.getClientIp();

            // 使用高级IP定位获取详细位置信息
            Map<String, String> locationInfo = advancedIpLocationUtil.getDetailedLocation(clientIp);
            String country = locationInfo.getOrDefault("country", "未知");
            String region = locationInfo.getOrDefault("region", "未知");
            String city = locationInfo.getOrDefault("city", "未知");
            String isp = locationInfo.getOrDefault("isp", "未知");

            // 格式化位置信息
            String location;
            if ("中国".equals(country)) {
                if (region.equals(city) || city.contains("市辖区")) {
                    location = region;  // 直辖市只显示城市
                } else {
                    location = region + "·" + city;
                }
            } else if (!"未知".equals(country)) {
                location = country;
            } else {
                location = ipLocationUtil.getLocation(clientIp);
            }

            // 更新登录信息（包含IP和位置）
            updateLoginInfo(loginUser, clientIp, location);

            // 生成JWT token
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("phone", loginUser.getPhone());
            claims.put("username", loginUser.getUsername());
            claims.put("nickname", loginUser.getNickname());
            claims.put("userType", loginUser.getUserType());
            claims.put("status", loginUser.getStatus());
            claims.put("avatar", loginUser.getAvatar());
            claims.put("clientIp", clientIp);
            claims.put("lastLoginLocation", location);
            claims.put("loginDays", loginUser.getLoginDays());
            claims.put("totalLoginDays", loginUser.getTotalLoginDays());

            String token = jwtUtil.generateToken(claims);

            // 存储token到Redis（有效期7天）
            redisTemplate.opsForValue().set(token, token, 7, TimeUnit.DAYS);

            log.info("用户登录成功: userId={}, phone={}, loginIp={}, location={}, country={}, region={}, city={}, isp={}",
                    loginUser.getId(), phone, clientIp, location, country, region, city, isp);

            //设置用户在线状态
            loginUser.setIsOnline(UserConstant.ONLINE);

            // 登录成功后记录统计事件
            Map<String, Object> loginData = new HashMap<>();
            loginData.put("phone", phone.substring(0, 3) + "****" + phone.substring(7, 11));
            loginData.put("loginIp", clientIp);
            loginData.put("loginLocation", location);
            loginData.put("country", country);
            loginData.put("region", region);
            loginData.put("city", city);
            loginData.put("isp", isp);
            loginData.put("loginType", "code");

            userStatisticsService.recordLoginEvent(loginUser.getId(), loginData);

            log.info("验证码登录成功并记录统计: userId={}, phone={}", loginUser.getId(), phone);

            // 返回完整的用户信息
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", loginUser.getId());
            data.put("phone", loginUser.getPhone().substring(0, 3) + "****" + loginUser.getPhone().substring(7, 11));
            data.put("username", loginUser.getUsername());
            data.put("nickname", loginUser.getNickname());
            data.put("avatar", loginUser.getAvatar());
            data.put("userType", loginUser.getUserType());
            data.put("status", loginUser.getStatus());
            data.put("location", location);
            data.put("locationDetail", locationInfo);  // 返回详细的位置信息
            data.put("lastLoginTime", loginUser.getLastLoginTime());
            data.put("lastLoginIp", clientIp);
            data.put("loginDays", loginUser.getLoginDays());  // 连续登录天数
            data.put("totalLoginDays", loginUser.getTotalLoginDays());  // 累计登录天数
            data.put("level", loginUser.getLevel());  // 用户等级
            data.put("exp", loginUser.getExp());  // 经验值
            data.put("isOnline", loginUser.getIsOnline());

            // 如果是VIP，返回VIP信息
            if (loginUser.isVip()) {
                data.put("vipType", loginUser.getVipType());
                data.put("vipExpireTime", loginUser.getVipExpireTime());
                data.put("vipRemainingDays", loginUser.getVipRemainingDays());
            }

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

            // 给予每日登录经验奖励
            userService.addExperience(user.getId(), UserConstant.ExpAction.DAILY_LOGIN,
                    "每日登录奖励，连续登录" + user.getLoginDays() + "天");

            log.info("用户每日登录奖励: userId={}, loginDays={}, totalLoginDays={}", user.getId(), user.getLoginDays(), user.getTotalLoginDays());
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
        //判断新密码和旧密码是否相同
        if (Md5Util.verify(newPwd, user.getPassword())) {
            return Result.error("新密码不能和旧密码相同");
        }
        //判断两次密码是否一致
        if (!newPwd.equals(rePwd)) {
            return Result.error("两次密码不一致");
        }
        //2.调用service完成密码更新
        userService.updatePwd(newPwd);
        //当用户密码更新后需要删除redis中的token
        ValueOperations operations = redisTemplate.opsForValue();
        operations.getOperations().delete(token);
        return Result.success();
    }


}
