package com.itheima.controller;

import com.itheima.dto.SystemMessageDTO;
import com.itheima.pojo.Result;
import com.itheima.service.SystemMessageService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统消息控制器
 * <p>
 * 提供系统消息的管理和查询功能，分为管理端和用户端两组接口：
 * 管理端接口用于创建、编辑、删除和停用系统消息；用户端接口用于获取当前活跃的系统消息。
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SystemMessageController {

    private final SystemMessageService systemMessageService;

    // ==================== Admin Endpoints ====================

    /**
     * 创建系统消息（管理员）
     *
     * @param dto 包含系统消息标题、内容、生效时间等信息的 DTO
     * @return 包含创建后的系统消息完整信息的 Result 对象
     */
    @PostMapping("/admin/system-message/send")
    public Result<SystemMessageDTO> createSystemMessage(@RequestBody SystemMessageDTO dto) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return systemMessageService.createSystemMessage(userId, dto);
    }

    /**
     * 获取所有系统消息列表（管理员）
     *
     * @return 包含全部系统消息（包括已停用）列表的 Result 对象
     */
    @GetMapping("/admin/system-message/list")
    public Result<List<SystemMessageDTO>> getAllMessages() {
        return systemMessageService.getAllMessages();
    }

    /**
     * 更新系统消息（管理员）
     *
     * @param messageId 消息ID，作为路径变量传入
     * @param dto       包含要更新的字段（如标题、内容、生效时间等）的 DTO
     * @return 包含更新后系统消息信息的 Result 对象
     */
    @PutMapping("/admin/system-message/{messageId}")
    public Result<SystemMessageDTO> updateSystemMessage(@PathVariable("messageId") String messageId,
                                                         @RequestBody SystemMessageDTO dto) {
        return systemMessageService.updateSystemMessage(messageId, dto);
    }

    /**
     * 删除系统消息（管理员）
     *
     * @param messageId 消息ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @DeleteMapping("/admin/system-message/{messageId}")
    public Result<?> deleteSystemMessage(@PathVariable("messageId") String messageId) {
        return systemMessageService.deleteSystemMessage(messageId);
    }

    /**
     * 停用系统消息（管理员）
     * <p>
     * 将指定系统消息标记为不活跃状态，用户端将不再展示该消息。
     * </p>
     *
     * @param messageId 消息ID，作为路径变量传入
     * @return 操作结果的 Result 对象
     */
    @PutMapping("/admin/system-message/{messageId}/deactivate")
    public Result<?> deactivateSystemMessage(@PathVariable("messageId") String messageId) {
        return systemMessageService.deactivateSystemMessage(messageId);
    }

    // ==================== User Endpoints ====================

    /**
     * 获取当前活跃的系统消息（用户端）
     * <p>
     * 返回当前登录用户可见的所有活跃系统消息。
     * </p>
     *
     * @return 包含活跃系统消息列表的 Result 对象
     */
    @GetMapping("/system-message/active")
    public Result<List<SystemMessageDTO>> getActiveMessages() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return systemMessageService.getActiveMessages(userId);
    }
}
