package com.itheima.controller;

import com.itheima.dto.*;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.pojo.Notification;
import com.itheima.service.SocialService;
import com.itheima.service.impl.FileUploadService;
import com.itheima.service.impl.MessageStatusService;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final MessageStatusService messageStatusService;
    private final FileUploadService fileUploadService;

    /**
     * 获取社交信息汇总
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getSocialInfo() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getSocialInfo(userId);
    }

    /**
     * 发送好友申请
     */
    @PostMapping("/friend/apply")
    public Result sendFriendApply(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer receiverId = (Integer) params.get("receiverId");
        String applyMsg = (String) params.get("applyMsg");
        return socialService.sendFriendApply(userId, receiverId, applyMsg);
    }

    /**
     * 获取所有收到的好友申请
     */
    @GetMapping("/friend/applies")
    public Result<List<FriendApplyDTO>> getPendingApplies() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getPendingApplies(userId);
    }

    /**
     * 获取所有我发出的好友申请请求
     */
    @GetMapping("/friend/myApplies")
    public Result<List<FriendApplyDTO>> getMyAllApplies() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getMyAllApplies(userId);
    }

    /**
     * 接受好友申请
     */
    @PostMapping("/friend/accept/{applyId}")
    public Result acceptFriendApply(@PathVariable("applyId") Long applyId) {
        return socialService.acceptFriendApply(applyId);
    }

    /**
     * 拒绝好友申请
     */
    @PostMapping("/friend/reject/{applyId}")
    public Result rejectFriendApply(@PathVariable("applyId") Long applyId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.rejectFriendApply(applyId, userId);
    }

    /**
     * 取消我的好友申请
     */
    @DeleteMapping("/friend/cancel/{applyId}")
    public Result cancelFriendApply(@PathVariable("applyId") Long applyId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.cancelFriendApply(applyId, userId);
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/friends")
    public Result<List<FriendRelationDTO>> getFriendList() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getFriendList(userId);
    }

    /**
     * 修改好友备注
     */
    @PostMapping("/friend/remark")
    public Result updateFriendRemark(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        // 检查参数
        if (!params.containsKey("friendId")) {
            System.out.println("缺少friendId参数");
            return Result.error("缺少必要参数");
        }

        try {
            Integer friendId = Integer.parseInt(params.get("friendId").toString());
            String remark = params.get("remark") != null ? params.get("remark").toString() : "";

            if (friendId == null || remark == null || remark.isEmpty()) {
                return Result.error("参数不能为空");
            }
            return socialService.updateFriendRemark(userId, friendId, remark);
        } catch (Exception e) {
            System.out.println("参数解析错误: " + e.getMessage());
            return Result.error("参数格式错误");
        }
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/friend/{friendId}")
    public Result deleteFriend(@PathVariable("friendId") Integer friendId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.deleteFriend(userId, friendId);
    }

    /**
     * 拉黑/取消拉黑好友
     */
    @PutMapping("/friend/block")
    public Result blockFriend(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer friendId = (Integer) params.get("friendId");
        Boolean isBlocked = (Boolean) params.get("isBlocked");
        return socialService.blockFriend(userId, friendId, isBlocked);
    }


    /**
     * 获取黑名单列表
     */
@GetMapping("/blacklist")
    public Result<List<FriendRelationDTO>> getBlacklist() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getBlacklist(userId);
    }

    /**
     * 获取好友分组列表
     */
    @GetMapping("/groups")
    public Result<List<FriendGroupDTO>> getFriendGroups() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getFriendGroups(userId);
    }

    /**
     * 创建好友分组
     */
    @PostMapping("/groups")
    public Result<Void> createFriendGroup(@RequestBody FriendGroupDTO groupDTO) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        groupDTO.setUserId(userId);
        return socialService.createFriendGroup(groupDTO);
    }

    /**
     * 更新好友分组
     */
    @PutMapping("/groups/{groupId}")
    public Result<Void> updateFriendGroup(@PathVariable("groupId") Long groupId, @RequestBody FriendGroupDTO groupDTO) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        groupDTO.setId(groupId);
        groupDTO.setUserId(userId);
        return socialService.updateFriendGroup(groupDTO);
    }

    /**
     * 删除好友分组
     */
    @DeleteMapping("/groups/{groupId}")
    public Result<Void> deleteFriendGroup(@PathVariable("groupId") Long groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.deleteFriendGroup(groupId, userId);
    }

    /**
     * 获取分组详情
     */
    @GetMapping("/groups/{groupId}")
    public Result<FriendGroupDTO> getFriendGroup(@PathVariable("groupId") Long groupId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getFriendGroup(groupId, userId);
    }

    /**
     * 添加好友到分组
     */
    @PostMapping("/friends/{friendId}/group")
    public Result<Void> addFriendToGroup(@PathVariable("friendId") Integer friendId, @RequestParam("groupName") String groupName) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.addFriendToGroup(userId, friendId, groupName);
    }

    /**
     * 从分组移除好友
     */
    @DeleteMapping("/friends/{friendId}/group")
    public Result<Void> removeFriendFromGroup(@PathVariable("friendId") Integer friendId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.removeFriendFromGroup(userId, friendId);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/user/{userId}/detail")
    public Result<UserSimpleDTO> getUserDetail(@PathVariable("userId") Integer userId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");
        return socialService.getUserDetail(userId, currentUserId);
    }

    /**
     * 关注用户
     */
    @PostMapping("/follow/{followingId}")
    public Result followUser(@PathVariable("followingId") Integer followingId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.followUser(userId, followingId);
    }

    /**
     * 获取关注列表
     */
    @GetMapping("/following")
    public Result<List<FollowRelationDTO>> getFollowingList() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getFollowingList(userId);
    }

    /**
     * 获取粉丝列表
     */
    @GetMapping("/follower")
    public Result<List<FollowRelationDTO>> getFollowerList() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getFollowerList(userId);
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/follow/{followingId}")
    public Result unfollowUser(@PathVariable("followingId") Integer followingId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.unfollowUser(userId, followingId);
    }

    /**
     * 发送消息
     */
    @PostMapping("/message/send")
    public Result sendMessage(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer receiverId = (Integer) params.get("receiverId");
        String content = (String) params.get("content");
        String chatType = (String) params.get("chatType");
        String contentType = (String) params.get("contentType");
        String messageId = (String) params.get("messageId");
        Map<String, Object> extraData = (Map<String, Object>) params.get("extraData");
        return socialService.sendMessage(userId, receiverId, content, chatType, contentType, messageId, extraData);
    }

    /**
     * 根据messageId删除消息
     */
    @DeleteMapping("/message/delete/{messageId}")
    public Result deleteMessage(@PathVariable("messageId") String messageId) {
       Map<String, Object> claims = ThreadLocalUtil.get();
       Integer userId = (Integer) claims.get("id");
       return socialService.deleteMessage(userId,messageId);
    }

    /**
     * 获取通知列表
     */
    @GetMapping("/notifications")
    public Result<List<Notification>> getNotifications(
            @RequestParam(defaultValue = "1", name = "pageNum") Integer pageNum,
            @RequestParam(defaultValue = "20", name = "pageSize") Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getNotifications(userId, pageNum, pageSize);
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/notifications/unread/count")
    public Result<Integer> getUnreadCount() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getUnreadCount(userId);
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/notification/read/{notificationId}")
    public Result markAsRead(@PathVariable("notificationId") Integer notificationId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.markAsRead(notificationId, userId);
    }

    /**
     * 获取最近聊天列表
     */
    @GetMapping("/chats/recent")
    public Result<List<RecentChatDTO>> getRecentChats() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getRecentChats(userId);
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/chats/history/{relatedUserId}")
    public Result<List<ChatMessageDTO>> getChatHistory(
            @PathVariable("relatedUserId") Integer relatedUserId,
            @RequestParam(required = false,name = "lastMessageId") String lastMessageId,
            @RequestParam(required = false,defaultValue = "20", name = "pageSize") Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.getChatHistory(userId, relatedUserId,lastMessageId, pageSize);
    }


    /**
     * 修改好友分组
     */
    @PutMapping("/friend/group")
    public Result updateFriendGroup(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer friendId = (Integer) params.get("friendId");
        String groupName = (String) params.get("groupName");
        return socialService.updateFriendGroup(userId, friendId, groupName);
    }

    /**
     * 设为星标好友
     */
    @PutMapping("/friend/star")
    public Result starFriend(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer friendId = (Integer) params.get("friendId");
        Boolean isStarred = (Boolean) params.get("isStarred");
        return socialService.starFriend(userId, friendId, isStarred);
    }

    /**
     * 修改关注备注
     */
    @PutMapping("/follow/remark")
    public Result updateFollowRemark(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Integer followingId = (Integer) params.get("followingId");
        String remark = (String) params.get("remark");
        return socialService.updateFollowRemark(userId, followingId, remark);
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/notifications/read-all")
    public Result markAllAsRead() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.markAllAsRead(userId);
    }

    /**
     * 清空聊天记录
     */
    @DeleteMapping("/chats/history/{friendId}")
    public Result clearChatHistory(@PathVariable("friendId") Integer friendId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return socialService.clearChatHistory(userId, friendId);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search/users")
    public Result<PageBean<UserSimpleDTO>> searchUsers(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "1", name = "pageNum") Integer pageNum,
            @RequestParam(defaultValue = "20", name = "pageSize") Integer pageSize) {
        return socialService.searchUsers(keyword, pageNum, pageSize);
    }


    /**
     * 消息撤回
     */
    @PostMapping("/message/withdraw")
    public Result withdrawMessage(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String messageId = (String) params.get("messageId");
        String reason = (String) params.get("reason");
        return socialService.withdrawMessage(messageId, userId, reason);
    }

    /**
     * 获取消息状态
     */
    @GetMapping("/message/status/{messageId}")
    public Result<String> getMessageStatus(@PathVariable("messageId") String messageId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return messageStatusService.getMessageStatus(messageId, userId);
    }

    /**
     * 更新消息状态
     */
    @PutMapping("/message/status")
    public Result updateMessageStatus(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String messageId = (String) params.get("messageId");
        String status = (String) params.get("status");
        return messageStatusService.updateMessageStatus(messageId, status, userId);
    }

    /**
     * 上传聊天文件
     */
    @PostMapping("/file/upload")
    public Result uploadChatFile(@RequestParam("file") MultipartFile file,
                                 @RequestParam("receiverId") Integer receiverId,
                                 @RequestParam("messageId") String messageId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return fileUploadService.uploadChatFile(file, userId, receiverId, messageId);
    }

    /**
     * 获取聊天文件列表
     */
    @GetMapping("/files")
    public Result<List<ChatFileDTO>> getChatFiles(@RequestParam(required = false,name = "fileType") String fileType,
                               @RequestParam(required = false,defaultValue = "1", name = "pageNum") Integer pageNum,
                               @RequestParam(required = false,defaultValue = "20", name = "pageSize") Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return fileUploadService.getChatFiles(userId, fileType, pageNum, pageSize);
    }

    /**
     * 删除聊天文件
     */
    @DeleteMapping("/file/{fileId}")
    public Result deleteChatFile(@PathVariable("fileId") Long fileId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return fileUploadService.deleteChatFile(fileId, userId);
    }

    /**
     * 批量标记消息为已读
     */
    @PutMapping("/messages/read/{friendId}")
    public Result markMessagesAsRead(@PathVariable("friendId") Integer friendId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return messageStatusService.markMessagesAsRead(userId, friendId);
    }
}