package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.pojo.VipConfig;
import com.itheima.service.VipConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vip")
@Validated
@RequiredArgsConstructor
public class VipConfigController {

    private final VipConfigService vipConfigService;

    /**
     * 获取所有VIP配置（管理后台用）
     */
    @GetMapping("/configs/all")
    public Result<List<VipConfig>> getAllVipConfigs() {
        List<VipConfig> configs = vipConfigService.getAllVipConfigs();
        return Result.success(configs);
    }

    /**
     * 获取启用的VIP配置（前端展示用）
     */
    @GetMapping("/configs/active")
    public Result<List<VipConfig>> getActiveVipConfigs() {
        List<VipConfig> configs = vipConfigService.getActiveVipConfigs();
        return Result.success(configs);
    }

    /**
     * 根据VIP类型获取配置
     */
    @GetMapping("/configs/{vipType}")
    public Result<VipConfig> getVipConfigByType(@PathVariable("vipType") Integer vipType) {
        VipConfig config = vipConfigService.getVipConfigByType(vipType);
        if (config == null) {
            return Result.error("VIP配置不存在");
        }
        return Result.success(config);
    }

    /**
     * 获取VIP价格信息
     */
    @GetMapping("/price-info/{vipType}")
    public Result<Map<String, Object>> getVipPriceInfo(@PathVariable("vipType") Integer vipType) {
        Map<String, Object> priceInfo = vipConfigService.getVipPriceInfo(vipType);
        if (priceInfo == null) {
            return Result.error("VIP配置不存在");
        }
        return Result.success(priceInfo);
    }

    /**
     * 获取VIP权益详情
     */
    @GetMapping("/benefits/{vipType}")
    public Result<Map<String, Object>> getVipBenefits(@PathVariable("vipType") Integer vipType) {
        Map<String, Object> benefits = vipConfigService.getVipBenefits(vipType);
        return Result.success(benefits);
    }

    /**
     * 添加VIP配置（管理后台用）
     */
    @PostMapping("/configs")
    public Result<String> addVipConfig(@RequestBody @Valid VipConfig vipConfig) {
        boolean success = vipConfigService.addVipConfig(vipConfig);
        if (success) {
            return Result.success("VIP配置添加成功");
        } else {
            return Result.error("VIP配置添加失败，可能VIP类型已存在");
        }
    }

    /**
     * 更新VIP配置（管理后台用）
     */
    @PutMapping("/configs")
    public Result<String> updateVipConfig(@RequestBody @Valid VipConfig vipConfig) {
        boolean success = vipConfigService.updateVipConfig(vipConfig);
        if (success) {
            return Result.success("VIP配置更新成功");
        } else {
            return Result.error("VIP配置更新失败");
        }
    }

    /**
     * 删除VIP配置（管理后台用）
     */
    @DeleteMapping("/configs/{id}")
    public Result<String> deleteVipConfig(@PathVariable("id") Integer id) {
        boolean success = vipConfigService.deleteVipConfig(id);
        if (success) {
            return Result.success("VIP配置删除成功");
        } else {
            return Result.error("VIP配置删除失败");
        }
    }

    /**
     * 启用/禁用VIP配置（管理后台用）
     */
    @PatchMapping("/configs/{id}/status")
    public Result<String> toggleVipConfigStatus(@PathVariable("id") Integer id,
                                                @RequestParam("isActive") Boolean isActive) {
        boolean success = vipConfigService.toggleVipConfigStatus(id, isActive);
        if (success) {
            String message = isActive ? "启用" : "禁用";
            return Result.success("VIP配置" + message + "成功");
        } else {
            return Result.error("操作失败");
        }
    }

}