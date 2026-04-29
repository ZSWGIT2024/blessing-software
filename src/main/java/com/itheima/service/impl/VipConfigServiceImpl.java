package com.itheima.service.impl;

import com.itheima.common.UserConstant;
import com.itheima.mapper.UserMapper;
import com.itheima.mapper.VipConfigMapper;
import com.itheima.mapper.VipMapper;
import com.itheima.pojo.User;
import com.itheima.pojo.VipConfig;
import com.itheima.service.VipConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipConfigServiceImpl implements VipConfigService {

    private final VipConfigMapper vipConfigMapper;

    private final VipMapper vipMapper;

    private final UserMapper userMapper;

    @Override
    public List<VipConfig> getAllVipConfigs() {
        return vipConfigMapper.findAll();
    }

    @Override
    public List<VipConfig> getActiveVipConfigs() {
        return vipMapper.findAllActiveVipConfigs();
    }

    @Override
    public VipConfig getVipConfigByType(Integer vipType) {
        if (vipType == null) {
            return null;
        }
        return vipConfigMapper.findByVipType(vipType);
    }

    @Override
    public boolean addVipConfig(VipConfig vipConfig) {
        try {
            // 检查VIP类型是否已存在
            int count = vipConfigMapper.countByVipType(vipConfig.getVipType());
            if (count > 0) {
                log.warn("VIP类型已存在: {}", vipConfig.getVipType());
                return false;
            }

            // 设置默认值
            if (vipConfig.getIsActive() == null) {
                vipConfig.setIsActive(true);
            }

            int result = vipConfigMapper.insert(vipConfig);
            return result > 0;
        } catch (Exception e) {
            log.error("添加VIP配置失败", e);
            return false;
        }
    }

    @Override
    public boolean updateVipConfig(VipConfig vipConfig) {
        try {
            int result = vipConfigMapper.update(vipConfig);
            return result > 0;
        } catch (Exception e) {
            log.error("更新VIP配置失败", e);
            return false;
        }
    }

    @Override
    public boolean deleteVipConfig(Integer id) {
        try {
            int result = vipConfigMapper.delete(id);
            return result > 0;
        } catch (Exception e) {
            log.error("删除VIP配置失败", e);
            return false;
        }
    }

    @Override
    public boolean toggleVipConfigStatus(Integer id, Boolean isActive) {
        try {
            int result = vipConfigMapper.updateStatus(id, isActive);
            return result > 0;
        } catch (Exception e) {
            log.error("更新VIP配置状态失败", e);
            return false;
        }
    }

    @Override
    public BigDecimal calculateDiscountRate(VipConfig vipConfig) {
        if (vipConfig == null || vipConfig.getCurrentPrice() == null ||
                vipConfig.getOriginalPrice() == null || vipConfig.getOriginalPrice().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }

        return vipConfig.getCurrentPrice()
                .divide(vipConfig.getOriginalPrice(), 2, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, Object> getVipPriceInfo(Integer vipType) {
        VipConfig config = getVipConfigByType(vipType);
        if (config == null) {
            return null;
        }

        Map<String, Object> priceInfo = new HashMap<>();
        priceInfo.put("vipType", config.getVipType());
        priceInfo.put("vipName", config.getVipName());
        priceInfo.put("originalPrice", config.getOriginalPrice());
        priceInfo.put("currentPrice", config.getCurrentPrice());
        priceInfo.put("durationDays", config.getDurationDays());
        priceInfo.put("discountRate", calculateDiscountRate(config));
        priceInfo.put("discountTag", config.getDiscountTag());
        priceInfo.put("perDayPrice", calculatePerDayPrice(config));

        return priceInfo;
    }

    /**
     * 计算每日价格
     */
    private BigDecimal calculatePerDayPrice(VipConfig config) {
        if (config.getDurationDays() == null || config.getDurationDays() <= 0) {
            return config.getCurrentPrice();
        }
        return config.getCurrentPrice()
                .divide(new BigDecimal(config.getDurationDays()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, Object> getVipBenefits(Integer vipType) {
        VipConfig config = getVipConfigByType(vipType);
        if (config == null) {
            return getDefaultBenefits(vipType);
        }

        Map<String, Object> benefits = new HashMap<>();
        benefits.put("vipType", config.getVipType());
        benefits.put("vipName", config.getVipName());
        benefits.put("maxDailyUpload", config.getMaxDailyUpload());
        benefits.put("adFree", config.getAdFree());
        benefits.put("exclusiveBadge", config.getExclusiveBadge());
        benefits.put("priorityReview", config.getPriorityReview());
        benefits.put("highlightFeatures", config.getHighlightFeatures());
        benefits.put("description", config.getDescription());

        return benefits;
    }

    /**
     * 获取默认的VIP权益（当配置不存在时）
     */
    private Map<String, Object> getDefaultBenefits(Integer vipType) {
        Map<String, Object> benefits = new HashMap<>();
        benefits.put("vipType", vipType);
        benefits.put("vipName", getDefaultVipName(vipType));

        // 设置默认权益
        switch (vipType) {
            case 1: // 月度VIP
                benefits.put("maxDailyUpload", 50);
                benefits.put("adFree", true);
                break;
            case 2: // 季度VIP
                benefits.put("maxDailyUpload", 100);
                benefits.put("adFree", true);
                break;
            case 3: // 年度VIP
                benefits.put("maxDailyUpload", 200);
                benefits.put("adFree", true);
                break;
            case 4: // 终身VIP
                benefits.put("maxDailyUpload", -1); // 无限
                benefits.put("adFree", true);
                break;
            default:
                benefits.put("maxDailyUpload", 20);
                benefits.put("adFree", false);
        }

        return benefits;
    }

    private String getDefaultVipName(Integer vipType) {
        switch (vipType) {
            case 1:
                return "月度VIP";
            case 2:
                return "季度VIP";
            case 3:
                return "年度VIP";
            case 4:
                return "终身VIP";
            default:
                return "普通用户";
        }
    }

    @Override
    public Map<String, Object> checkPurchaseEligibility(Integer userId, Integer vipType) {
        Map<String, Object> result = new HashMap<>();

        // 获取用户信息
        User user = userMapper.findUserById(userId);
        if (user == null) {
            result.put("eligible", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 获取VIP配置
        VipConfig config = getVipConfigByType(vipType);
        if (config == null) {
            result.put("eligible", false);
            result.put("message", "VIP配置不存在");
            return result;
        }

        // 检查是否已启用
        if (!config.getIsActive()) {
            result.put("eligible", false);
            result.put("message", "该VIP套餐已下架");
            return result;
        }

        // 检查是否已经是终身VIP
        if (user.getVipType() != null && UserConstant.VIP_TYPE_LIFETIME.equals(user.getVipType())) {
            result.put("eligible", false);
            result.put("message", "您已是终身VIP，无需重复购买");
            return result;
        }

        // 检查是否已购买更高级别的VIP
        if (user.getVipType() != null && user.getVipType() > vipType) {
            result.put("eligible", false);
            result.put("message", "您已拥有更高级别的VIP");
            return result;
        }

        // 检查VIP是否在有效期内
        if (user.getVipExpireTime() != null && user.getVipExpireTime().isAfter(LocalDateTime.now())) {
            // 如果当前VIP未过期，需要提示
            long remainingDays = Duration.between(LocalDateTime.now(), user.getVipExpireTime()).toDays();
            if (remainingDays > 7) { // 剩余超过7天
                result.put("eligible", false);
                result.put("message", String.format("您当前VIP还有%d天到期，建议到期后再续费", remainingDays));
                return result;
            }
        }

        result.put("eligible", true);
        result.put("message", "可以购买");
        result.put("vipConfig", config);

        return result;
    }

}
