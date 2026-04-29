package com.itheima.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.common.UserConstant;
import com.itheima.dto.OrderWithUserDTO;
import com.itheima.dto.VipRecordDTO;
import com.itheima.mapper.UserMapper;
import com.itheima.mapper.VipMapper;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.pojo.VipConfig;
import com.itheima.pojo.VipOrder;
import com.itheima.service.UserService;
import com.itheima.service.UserStatisticsService;
import com.itheima.service.VipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipServiceImpl implements VipService {

    private final VipMapper vipMapper;

    private final UserMapper userMapper;

    private final UserService userService;

    private final UserStatisticsService userStatisticsService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // VIP类型对应的天数映射
    private static final Map<Integer, Integer> VIP_DURATION_MAP = new HashMap<>();

    // VIP操作类型常量
    private static final int VIP_ACTION_OPEN = 1;     // 开通
    private static final int VIP_ACTION_RENEW = 2;    // 续费
    private static final int VIP_ACTION_UPGRADE = 3;  // 升级
    private static final int VIP_ACTION_DOWNGRADE = 4;// 降级
    private static final int VIP_ACTION_EXPIRE = 5;   // 到期
    private static final int VIP_ACTION_CANCEL = 6;   // 取消

    static {
        VIP_DURATION_MAP.put(1, 30);   // 月度VIP
        VIP_DURATION_MAP.put(2, 90);   // 季度VIP
        VIP_DURATION_MAP.put(3, 365);  // 年度VIP
        VIP_DURATION_MAP.put(4, 0);    // 终身VIP
    }

    // 缓存配置
    private static final String VIP_CONFIG_CACHE_KEY = "vip:configs";
    private static final String ORDER_LOCK_KEY = "vip:order:lock:";

    @Override
    public List<VipConfig> getAvailableVipConfigs() {
        // 先从缓存获取
        List<VipConfig> cachedConfigs = (List<VipConfig>) redisTemplate.opsForValue().get(VIP_CONFIG_CACHE_KEY);
        if (cachedConfigs != null) {
            return cachedConfigs;
        }

        // 从数据库查询
        List<VipConfig> configs = vipMapper.findAllActiveVipConfigs();

        // 设置缓存，有效期1小时
        redisTemplate.opsForValue().set(VIP_CONFIG_CACHE_KEY, configs, 1, TimeUnit.HOURS);

        return configs;
    }

    @Override
    public VipConfig getUserCurrentVip(Integer userId) {
        try {
            VipConfig currentUserVip = vipMapper.findCurrentUserVip(userId);
            User user = userMapper.findUserById(userId);
            currentUserVip.setExpireTime(user.getVipExpireTime());
            return currentUserVip;
        } catch (Exception e) {
            log.error("查询用户当前VIP信息失败: userId={}", userId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createVipOrder(Integer userId, Integer vipType, String payType) {
        // 获取分布式锁，防止重复下单
        String lockKey = ORDER_LOCK_KEY + userId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(locked)) {
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }

        try {
            // 1. 检查VIP配置
            VipConfig vipConfig = vipMapper.findVipConfigByType(vipType);
            if (vipConfig == null) {
                throw new RuntimeException("VIP配置不存在或已下架");
            }

            // 2. 检查用户是否存在
            User user = userMapper.findUserById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            // 3. 检查用户是否有未支付的订单
            int unpaidCount = vipMapper.countUnpaidOrders(userId);
            if (unpaidCount > 0) {
                throw new RuntimeException("您有未支付的订单，请先完成支付");
            }

            // 4. 获取用户当前VIP信息，判断是否需要升级
            VipConfig currentVip = getUserCurrentVip(userId);
            String remark = "";
            if (currentVip != null) {
                if (vipType > currentVip.getVipType()) {
                    remark = "VIP升级";
                } else if (vipType < currentVip.getVipType()) {
                    remark = "VIP降级";
                } else {
                    remark = "VIP续费";
                }
            } else {
                remark = "首次开通VIP";
            }

            // 5. 创建订单
            VipOrder order = new VipOrder();
            order.setOrderNo(generateOrderNo());
            order.setUserId(userId);
            order.setVipType(vipType);
            order.setAmount(vipConfig.getCurrentPrice());
            order.setPayType(payType);
            order.setStatus(0); // 待支付
            order.setCreateTime(LocalDateTime.now());

            int result = vipMapper.createVipOrder(order);
            if (result <= 0) {
                throw new RuntimeException("创建订单失败");
            }

            log.info("创建VIP订单成功: orderNo={}, userId={}, vipType={}, amount={}, remark={}",
                    order.getOrderNo(), userId, vipType, vipConfig.getCurrentPrice(), remark);

            // 6. 返回订单信息
            Map<String, Object> response = new HashMap<>();
            response.put("orderNo", order.getOrderNo());
            response.put("amount", order.getAmount());
            response.put("vipName", vipConfig.getVipName());
            response.put("durationDays", vipConfig.getDurationDays());
            response.put("remark", remark);

            // 根据支付方式生成支付参数
            Map<String, Object> paymentParams = generatePaymentParams(order, vipConfig);
            response.put("paymentParams", paymentParams);

            return response;

        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    @Transactional
    public Result<VipOrder> handlePaymentCallback(String orderNo, Map<String, String> paymentData) {
        try {
            // 1. 查询订单
            VipOrder order = vipMapper.findOrderByOrderNo(orderNo);
            if (order == null) {
                log.error("订单不存在: orderNo={}", orderNo);
                return Result.error("订单不存在");
            }

            // 2. 检查订单状态（防止重复处理）
            if (order.getStatus() == 1) {
                log.warn("订单已支付成功: orderNo={}", orderNo);
                return Result.success(order);
            }

            // 3. 验证支付结果
            boolean paymentSuccess = verifyPayment(paymentData);
            if (!paymentSuccess) {
                log.error("支付验证失败: orderNo={}", orderNo);
                order.setStatus(2); // 支付失败
                vipMapper.updateOrderStatus(orderNo, 2, null, null, null, null);
                return Result.error("支付验证失败");
            }

            // 4. 获取VIP配置
            VipConfig vipConfig = vipMapper.findVipConfigByType(order.getVipType());
            if (vipConfig == null) {
                log.error("VIP配置不存在: vipType={}", order.getVipType());
                return Result.error("VIP配置不存在");
            }

            // 5. 获取用户当前VIP信息
            User user = userMapper.findUserById(order.getUserId());
            if (user == null) {
                log.error("用户不存在: userId={}", order.getUserId());
                return Result.error("用户不存在");
            }

            // 6. 计算VIP到期时间
            LocalDateTime newExpireTime = calculateExpireTime(vipConfig.getVipType(), user.getVipExpireTime());

            // 7. 记录VIP变更历史
            recordVipChangeHistory(user, order, vipConfig, newExpireTime);

            // 8. 更新用户VIP信息
            boolean userUpdated = userService.updateUserVip(order.getUserId(),
                    vipConfig.getVipType(), newExpireTime);

            if (!userUpdated) {
                log.error("更新用户VIP信息失败: userId={}, vipType={}", order.getUserId(), vipConfig.getVipType());
                return Result.error("更新用户信息失败");
            }

            // 9. 更新订单状态
            String paymentDataJson = objectMapper.writeValueAsString(paymentData);
            order.setStatus(1); // 支付成功
            order.setPayTime(LocalDateTime.now());
            order.setExpireTime(newExpireTime);
            order.setTransactionId(paymentData.get("transaction_id"));
            order.setPaymentData(paymentDataJson);

            // 修复这里：参数顺序需要与Mapper中定义的一致
            vipMapper.updateOrderStatus(
                    order.getOrderNo(),        // orderNo
                    1,                         // status
                    order.getPayTime(),        // payTime
                    newExpireTime,             // expireTime (新增)
                    order.getTransactionId(),  // transactionId
                    paymentDataJson            // paymentData
            );

            // 10. 记录统计事件
            Map<String, Object> vipData = new HashMap<>();
            vipData.put("vipType", vipConfig.getVipType());
            vipData.put("vipName", vipConfig.getVipName());
            vipData.put("amount", order.getAmount());
            vipData.put("orderNo", orderNo);
            vipData.put("expireTime", newExpireTime);

            // 判断是续费还是新开通
            boolean isRenewal = user.getVipExpireTime() != null &&
                    user.getVipExpireTime().isAfter(LocalDateTime.now());

            if (isRenewal) {
                vipData.put("purchaseType", "renew");
                userStatisticsService.recordVipRenewEvent(order.getUserId(), vipData);
            } else {
                vipData.put("purchaseType", "new");
                userStatisticsService.recordVipPurchaseEvent(order.getUserId(), vipData);
            }

            // 11. 清除相关缓存
            clearUserVipCache(order.getUserId());

            log.info("VIP支付回调处理成功: orderNo={}, userId={}, vipType={}, expireTime={}",
                    orderNo, order.getUserId(), vipConfig.getVipType(), newExpireTime);

            return Result.success(order);

        } catch (Exception e) {
            log.error("处理支付回调失败: orderNo={}", orderNo, e);
            return Result.error("处理支付回调失败");
        }
    }

    @Override
    public VipOrder getOrderStatus(String orderNo) {
        return vipMapper.findOrderByOrderNo(orderNo);
    }

    @Override
    public boolean cancelOrder(String orderNo) {
        try {
            VipOrder order = vipMapper.findOrderByOrderNo(orderNo);
            if (order == null || order.getStatus() != 0) {
                return false; // 订单不存在或不是待支付状态
            }

            // 更新订单状态为已取消
            vipMapper.updateOrderStatus(orderNo, 3, null, null, null, null);

            // 记录VIP变更历史
            vipMapper.insertVipRecord(order.getUserId(), order.getVipType(),
                    orderNo, VIP_ACTION_CANCEL, null, null, "用户取消订单");

            return true;
        } catch (Exception e) {
            log.error("取消订单失败: orderNo={}", orderNo, e);
            return false;
        }
    }

    @Override
    public List<VipOrder> getUserOrderHistory(Integer userId, Integer limit) {
        return vipMapper.findUserOrders(userId, null, limit);
    }

    // VipServiceImpl.java
    @Override
    public OrderWithUserDTO getOrderListByPage(String orderNo, String username,
                                               Integer vipType, Integer status,
                                               LocalDateTime startTime, LocalDateTime endTime,
                                               Integer page, Integer pageSize) {
        // 计算偏移量
        int offset = (page - 1) * pageSize;

        // 查询订单列表（返回DTO对象）
        List<VipOrder> orders = vipMapper.findOrdersByPage(
                orderNo, username, vipType, status, startTime, endTime, offset, pageSize);

        // 查询用户信息
        for (VipOrder order : orders) {
            User user = userMapper.findUserById(order.getUserId());
            order.setUsername(user.getUsername());
            order.setPhone(user.getPhone());
            order.setVipName(getVipTypeName(order.getVipType()));
        }
        // 查询总记录数
        int total = vipMapper.countOrders(orderNo, username, vipType, status, startTime, endTime);
        OrderWithUserDTO orderWithUserDTO = new OrderWithUserDTO();
        orderWithUserDTO.setList(orders);
        orderWithUserDTO.setPage(page);
        orderWithUserDTO.setPageSize(pageSize);
        orderWithUserDTO.setTotal(total);

        return orderWithUserDTO;
    }

    @Override
    public List<VipRecordDTO> getUserVipRecords(Integer userId, Integer limit) {
        try {
            // 先从缓存获取
            String cacheKey = "user:vip:records:" + userId + ":" + limit;
            List<VipRecordDTO> cachedRecords = (List<VipRecordDTO>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedRecords != null) {
                return cachedRecords;
            }

            // 查询数据库
            List<VipRecordDTO> records = vipMapper.findUserVipRecords(userId, limit);

            // 格式化记录数据
            List<VipRecordDTO> formattedRecords = formatVipRecords(records);

            // 设置缓存，有效期10分钟
            redisTemplate.opsForValue().set(cacheKey, formattedRecords, 10, TimeUnit.MINUTES);

            return formattedRecords;
        } catch (Exception e) {
            log.error("查询用户VIP变更记录失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 格式化VIP记录数据
     */
    private List<VipRecordDTO> formatVipRecords(List<VipRecordDTO> records) {
        List<VipRecordDTO> formattedRecords = new ArrayList<>();

        for (VipRecordDTO record : records) {
            VipRecordDTO formatted = new VipRecordDTO();
            // 复制原有属性
            formatted.setId(record.getId());
            formatted.setUserId(record.getUserId());
            formatted.setVipType(record.getVipType());
            formatted.setOrderNo(record.getOrderNo());
            formatted.setActionType(record.getActionType());
//            formatted.setOldExpireTime(record.getOldExpireTime());
//            formatted.setNewExpireTime(record.getNewExpireTime());
            formatted.setRemark(record.getRemark());
//            formatted.setCreateTime(record.getCreateTime());
            formatted.setCreateTimeStr(record.getCreateTime().toString());


            formattedRecords.add(formatted);
        }

        return formattedRecords;
    }

    /**
     * 获取操作类型名称
     */
    private String getActionTypeName(Integer actionType) {
        Map<Integer, String> actionTypeNames = new HashMap<>();
        actionTypeNames.put(1, "开通");
        actionTypeNames.put(2, "续费");
        actionTypeNames.put(3, "升级");
        actionTypeNames.put(4, "降级");
        actionTypeNames.put(5, "到期");
        actionTypeNames.put(6, "取消");
        return actionTypeNames.getOrDefault(actionType, "未知操作");
    }

    /**
     * 定时任务：检查即将到期的VIP订单
     */
    @Scheduled(cron = "0 0 9 * * ?") // 每天上午9点执行
    public void checkExpiringVipOrders() {
        try {
            List<VipOrder> expiringOrders = vipMapper.findExpiringOrders();

            for (VipOrder order : expiringOrders) {
                // 发送到期提醒
                sendExpirationReminder(order);
                log.info("发送VIP到期提醒: orderNo={}, userId={}, expireTime={}",
                        order.getOrderNo(), order.getUserId(), order.getExpireTime());
            }

            log.info("VIP到期检查完成，共检查{}个订单", expiringOrders.size());
        } catch (Exception e) {
            log.error("检查即将到期的VIP订单失败", e);
        }
    }

    /**
     * 定时任务：处理已过期的VIP
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨0点执行
    public void handleExpiredVip() {
        try {
            // 这里可以实现VIP到期后的处理逻辑
            // 例如：更新用户VIP状态为过期，发送到期通知等
            log.info("处理已过期的VIP任务开始执行");
        } catch (Exception e) {
            log.error("处理已过期的VIP失败", e);
        }
    }

    // ========== 私有方法 ==========

    /**
     * 记录VIP变更历史
     */
    private void recordVipChangeHistory(User user, VipOrder order,
                                        VipConfig vipConfig, LocalDateTime newExpireTime) {
        try {
            LocalDateTime oldExpireTime = user.getVipExpireTime();
            int actionType;
            String remark;

            // 判断操作类型
            if (oldExpireTime == null) {
                actionType = VIP_ACTION_OPEN;
                remark = "首次开通" + vipConfig.getVipName();
            } else if (oldExpireTime.isBefore(LocalDateTime.now())) {
                actionType = VIP_ACTION_OPEN;
                remark = "重新开通" + vipConfig.getVipName();
            } else {
                // 判断是升级、降级还是续费
                Integer oldVipType = user.getVipType();
                if (oldVipType == null) {
                    actionType = VIP_ACTION_RENEW;
                    remark = "续费" + vipConfig.getVipName();
                } else if (order.getVipType() > oldVipType) {
                    actionType = VIP_ACTION_UPGRADE;
                    remark = "从" + getVipTypeName(oldVipType) + "升级到" + vipConfig.getVipName();
                } else if (order.getVipType() < oldVipType) {
                    actionType = VIP_ACTION_DOWNGRADE;
                    remark = "从" + getVipTypeName(oldVipType) + "降级到" + vipConfig.getVipName();
                } else {
                    actionType = VIP_ACTION_RENEW;
                    remark = "续费" + vipConfig.getVipName();
                }
            }

            vipMapper.insertVipRecord(order.getUserId(), vipConfig.getVipType(),
                    order.getOrderNo(), actionType, oldExpireTime, newExpireTime, remark);

        } catch (Exception e) {
            log.error("记录VIP变更历史失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 获取VIP类型名称
     */
    private String getVipTypeName(Integer vipType) {
        Map<Integer, String> typeNames = new HashMap<>();
        typeNames.put(1, "月度会员");
        typeNames.put(2, "季度会员");
        typeNames.put(3, "年度会员");
        typeNames.put(4, "终身会员");
        return typeNames.getOrDefault(vipType, "未知会员");
    }

    /**
     * 计算VIP到期时间
     */
    private LocalDateTime calculateExpireTime(Integer vipType, LocalDateTime currentExpireTime) {
        LocalDateTime now = LocalDateTime.now();

        if (vipType == UserConstant.VIP_TYPE_LIFETIME) {
            // 终身VIP，设置100年后
            return now.plusYears(100);
        } else {
            // 根据VIP类型计算天数
            Integer days = VIP_DURATION_MAP.get(vipType);
            if (currentExpireTime != null && currentExpireTime.isAfter(now)) {
                // 在原有基础上续期
                return currentExpireTime.plusDays(days != null ? days : 30);
            } else {
                // 从当前时间开始计算
                return now.plusDays(days != null ? days : 30);
            }
        }
    }

    /**
     * 发送到期提醒
     */
    private void sendExpirationReminder(VipOrder order) {
        try {
            // 这里可以实现发送站内信、短信或邮件提醒
            // 示例：发送站内信
            String message = String.format(
                    "您的%s即将在%s到期，请及时续费以继续享受VIP特权",
                    getVipTypeName(order.getVipType()),
                    order.getExpireTime().toString()
            );

            // 记录提醒日志
            log.info("发送到期提醒: userId={}, message={}", order.getUserId(), message);

        } catch (Exception e) {
            log.error("发送到期提醒失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        // 格式: V + 时间戳 + 随机数
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) ((Math.random() * 9 + 1) * 1000));
        return "V" + timestamp.substring(5) + random;
    }

    /**
     * 生成支付参数（根据具体支付接口实现）
     */
    private Map<String, Object> generatePaymentParams(VipOrder order, VipConfig vipConfig) {
        Map<String, Object> params = new HashMap<>();

        // 这里根据不同的支付方式生成不同的参数
        // 示例：支付宝支付参数
        if ("alipay".equals(order.getPayType())) {
            params.put("subject", "樱花会员 - " + vipConfig.getVipName());
            params.put("body", vipConfig.getDescription());
            params.put("total_amount", order.getAmount().toString());
            params.put("out_trade_no", order.getOrderNo());
            params.put("product_code", "FAST_INSTANT_TRADE_PAY");
        }
        // 微信支付参数
        else if ("wechat".equals(order.getPayType())) {
            params.put("description", "樱花会员 - " + vipConfig.getVipName());
            params.put("out_trade_no", order.getOrderNo());
            params.put("amount", order.getAmount().multiply(new BigDecimal("100")).intValue());
        }
        // 余额支付参数
        else if ("balance".equals(order.getPayType())) {
            params.put("need_password", true);
        }

        return params;
    }

    /**
     * 验证支付结果（需要对接具体的支付接口）
     */
    private boolean verifyPayment(Map<String, String> paymentData) {
        // 这里需要根据具体的支付接口实现验证逻辑
        // 示例：支付宝支付验证
        if ("alipay".equals(paymentData.get("pay_type"))) {
            return "TRADE_SUCCESS".equals(paymentData.get("trade_status"));
        }
        // 微信支付验证
        else if ("wechat".equals(paymentData.get("pay_type"))) {
            return "SUCCESS".equals(paymentData.get("result_code"));
        }
        // 余额支付验证
        else if ("balance".equals(paymentData.get("pay_type"))) {
            return "success".equals(paymentData.get("status"));
        }

        return false;
    }

    /**
     * 清除用户VIP缓存
     */
    private void clearUserVipCache(Integer userId) {
        String cacheKey = "user:vip:" + userId;
        redisTemplate.delete(cacheKey);
    }
}