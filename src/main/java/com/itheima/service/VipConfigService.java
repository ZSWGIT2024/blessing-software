package com.itheima.service;

import com.itheima.pojo.VipConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface VipConfigService {

    /**
     * 获取所有VIP配置
     */
    List<VipConfig> getAllVipConfigs();

    /**
     * 获取启用的VIP配置
     */
    List<VipConfig> getActiveVipConfigs();

    /**
     * 根据VIP类型获取配置
     */
    VipConfig getVipConfigByType(Integer vipType);

    /**
     * 添加VIP配置
     */
    boolean addVipConfig(VipConfig vipConfig);

    /**
     * 更新VIP配置
     */
    boolean updateVipConfig(VipConfig vipConfig);

    /**
     * 删除VIP配置
     */
    boolean deleteVipConfig(Integer id);

    /**
     * 启用/禁用VIP配置
     */
    boolean toggleVipConfigStatus(Integer id, Boolean isActive);

    /**
     * 计算VIP折扣率
     */
    BigDecimal calculateDiscountRate(VipConfig vipConfig);

    /**
     * 获取VIP价格信息
     */
    Map<String, Object> getVipPriceInfo(Integer vipType);

    /**
     * 获取VIP权益详情
     */
    Map<String, Object> getVipBenefits(Integer vipType);

    /**
     * 检查是否可以购买VIP
     */
    Map<String, Object> checkPurchaseEligibility(Integer userId, Integer vipType);
}
