package com.itheima.controller;

import com.itheima.pojo.FriendApply;
import com.itheima.pojo.FriendRelation;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.FriendApplyService;
import com.itheima.service.FriendRelationService;
import com.itheima.service.impl.FriendRelationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "好友管理", description = "好友申请、好友关系管理")
@Validated
@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendApplyService friendApplyService;

    private final FriendRelationService friendRelationService;

    private final HttpServletRequest request;

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser() {
        return (User) request.getAttribute("currentUser");
    }

    @Operation(summary = "发送好友申请")
    @PostMapping("/apply")
    public Result<Boolean> sendFriendApply(
            @Parameter(description = "接收者ID") @RequestParam Integer receiverId,
            @Parameter(description = "申请消息") @RequestParam(required = false) String applyMsg) {
        User currentUser = getCurrentUser();
        boolean success = friendApplyService.sendFriendApply(currentUser.getId(), receiverId, applyMsg);
        return Result.success(success);
    }

    @Operation(summary = "处理好友申请")
    @PutMapping("/apply/{applyId}")
    public Result<Boolean> handleFriendApply(
            @Parameter(description = "申请ID") @PathVariable Long applyId,
            @Parameter(description = "是否接受") @RequestParam Boolean accept,
            @Parameter(description = "回复消息") @RequestParam(required = false) String replyMsg) {
        User currentUser = getCurrentUser();
        boolean success = friendApplyService.handleFriendApply(applyId, currentUser.getId(), accept, replyMsg);
        return Result.success(success);
    }

    @Operation(summary = "获取我发出的申请")
    @GetMapping("/apply/sent")
    public Result<List<FriendApply>> getMyApplies(
            @Parameter(description = "状态：pending, accepted, rejected, expired")
            @RequestParam(defaultValue = "pending") String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {
        User currentUser = getCurrentUser();
        List<FriendApply> applies = friendApplyService.getMyApplies(currentUser.getId(), status, page, pageSize);
        return Result.success(applies);
    }

    @Operation(summary = "获取我收到的申请")
    @GetMapping("/apply/received")
    public Result<List<FriendApply>> getReceivedApplies(
            @Parameter(description = "状态：pending, accepted, rejected, expired")
            @RequestParam(defaultValue = "pending") String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {
        User currentUser = getCurrentUser();
        List<FriendApply> applies = friendApplyService.getReceivedApplies(currentUser.getId(), status, page, pageSize);
        return Result.success(applies);
    }

    @Operation(summary = "删除好友")
    @DeleteMapping("/{friendId}")
    public Result<Boolean> deleteFriend(
            @Parameter(description = "好友ID") @PathVariable Integer friendId) {
        User currentUser = getCurrentUser();
        boolean success = friendRelationService.deleteFriend(currentUser.getId(), friendId);
        return Result.success(success);
    }

    @Operation(summary = "设置星标好友")
    @PutMapping("/star/{friendId}")
    public Result<Boolean> setStarFriend(
            @Parameter(description = "好友ID") @PathVariable Integer friendId,
            @Parameter(description = "是否星标") @RequestParam Boolean starred) {
        User currentUser = getCurrentUser();
        boolean success = friendRelationService.toggleStarFriend(currentUser.getId(), friendId, starred);
        return Result.success(success);
    }

    @Operation(summary = "设置屏蔽好友")
    @PutMapping("/block/{friendId}")
    public Result<Boolean> setBlockFriend(
            @Parameter(description = "好友ID") @PathVariable Integer friendId,
            @Parameter(description = "是否屏蔽") @RequestParam Boolean blocked) {
        User currentUser = getCurrentUser();
        boolean success = friendRelationService.toggleBlockFriend(currentUser.getId(), friendId, blocked);
        return Result.success(success);
    }

    @Operation(summary = "获取好友列表")
    @GetMapping("/list")
    public Result<FriendRelationServiceImpl.FriendListVO> getFriendList(
            @Parameter(description = "分组名称") @RequestParam(required = false) String groupName,
            @Parameter(description = "是否星标") @RequestParam(required = false) Boolean starred,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "排序方式：time-按时间，interaction-按互动")
            @RequestParam(required = false) String sortType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {
        User currentUser = getCurrentUser();
        FriendRelationServiceImpl.FriendListVO vo = friendRelationService.getFriendList(
                currentUser.getId(), groupName, starred, keyword, sortType, page, pageSize);
        return Result.success(vo);
    }

    @Operation(summary = "获取好友详情")
    @GetMapping("/detail/{friendId}")
    public Result<Object> getFriendDetail(
            @Parameter(description = "好友ID") @PathVariable Integer friendId) {
        User currentUser = getCurrentUser();
        Object detail = friendRelationService.getFriendDetail(currentUser.getId(), friendId);
        return Result.success(detail);
    }

    @Operation(summary = "批量移动好友到分组")
    @PutMapping("/batch/move")
    public Result<Boolean> batchMoveFriends(
            @Parameter(description = "好友ID列表") @RequestBody List<Integer> friendIds,
            @Parameter(description = "新分组名称") @RequestParam @NotBlank String newGroupName) {
        User currentUser = getCurrentUser();
        boolean success = friendRelationService.batchMoveFriendsToGroup(currentUser.getId(), friendIds, newGroupName);
        return Result.success(success);
    }

    @Operation(summary = "搜索好友")
    @GetMapping("/search")
    public Result<List<FriendRelation>> searchFriends(
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        User currentUser = getCurrentUser();
        List<FriendRelation> friends = friendRelationService.searchFriends(currentUser.getId(), keyword);
        return Result.success(friends);
    }

    @Operation(summary = "获取共同好友")
    @GetMapping("/common/{otherUserId}")
    public Result<List<FriendRelation>> getCommonFriends(
            @Parameter(description = "对方用户ID") @PathVariable Integer otherUserId) {
        User currentUser = getCurrentUser();
        List<FriendRelation> commonFriends = friendRelationService.getCommonFriends(currentUser.getId(), otherUserId);
        return Result.success(commonFriends);
    }

    @Operation(summary = "记录互动")
    @PostMapping("/interaction/{friendId}")
    public Result<Void> recordInteraction(
            @Parameter(description = "好友ID") @PathVariable Integer friendId) {
        User currentUser = getCurrentUser();
        friendRelationService.recordInteraction(currentUser.getId(), friendId);
        return Result.success();
    }

    @Operation(summary = "获取分组统计")
    @GetMapping("/group/stats")
    public Result<List<FriendRelationServiceImpl.GroupStatDTO>> getGroupStats() {
        User currentUser = getCurrentUser();
        List<FriendRelationServiceImpl.GroupStatDTO> stats = friendRelationService.getGroupStats(currentUser.getId());
        return Result.success(stats);
    }

    @Operation(summary = "获取待处理申请数量")
    @GetMapping("/apply/pending/count")
    public Result<Integer> getPendingApplyCount() {
        User currentUser = getCurrentUser();
        int count = friendApplyService.getPendingApplyCount(currentUser.getId());
        return Result.success(count);
    }
}