package com.itheima.mapper;

import com.itheima.dto.OrderWithUserDTO;
import com.itheima.dto.VipRecordDTO;
import com.itheima.pojo.VipConfig;
import com.itheima.pojo.VipOrder;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface VipMapper {

    // ========== VIP配置相关 ==========

    /**
     * 查询所有启用的VIP配置
     */
    List<VipConfig> findAllActiveVipConfigs();

    /**
     * 根据VIP类型查询配置
     */
    VipConfig findVipConfigByType(@Param("vipType") Integer vipType);

    // ========== VIP订单相关 ==========

    /**
     * 创建VIP订单
     */
    int createVipOrder(VipOrder order);

    /**
     * 根据订单号查询订单
     */
    VipOrder findOrderByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 更新订单状态
     */
    int updateOrderStatus(@Param("orderNo") String orderNo,
                          @Param("status") Integer status,
                          @Param("payTime") LocalDateTime payTime,
                          @Param("expireTime") LocalDateTime expireTime,
                          @Param("transactionId") String transactionId,
                          @Param("paymentData") String paymentData);

    /**
     * 查询用户的VIP订单历史
     */
    List<VipOrder> findUserOrders(@Param("userId") Integer userId,
                                  @Param("status") Integer status,
                                  @Param("limit") Integer limit);

    /**
     * 查询用户当前的VIP信息
     */
    VipConfig findCurrentUserVip(@Param("userId") Integer userId);

    /**
     * 检查用户是否已有未支付的订单
     */
    int countUnpaidOrders(@Param("userId") Integer userId);

    /**
     * 查询即将到期的VIP订单
     */
    List<VipOrder> findExpiringOrders();

    /**
     * 统计VIP订单数量
     */
    int countOrders(@Param("orderNo") String orderNo,
                    @Param("username") String username,
                    @Param("vipType") Integer vipType,
                    @Param("status") Integer status,
                    @Param("startTime") LocalDateTime startTime,
                    @Param("endTime") LocalDateTime endTime);

    /**
     * 记录VIP变更历史
     */
    int insertVipRecord(@Param("userId") Integer userId,
                        @Param("vipType") Integer vipType,
                        @Param("orderNo") String orderNo,
                        @Param("actionType") Integer actionType,
                        @Param("oldExpireTime") LocalDateTime oldExpireTime,
                        @Param("newExpireTime") LocalDateTime newExpireTime,
                        @Param("remark") String remark);

    /**
     * 分页查询VIP订单（带用户信息）
     */
    List<VipOrder> findOrdersByPage(@Param("orderNo") String orderNo,
                                            @Param("username") String username,
                                            @Param("vipType") Integer vipType,
                                            @Param("status") Integer status,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("offset") Integer offset,
                                            @Param("pageSize") Integer pageSize);

    /**
     * 查询用户的VIP变更记录
     */
    List<VipRecordDTO> findUserVipRecords(@Param("userId") Integer userId,
                                          @Param("limit") Integer limit);
}