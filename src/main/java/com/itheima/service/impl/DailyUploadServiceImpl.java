package com.itheima.service.impl;

import com.itheima.common.UserConstant;
import com.itheima.mapper.UserDailyUploadMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.User;
import com.itheima.pojo.UserDailyUpload;
import com.itheima.service.DailyUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyUploadServiceImpl implements DailyUploadService {

    private final UserMapper userMapper;

    private final UserDailyUploadMapper userDailyUploadMapper;

    // 获取用户今日上传数量
    @Override
    public int getTodayUploadCount(Integer userId) {
        LocalDate today = LocalDate.now();
        UserDailyUpload dailyUpload = userDailyUploadMapper.selectByUserAndDate(userId, today);
        return dailyUpload != null ? dailyUpload.getUploadCount() : 0;
    }

    // 修复：检查上传限制的方法（现在会根据VIP类型判断）
    @Override
    public void checkUploadLimit(Integer userId, int fileCount) {
        // 1. 获取用户信息
        User user = userMapper.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!user.isActive()) {
            throw new RuntimeException("账号状态异常，无法上传");
        }

        // 2. 获取用户每日上传限制（根据VIP类型）
        Integer dailyLimit = user.getDailyUploadLimit();

        // 3. 如果是无限上传，直接通过
        if (user.isUnlimitedUpload()) {
            log.info("用户 {} 是{}，可无限上传", userId,
                    user.isAdmin() ? "管理员" : "终身VIP");
            return;
        }

        // 4. 检查VIP是否过期（如果是月度/季度/年度VIP）
        if (!UserConstant.VIP_TYPE_NONE.equals(user.getVipType()) &&
                !UserConstant.VIP_TYPE_LIFETIME.equals(user.getVipType()) &&
                user.getVipExpireTime() != null &&
                LocalDateTime.now().isAfter(user.getVipExpireTime())) {

            // VIP已过期，降级为普通用户
            dailyLimit = UserConstant.DAILY_UPLOAD_COUNT_NORMAL;
            log.warn("用户 {} 的VIP已过期，降级为普通用户上传限制", userId);
        }

        // 5. 获取今日上传统计
        LocalDate today = LocalDate.now();
        UserDailyUpload dailyUpload = userDailyUploadMapper.selectByUserAndDate(userId, today);

        int todayUploadCount = 0;
        if (dailyUpload != null) {
            todayUploadCount = dailyUpload.getUploadCount();
        }

        // 6. 检查是否超过限制
        if (todayUploadCount >= dailyLimit) {
            String errorMsg = String.format("今日上传已达上限（%d张），请明天再试", dailyLimit);
            if (!UserConstant.VIP_TYPE_NONE.equals(user.getVipType())) {
                errorMsg += "（当前为" + getVipTypeName(user.getVipType()) + "）";
            }
            throw new RuntimeException(errorMsg);
        }

        // 7. 检查加上当前文件是否会超过限制
        if (todayUploadCount + fileCount > dailyLimit) {
            int remaining = dailyLimit - todayUploadCount;
            String errorMsg = String.format(
                    "今日还可上传 %d 张，本次尝试上传 %d 张，超出 %d 张",
                    remaining, fileCount, fileCount - remaining
            );

            if (!UserConstant.VIP_TYPE_NONE.equals(user.getVipType())) {
                errorMsg += "（当前为" + getVipTypeName(user.getVipType()) + "，每日限" + dailyLimit + "张）";
            }

            throw new RuntimeException(errorMsg);
        }

        log.info("用户 {} 上传检查通过：今日已上传 {} 张，限制 {} 张，本次上传 {} 张",
                userId, todayUploadCount, dailyLimit, fileCount);
    }

    // 辅助方法：获取VIP类型名称
    @Override
    public String getVipTypeName(Integer vipType) {
        if (vipType == null) return "普通用户";

        return switch (vipType) {
            case 1 -> "月度VIP";
            case 2 -> "季度VIP";
            case 3 -> "年度VIP";
            case 4 -> "终身VIP";
            default -> "普通用户";
        };
    }

    // 修复：更新上传计数的方法
    @Override
    public void updateUploadCount(Integer userId, int increment) {
        try {
            LocalDate today = LocalDate.now();

            // 使用乐观锁或原子操作更新计数
            int updated = userDailyUploadMapper.incrementCount(userId, today, increment);

            if (updated == 0) {
                // 没有记录，创建新记录
                UserDailyUpload dailyUpload = new UserDailyUpload();
                dailyUpload.setUserId(userId);
                dailyUpload.setUploadDate(today);
                dailyUpload.setUploadCount(increment);
                dailyUpload.setCreateTime(LocalDateTime.now());
                dailyUpload.setUpdateTime(LocalDateTime.now());
                userDailyUploadMapper.insert(dailyUpload);
            }

            // 更新用户总上传计数
            User user = userMapper.findUserById(userId);
            if (user != null) {
                user.setUploadCount(user.getUploadCount() + increment);
                user.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(user);

                log.info("用户 {} 上传计数更新：今日+{}，总计{}",
                        userId, increment, user.getUploadCount());
            }

        } catch (Exception e) {
            log.error("更新用户 {} 上传计数失败", userId, e);
            // 这里不抛出异常，因为文件已经上传成功
        }
    }
}
