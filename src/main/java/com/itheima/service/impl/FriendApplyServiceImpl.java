package com.itheima.service.impl;

import com.itheima.common.CacheConstant;
import com.itheima.common.RelationConstant;
import com.itheima.mapper.FriendApplyMapper;
import com.itheima.mapper.FriendRelationMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.FriendApply;
import com.itheima.pojo.FriendRelation;
import com.itheima.pojo.User;
import com.itheima.service.FriendApplyService;
import com.itheima.service.NotificationService;
import com.itheima.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendApplyServiceImpl implements FriendApplyService {

    private final FriendApplyMapper friendApplyMapper;

    private final FriendRelationMapper friendRelationMapper;

    private final UserMapper userMapper;

    private final UserService userService;

    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheConstant.CACHE_PENDING_APPLY_COUNT,
            CacheConstant.CACHE_FRIEND, CacheConstant.CACHE_FRIEND_COUNT},
            allEntries = true)
    public boolean sendFriendApply(Integer applicantId, Integer receiverId, String applyMsg) {
        // 检查用户是否存在
        User applicant = userMapper.findUserById(applicantId);
        User receiver = userMapper.findUserById(receiverId);
        if (applicant == null || receiver == null) {
            throw new RuntimeException("用户不存在");
        }

        // 不能添加自己为好友
        if (applicantId.equals(receiverId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查是否已经是好友
        if (friendRelationMapper.isFriend(applicantId, receiverId) > 0) {
            throw new RuntimeException("已经是好友了");
        }

        // 检查是否有待处理的申请
        if (friendApplyMapper.existsPendingApply(applicantId, receiverId) > 0) {
            throw new RuntimeException("已发送过好友申请，请等待对方处理");
        }

        // 检查对方是否已发送申请
        if (friendApplyMapper.existsPendingApply(receiverId, applicantId) > 0) {
            throw new RuntimeException("对方已向您发送好友申请，请先处理");
        }

        // 检查好友数量限制
        int friendCount = friendRelationMapper.countFriends(applicantId, null, null);
        User applicantInfo = userMapper.findUserById(applicantId);
        int maxFriends = applicantInfo.isVip() ?
                RelationConstant.MAX_FRIENDS_VIP : RelationConstant.MAX_FRIENDS_NORMAL;

        if (friendCount >= maxFriends) {
            throw new RuntimeException("您的好友数量已达上限");
        }

        // 检查对方好友数量限制
        int receiverFriendCount = friendRelationMapper.countFriends(receiverId, null, null);
        if (receiverFriendCount >= maxFriends) {
            throw new RuntimeException("对方好友数量已达上限");
        }

        // 创建好友申请
        FriendApply apply = new FriendApply();
        apply.setApplicantId(applicantId);
        apply.setReceiverId(receiverId);
        apply.setApplyMsg(applyMsg);
        apply.setStatus(RelationConstant.APPLY_STATUS_PENDING);
        apply.setExpireTime(LocalDateTime.now().plusDays(RelationConstant.APPLY_EXPIRE_DAYS));

        int result = friendApplyMapper.insert(apply);

        if (result > 0) {
            // 发送通知
            notificationService.sendFriendApplyNotification(applicantId, receiverId, applyMsg);
            log.info("用户 {} 向用户 {} 发送了好友申请", applicantId, receiverId);
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheConstant.CACHE_PENDING_APPLY_COUNT,
            CacheConstant.CACHE_FRIEND, CacheConstant.CACHE_FRIEND_COUNT},
            allEntries = true)
    public boolean handleFriendApply(Long applyId, Integer receiverId, Boolean accept, String replyMsg) {
        FriendApply apply = friendApplyMapper.selectById(applyId);

        if (apply == null) {
            throw new RuntimeException("申请不存在");
        }

        // 检查权限
        if (!apply.getReceiverId().equals(receiverId)) {
            throw new RuntimeException("无权处理此申请");
        }

        // 检查状态
        if (!RelationConstant.APPLY_STATUS_PENDING.equals(apply.getStatus())) {
            throw new RuntimeException("申请已处理");
        }

        String newStatus = accept ?
                RelationConstant.APPLY_STATUS_ACCEPTED :
                RelationConstant.APPLY_STATUS_REJECTED;

        int result = friendApplyMapper.updateStatus(applyId, newStatus);

        if (result > 0) {
            if (accept) {
                // 建立双向好友关系
                establishFriendship(apply.getApplicantId(), apply.getReceiverId());
            }

            // 发送处理结果通知
            notificationService.sendFriendApplyResultNotification(
                    apply.getReceiverId(), apply.getApplicantId(), accept, replyMsg);

            log.info("用户 {} {} 了用户 {} 的好友申请",
                    receiverId, accept ? "接受" : "拒绝", apply.getApplicantId());

            return true;
        }

        return false;
    }

    /**
     * 建立好友关系（双向）
     */
    private void establishFriendship(Integer userId1, Integer userId2) {
        // 建立 user1 -> user2 的关系
        FriendRelation relation1 = new FriendRelation();
        relation1.setUserId(userId1);
        relation1.setFriendId(userId2);
        friendRelationMapper.insert(relation1);

        // 建立 user2 -> user1 的关系
        FriendRelation relation2 = new FriendRelation();
        relation2.setUserId(userId2);
        relation2.setFriendId(userId1);
        friendRelationMapper.insert(relation2);
        // 更新用户的好友数统计
        updateUserFriendStats(userId1);
        updateUserFriendStats(userId2);

        log.info("用户 {} 和用户 {} 成为好友", userId1, userId2);
    }

    @Override
    public List<FriendApply> getMyApplies(Integer userId, String status, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return friendApplyMapper.selectMyApplies(userId, status, offset, pageSize);
    }

    @Override
    public List<FriendApply> getReceivedApplies(Integer userId, String status, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return friendApplyMapper.selectReceivedApplies(userId, status, offset, pageSize);
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.CACHE_PENDING_APPLY_COUNT,
            key = "T(com.itheima.common.CacheConstant).getPendingApplyCountKey(#userId)")
    public int getPendingApplyCount(Integer userId) {
        List<FriendApply> applies = friendApplyMapper.selectReceivedApplies(userId,
                RelationConstant.APPLY_STATUS_PENDING, 0, Integer.MAX_VALUE);
        return applies.size();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstant.CACHE_PENDING_APPLY_COUNT, allEntries = true)
    public int cleanupExpiredApplies() {
        LocalDateTime now = LocalDateTime.now();
        return friendApplyMapper.updateExpiredApplies(now);
    }

    /**
     * 更新用户好友统计
     */
    private void updateUserFriendStats(Integer userId) {
        int friendCount = friendRelationMapper.countFriends(userId, null, null);
        userMapper.updateFollowCount(userId, friendCount); // 复用follow_count字段存储好友数
    }
}