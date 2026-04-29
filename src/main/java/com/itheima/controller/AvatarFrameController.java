package com.itheima.controller;

import com.itheima.dto.AvatarFrameWithStatusDTO;
import com.itheima.pojo.AvatarFrame;
import com.itheima.pojo.Result;
import com.itheima.service.AvatarFrameService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/avatar-frames")
@RequiredArgsConstructor
public class AvatarFrameController {

    private final AvatarFrameService frameService;


    /**
     * 获取用户当前使用的头像框
     */
    @GetMapping("/current")
    public Result<AvatarFrame> getCurrentFrame() {
        Integer currentUserId = (Integer) ThreadLocalUtil.get().get("id");
        AvatarFrame frame = frameService.getCurrentFrame(currentUserId);
        return Result.success(frame);
    }

    /**
     * 根据用户ID获取其他用户当前使用的头像框
     */
    @GetMapping("/other/{userId}")
    public Result<AvatarFrame> getUserCurrentFrame(@PathVariable("userId") Integer userId) {
        AvatarFrame frame = frameService.getCurrentFrame(userId);
        return Result.success(frame);
    }


    //获取所有头像框列表
    @GetMapping("/all-with-status")
    public Result<List<AvatarFrameWithStatusDTO>> getAllFramesWithStatus() {
        // 从ThreadLocalUtil获取当前用户id
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return Result.success(frameService.getAllFramesWithStatus(userId));
    }


    /**
     * 获取用户可用的头像框列表
     * @return 包含状态标记的头像框列表
     */
    @GetMapping("/available")
    public Result<List<AvatarFrame>> getAvailableFrames() {
        // 从ThreadLocalUtil获取当前用户id
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        List<AvatarFrame> frames = frameService.getAvailableFrames(currentUserId);
        return Result.success(frames);
    }

    /**
     * 更换用户头像框
     * @param frameId 要切换的头像框ID
     * @return 操作结果
     */
    @PostMapping("/change/{frameId}")
    public Result<Boolean> changeFrame(@PathVariable("frameId") Integer frameId) {
        // 从ThreadLocalUtil获取当前用户id
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        boolean success = frameService.changeUserFrame(currentUserId, frameId);
        return success ? Result.success() : Result.error("头像框更换失败");
    }

    /**
     * 解锁新头像框（如达到等级时系统自动调用）
     * @param frameId 要解锁的头像框ID
     */
    @PostMapping("/unlock/{frameId}")
    public Result<Void> unlockFrame(@PathVariable("frameId") Integer frameId) {
        // 从ThreadLocalUtil获取当前用户id
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        frameService.unlockFrameForUser(currentUserId, frameId);
        return Result.success();
    }
}

