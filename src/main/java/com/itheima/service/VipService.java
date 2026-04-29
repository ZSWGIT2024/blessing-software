package com.itheima.service;

import com.itheima.dto.OrderWithUserDTO;
import com.itheima.dto.VipRecordDTO;
import com.itheima.pojo.Result;
import com.itheima.pojo.VipConfig;
import com.itheima.pojo.VipOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VipService {

    /**
     * 获取所有启用的VIP配置
     */
    List<VipConfig> getAvailableVipConfigs();

    /**
     * 获取用户当前的VIP信息
     * @param userId 用户ID
     * @return VIP配置信息，如果用户不是VIP则返回null
     */
    VipConfig getUserCurrentVip(Integer userId);

    /**
     * 创建VIP订单
     * @param userId 用户ID
     * @param vipType VIP类型
     * @param payType 支付方式
     * @return 订单信息
     */
    Map<String, Object> createVipOrder(Integer userId, Integer vipType, String payType);

    /**
     * 处理支付回调
     * @param orderNo 订单号
     * @param paymentData 支付数据
     * @return 处理结果
     */
    Result<VipOrder> handlePaymentCallback(String orderNo, Map<String, String> paymentData);

    /**
     * 查询订单状态
     * @param orderNo 订单号
     * @return 订单信息
     */
    VipOrder getOrderStatus(String orderNo);

    /**
     * 取消订单
     * @param orderNo 订单号
     * @return 取消结果
     */
    boolean cancelOrder(String orderNo);

    /**
     * 获取用户的VIP订单历史
     * @param userId 用户ID
     * @param limit 查询条数
     * @return 订单列表
     */
    List<VipOrder> getUserOrderHistory(Integer userId, Integer limit);

    /**
     * 获取用户的VIP变更记录
     * @param userId 用户ID
     * @param limit 查询条数
     * @return VIP变更记录列表
     */
    List<VipRecordDTO> getUserVipRecords(Integer userId, Integer limit);

    OrderWithUserDTO getOrderListByPage(String orderNo, String username, Integer vipType, Integer status, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer pageSize);
}