package com.itheima.service;

import com.itheima.pojo.FriendApply;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface FriendApplyService {

    boolean sendFriendApply(Integer applicantId, Integer receiverId, String applyMsg);

    boolean handleFriendApply(Long applyId, Integer receiverId, Boolean accept, String replyMsg);

    List<FriendApply> getMyApplies(Integer userId, String status, Integer page, Integer pageSize);

    List<FriendApply> getReceivedApplies(Integer userId, String status, Integer page, Integer pageSize);

    @Cacheable(value = "pending_apply_count", key = "#userId")
    int getPendingApplyCount(Integer userId);

    int cleanupExpiredApplies();
}