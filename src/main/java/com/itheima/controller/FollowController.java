package com.itheima.controller;

import com.itheima.dto.FollowStatusDTO;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.FollowService;
import com.itheima.vo.FollowListVO;
import com.itheima.vo.FollowRelationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "关注管理", description = "关注、粉丝、互关等关系管理")
@Validated
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final HttpServletRequest request;

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        // 这里简化处理，实际应从token或session中获取
        return (User) request.getAttribute("currentUser");
    }

    @Operation(summary = "关注用户")
    @PostMapping("/{followingId}")
    public Result<Boolean> follow(
            @Parameter(description = "被关注用户ID") @PathVariable Integer followingId,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        User currentUser = getCurrentUser();
        boolean success = followService.followUser(currentUser.getId(), followingId, remark);
        return success ? Result.success() : Result.error("关注失败");
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{followingId}")
    public Result<Boolean> unfollow(
            @Parameter(description = "被关注用户ID") @PathVariable Integer followingId) {
        User currentUser = getCurrentUser();
        boolean success = followService.unfollowUser(currentUser.getId(), followingId);
        return success ? Result.success() : Result.error("取关失败");
    }

    @Operation(summary = "设置为特别关注")
    @PutMapping("/special/{followingId}")
    public Result<Boolean> setSpecialFollow(
            @Parameter(description = "被关注用户ID") @PathVariable Integer followingId) {
        User currentUser = getCurrentUser();
        boolean success = followService.setSpecialFollow(currentUser.getId(), followingId);
        return success ? Result.success() : Result.error("设置失败");
    }

    @Operation(summary = "获取关注列表")
    @GetMapping("/following")
    public Result<FollowListVO> getFollowingList(
            @Parameter(description = "关注类型：normal-普通关注，special-特别关注")
            @RequestParam(required = false) String relationType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {
        User currentUser = getCurrentUser();
        FollowListVO list = followService.getFollowingList(currentUser.getId(), relationType, page, pageSize);
        return Result.success(list);
    }

    @Operation(summary = "获取粉丝列表")
    @GetMapping("/followers")
    public Result<FollowListVO> getFollowerList(
            @Parameter(description = "关注类型：normal-普通粉丝，special-特别粉丝")
            @RequestParam(required = false) String relationType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {
        User currentUser = getCurrentUser();
        FollowListVO list = followService.getFollowerList(currentUser.getId(), relationType, page, pageSize);
        return Result.success(list);
    }

    @Operation(summary = "获取互关列表")
    @GetMapping("/mutual")
    public Result<FollowListVO> getMutualFollowList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {
        User currentUser = getCurrentUser();
        FollowListVO list = followService.getMutualFollowList(currentUser.getId(), page, pageSize);
        return Result.success(list);
    }

    @Operation(summary = "检查关注状态")
    @GetMapping("/status/{targetUserId}")
    public Result<FollowRelationVO> checkFollowStatus(
            @Parameter(description = "目标用户ID") @PathVariable Integer targetUserId) {
        User currentUser = getCurrentUser();
        FollowRelationVO status = followService.checkFollowStatus(currentUser.getId(), targetUserId);
        return Result.success(status);
    }

    @Operation(summary = "批量检查关注状态")
    @PostMapping("/status/batch")
    public Result<List<FollowStatusDTO>> batchCheckFollowStatus(
            @Parameter(description = "目标用户ID列表") @RequestBody List<Integer> targetUserIds) {
        User currentUser = getCurrentUser();
        List<FollowStatusDTO> statusList = followService.batchCheckFollowStatus(currentUser.getId(), targetUserIds);
        return Result.success(statusList);
    }

    @Operation(summary = "获取关注数量")
    @GetMapping("/following/count")
    public Result<Integer> getFollowingCount() {
        User currentUser = getCurrentUser();
        int count = followService.getFollowingCount(currentUser.getId());
        return Result.success(count);
    }

    @Operation(summary = "获取粉丝数量")
    @GetMapping("/followers/count")
    public Result<Integer> getFollowerCount() {
        User currentUser = getCurrentUser();
        int count = followService.getFollowerCount(currentUser.getId());
        return Result.success(count);
    }
}