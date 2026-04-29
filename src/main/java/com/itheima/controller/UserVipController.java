package com.itheima.controller;

import com.itheima.dto.OrderWithUserDTO;
import com.itheima.dto.VipRecordDTO;
import com.itheima.pojo.Result;
import com.itheima.pojo.VipConfig;
import com.itheima.pojo.VipOrder;
import com.itheima.service.VipService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user/vip")
@Validated
@RequiredArgsConstructor
public class UserVipController {

    private final VipService vipService;

    /**
     * 获取VIP套餐列表
     */
    @GetMapping("/plans")
    public Result<List<VipConfig>> getVipPlans() {
        try {
            List<VipConfig> vipConfigs = vipService.getAvailableVipConfigs();
            return Result.success(vipConfigs);
        } catch (Exception e) {
            log.error("获取VIP套餐列表失败", e);
            return Result.error("获取套餐列表失败");
        }
    }

    /**
     * 获取用户当前VIP信息
     */
    @GetMapping("/current")
    public Result<VipConfig> getCurrentVip() {
        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");

            VipConfig vipConfig = vipService.getUserCurrentVip(userId);
            if (vipConfig == null) {
                return Result.success(null); // 不是VIP用户
            }

            return Result.success(vipConfig);
        } catch (Exception e) {
            log.error("获取用户当前VIP信息失败", e);
            return Result.error("获取VIP信息失败");
        }
    }

    /**
     * 创建VIP订单
     */
    @PostMapping("/order/create")
    public Result<Map<String, Object>> createOrder(
            @RequestParam("vipType") Integer vipType,
            @RequestParam(defaultValue = "alipay", name = "payType") String payType) {

        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");

            Map<String, Object> orderInfo = vipService.createVipOrder(userId, vipType, payType);
            return Result.success(orderInfo);

        } catch (Exception e) {
            log.error("创建VIP订单失败: vipType={}, payType={}", vipType, payType, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 支付回调接口
     */
    @PostMapping("/order/callback")
    public Result<VipOrder> paymentCallback(
            @RequestParam("orderNo") String orderNo,
            @RequestBody Map<String, String> paymentData) {

        try {
            Result<VipOrder> result = vipService.handlePaymentCallback(orderNo, paymentData);
            return result;
        } catch (Exception e) {
            log.error("支付回调处理失败: orderNo={}", orderNo, e);
            return Result.error("处理支付回调失败");
        }
    }

    /**
     * 查询订单状态
     */
    @GetMapping("/order/status")
    public Result<VipOrder> getOrderStatus(@RequestParam("orderNo") String orderNo) {
        try {
            VipOrder order = vipService.getOrderStatus(orderNo);
            if (order == null) {
                return Result.error("订单不存在");
            }
            return Result.success(order);
        } catch (Exception e) {
            log.error("查询订单状态失败: orderNo={}", orderNo, e);
            return Result.error("查询订单状态失败");
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/order/cancel")
    public Result<String> cancelOrder(@RequestParam("orderNo") String orderNo) {
        try {
            boolean success = vipService.cancelOrder(orderNo);
            if (success) {
                return Result.success("订单已取消");
            } else {
                return Result.error("取消失败");
            }
        } catch (Exception e) {
            log.error("取消订单失败: orderNo={}", orderNo, e);
            return Result.error("取消失败");
        }
    }

    /**
     * 获取用户订单历史
     */
    @GetMapping("/order/history")
    public Result<List<VipOrder>> getOrderHistory(
            @RequestParam(defaultValue = "10", name = "limit") Integer limit) {

        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");

            List<VipOrder> orders = vipService.getUserOrderHistory(userId, limit);
            return Result.success(orders);
        } catch (Exception e) {
            log.error("获取订单历史失败", e);
            return Result.error("获取订单历史失败");
        }
    }

    /**
     * 续费VIP
     */
    @PostMapping("/renew")
    public Result<Map<String, Object>> renewVip(
            @RequestParam("vipType") Integer vipType,
            @RequestParam(defaultValue = "alipay", name = "payType") String payType) {

        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");

            // 续费就是创建一个新的订单
            Map<String, Object> orderInfo = vipService.createVipOrder(userId, vipType, payType);
            return Result.success(orderInfo);

        } catch (Exception e) {
            log.error("续费VIP失败: vipType={}, payType={}", vipType, payType, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分页查询VIP订单
     */
    @GetMapping("/orders")
    public Result<OrderWithUserDTO> getOrderList(
            @RequestParam(required = false, name = "orderNo") String orderNo,
            @RequestParam(required = false, name = "username") String username,
            @RequestParam(required = false, name = "vipType") Integer vipType,
            @RequestParam(required = false, name = "status") Integer status,
            @RequestParam(required = false, name = "startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1", name = "page") Integer page,
            @RequestParam(defaultValue = "20", name = "pageSize") Integer pageSize) {

        try {
            OrderWithUserDTO result = vipService.getOrderListByPage(
                    orderNo, username, vipType, status, startTime, endTime, page, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询VIP订单列表失败", e);
            return Result.error("查询失败");
        }
    }

    /**
     * 获取用户的VIP变更记录
     */
    @GetMapping("/records")
    public Result<List<VipRecordDTO>> getVipRecords(
            @RequestParam(defaultValue = "10", name = "limit") Integer limit) {

        try {
            Map<String, Object> claims = ThreadLocalUtil.get();
            Integer userId = (Integer) claims.get("id");

            List<VipRecordDTO> records = vipService.getUserVipRecords(userId, limit);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取VIP变更记录失败", e);
            return Result.error("获取记录失败");
        }
    }

    //导出VIP订单数据
    @GetMapping("orders/export")
    public Result<String> exportOrderList(
            @RequestParam(required = false, name = "orderNo") String orderNo,
            @RequestParam(required = false, name = "username") String username,
            @RequestParam(required = false, name = "vipType") Integer vipType,
            @RequestParam(required = false, name = "status") Integer status,
            @RequestParam(required = false, name = "startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false, name = "endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1", name = "page") Integer page,
            @RequestParam(defaultValue = "20", name = "pageSize") Integer pageSize) {

        try {
            OrderWithUserDTO result = vipService.getOrderListByPage(
                    orderNo, username, vipType, status, startTime, endTime, page, pageSize);
            return Result.success(result.toString());
        } catch (Exception e) {
            log.error("查询VIP订单列表失败", e);
            return Result.error("查询失败");
        }
    }
}