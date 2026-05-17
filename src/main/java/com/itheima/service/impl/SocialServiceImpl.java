package com.itheima.service.impl;


import com.itheima.common.RelationConstant;
import com.itheima.common.UserConstant;
import com.itheima.dto.*;
import com.itheima.mapper.*;
import com.itheima.pojo.*;
import com.itheima.service.impl.SensitiveFilterService;
import com.itheima.service.SocialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final SocialMapper socialMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final SocialCacheService cacheService; // 新增缓存服务
    private final RateLimitService rateLimitService; // 新增频率限制服务
    private final SensitiveFilterService sensitiveFilterService; // 新增敏感词过滤服务

    private final FriendRelationMapper friendRelationMapper;

    private final FollowMapper followMapper;

    private final ChatFileMapper chatFileMapper;

    private final AvatarFrameMapper frameMapper;

    private final FriendGroupMapper friendGroupMapper;

    @Override
    @Transactional
    public Result sendFriendApply(Integer applicantId, Integer receiverId, String applyMsg) {
        try {

            //  频率限制检查
            if (!rateLimitService.checkFriendApplyRate(applicantId)) {
                return Result.error("发送好友申请过于频繁，请稍后再试");
            }

            // 1. 检查是否已是好友
            if (socialMapper.checkFriendRelationExists(applicantId, receiverId) > 0) {
                return Result.error("你们已经是好友了");
            }

            // 2. 检查是否有待处理的申请
            if (socialMapper.checkPendingApplyExists(applicantId, receiverId) > 0) {
                return Result.success("已发送过好友申请，请等待对方处理");
            }

            // 3. 检查好友数量限制
            int friendCount = socialMapper.countFriends(applicantId);
            User applicant = userMapper.findUserById(applicantId);
            int maxFriends = applicant.getVipType() > UserConstant.VIP_TYPE_NONE ?
                    RelationConstant.MAX_FRIENDS_VIP : RelationConstant.MAX_FRIENDS_NORMAL;

            if (maxFriends > 0 && friendCount >= maxFriends) {
                return Result.error("好友数量已达上限");
            }

            // 4. 创建好友申请
            FriendApply apply = new FriendApply();
            apply.setApplicantId(applicantId);
            apply.setReceiverId(receiverId);
            apply.setApplyMsg(applyMsg);
            apply.setStatus(RelationConstant.APPLY_STATUS_PENDING);
            apply.setExpireTime(LocalDateTime.now().plusDays(RelationConstant.APPLY_EXPIRE_DAYS));

            socialMapper.insertFriendApply(apply);

            // 5. 发送通知给接收者
            sendNotification(receiverId, applicantId, "friend_apply",
                    "收到好友申请", applyMsg, apply.getId(), "friend_apply");

            return Result.success("好友申请发送成功");

        } catch (Exception e) {
            log.error("发送好友申请失败", e);
            return Result.error("发送好友申请失败");
        }
    }

    @Override
    public Result<List<FriendApplyDTO>> getPendingApplies(Integer userId) {
        // 先尝试从缓存获取
        List<FriendApplyDTO> cachedApplies = cacheService.getCachedFriendApplies(userId);
        if (cachedApplies != null) {
            log.debug("从缓存获取好友申请列表 userId:{}", userId);
            return Result.success(cachedApplies);
        }

        // 缓存不存在，从数据库查询
        List<FriendApplyDTO> applies = socialMapper.selectPendingApplies(userId);
        //遍历获取用户信息给用户头像进行赋值
        for (FriendApplyDTO apply : applies) {
            User user = userMapper.findUserById(apply.getApplicantId());
            //获取用户头像框地址
            // 1. 查询用户当前使用的头像框ID
            Integer frameId = userMapper.selectCurrentFrameId(user.getId());
            AvatarFrame  frame = frameMapper.selectById(frameId);
            apply.setApplicantAvatarFrame(frame.getUrl());
        }

        // 存入缓存
        cacheService.cacheFriendApplies(userId, applies);

        return Result.success(applies);
    }

    @Override
    public Result<List<FriendApplyDTO>> getMyAllApplies(Integer userId) {
        //从数据库查询
        List<FriendApplyDTO> applies = socialMapper.selectMyAllApplies(userId);
        //遍历获取用户信息给用户头像进行赋值
        for (FriendApplyDTO apply : applies) {
            User user = userMapper.findUserById(apply.getReceiverId());
            //获取用户头像框地址
            // 1. 查询用户当前使用的头像框ID
            Integer frameId = userMapper.selectCurrentFrameId(user.getId());
            AvatarFrame  frame = frameMapper.selectById(frameId);
            apply.setReceiverAvatarFrame(frame.getUrl());
        }

        return Result.success(applies);
    }

    @Override
    @Transactional
    public Result acceptFriendApply(Long applyId) {
        try {
            FriendApply apply = socialMapper.selectFriendApplyById(applyId);
            if (apply == null) {
                return Result.error("申请不存在或无权操作");
            }

            if (!RelationConstant.APPLY_STATUS_PENDING.equals(apply.getStatus())) {
                return Result.error("申请已处理");
            }

            // 1. 更新申请状态
            socialMapper.updateFriendApplyStatus(applyId, RelationConstant.APPLY_STATUS_ACCEPTED);

            // 2. 检查双方是否都还有好友名额
            User applicant = userMapper.findUserById(apply.getApplicantId());
            User receiver = userMapper.findUserById(apply.getReceiverId());

            int applicantFriendCount = socialMapper.countFriends(apply.getApplicantId());
            int receiverFriendCount = socialMapper.countFriends(apply.getReceiverId());

            int applicantMaxFriends = applicant.getVipType() > UserConstant.VIP_TYPE_NONE ?
                    RelationConstant.MAX_FRIENDS_VIP : RelationConstant.MAX_FRIENDS_NORMAL;
            int receiverMaxFriends = receiver.getVipType() > UserConstant.VIP_TYPE_NONE ?
                    RelationConstant.MAX_FRIENDS_VIP : RelationConstant.MAX_FRIENDS_NORMAL;

            if ((applicantMaxFriends > 0 && applicantFriendCount >= applicantMaxFriends) ||
                    (receiverMaxFriends > 0 && receiverFriendCount >= receiverMaxFriends)) {
                return Result.error("好友数量已达上限");
            }

            // 3. 建立双向好友关系
            //判断是否已经关注对方
            Follow follow1 = followMapper.selectByBothIds(apply.getReceiverId(),apply.getApplicantId());
            //判断对方是否已经关注自己
            Follow follow2 = followMapper.selectByBothIds(apply.getApplicantId(),apply.getReceiverId());
            FriendRelation relation1 = new FriendRelation();
            relation1.setUserId(apply.getReceiverId());
            relation1.setFriendId(apply.getApplicantId());
            relation1.setRelationType("friend");

            socialMapper.insertFriendRelation(relation1);
            FriendRelation relation2 = new FriendRelation();
            relation2.setUserId(apply.getApplicantId());
            relation2.setFriendId(apply.getReceiverId());
            relation2.setRelationType("friend");
            if (follow1 != null && follow2 != null) {
                follow1.setRelationType(RelationConstant.FOLLOW_TYPE_SPECIAL);
                follow2.setRelationType(RelationConstant.FOLLOW_TYPE_SPECIAL);
            }
            socialMapper.insertFriendRelation(relation2);

            // 5. 发送通知给申请者
            sendNotification(apply.getApplicantId(), apply.getReceiverId(), "friend_accept",
                    "好友申请已通过", "你们现在已经是好友了，开始聊天吧！",
                    applyId, "friend_apply");

            // 清除相关缓存
            cacheService.evictFriendAppliesCache(apply.getReceiverId());
            cacheService.evictFriendsCache(apply.getReceiverId());
            cacheService.evictFriendsCache(apply.getApplicantId());

            return Result.success("好友申请已接受");

        } catch (Exception e) {
            log.error("接受好友申请失败", e);
            throw new RuntimeException("接受好友申请失败");
        }
    }

    @Override
    @Transactional
    public Result rejectFriendApply(Long applyId, Integer userId) {
        FriendApply apply = socialMapper.selectFriendApplyById(applyId);
        if (apply == null || !apply.getReceiverId().equals(userId)) {
            return Result.error("申请不存在或无权操作");
        }

        socialMapper.updateFriendApplyStatus(applyId, RelationConstant.APPLY_STATUS_REJECTED);

        // 发送通知给申请者
        sendNotification(apply.getApplicantId(), userId, "friend_reject",
                "好友申请被拒绝", "对方拒绝了你的好友申请",
                applyId, "friend_apply");

        // 清除相关缓存
        cacheService.evictFriendAppliesCache(userId);

        return Result.success("好友申请已拒绝");
    }

    @Override
    public Result cancelFriendApply(Long applyId, Integer userId) {
        FriendApply apply = socialMapper.selectFriendApplyById(applyId);
        if (apply == null || !apply.getApplicantId().equals(userId)) {
            return Result.error("申请不存在或无权操作");
        }

        socialMapper.deleteFriendApply(applyId);

        // 清除相关缓存
        cacheService.evictFriendAppliesCache(userId);

        return Result.success("好友申请已撤回");
    }

    @Override
    public Result<List<FriendRelationDTO>> getFriendList(Integer userId) {
        // 先尝试从缓存获取
        List<FriendRelationDTO> cachedFriends = cacheService.getCachedFriends(userId);
        if (cachedFriends != null) {
            log.debug("从缓存获取好友列表 userId:{}", userId);
            return Result.success(cachedFriends);
        }

        // 缓存不存在，从数据库查询
        List<FriendRelationDTO> friends = socialMapper.selectFriendsByUserId(userId);
        //遍历获取用户信息进行赋值
        for (FriendRelationDTO friend : friends) {
            User user = userMapper.findUserById(friend.getFriendId());
            //获取用户头像框地址
            // 1. 查询用户当前使用的头像框ID
            Integer frameId = userMapper.selectCurrentFrameId(user.getId());
            AvatarFrame  frame = frameMapper.selectById(frameId);

            //判断是否关注该用户
            Follow follow = followMapper.selectByBothIds(userId, friend.getFriendId());
            if (follow != null) {
                friend.setIsFollowing(true);
            } else {
                friend.setIsFollowing(false);
            }
            //获取好友关系信息
            FriendRelation relation = socialMapper.selectFriendRelation(userId, friend.getFriendId());
            //获取与目标用户之间未读消息的数量
            Integer unreadCount = cacheService.getCachedUnreadCount(userId, friend.getFriendId());
            if (unreadCount == null) {
                unreadCount = socialMapper.selectUnreadCountWithUser(userId, friend.getFriendId());
                cacheService.cacheUnreadCount(userId, friend.getFriendId(), unreadCount);
            }
            //获取与目标用户之间最后一条消息的内容
            Map<String, Object> lastMessage = socialMapper.selectLastMessageContent(userId, friend.getFriendId());
            if (lastMessage != null) {
                String content = (String) lastMessage.get("content");
                String contentType = (String) lastMessage.get("content_type");
                friend.setLastMessage(formatLastMessage(content, contentType));
            } else {
                friend.setLastMessage("暂时还没有消息，开始聊天吧");
            }
            friend.setUnreadCount(unreadCount);
            friend.setFriendId(user.getId());
            friend.setFriendUsername(user.getUsername());
            friend.setFriendNickname(user.getNickname());
            friend.setFriendAvatar(user.getAvatar());
            friend.setFriendAvatarFrame(frame.getUrl());
            friend.setIsOnline(user.getIsOnline());
            friend.setGroupName(relation.getGroupName());
            friend.setRemark(relation.getRemark());
            friend.setIsStarred(relation.getIsStarred());
            friend.setIsBlocked(relation.getIsBlocked());
            friend.setBecomeFriendTime(relation.getBecomeFriendTime());
            friend.setLastInteractionTime(relation.getLastInteractionTime());
        }

        // 存入缓存
        cacheService.cacheFriends(userId, friends);

        return Result.success(friends);
    }


    @Override
    public Result<List<FriendRelationDTO>> getBlacklist(Integer userId) {
        List<FriendRelationDTO> friends = socialMapper.selectBlacklistByUserId(userId);
        //遍历获取用户信息进行赋值
        for (FriendRelationDTO friend : friends) {
            User user = userMapper.findUserById(friend.getFriendId());
            //获取用户头像框地址
            // 1. 查询用户当前使用的头像框ID
            Integer frameId = userMapper.selectCurrentFrameId(user.getId());
            AvatarFrame frame = frameMapper.selectById(frameId);

            //获取好友关系信息
            FriendRelation relation = socialMapper.selectFriendRelation(userId, friend.getFriendId());
            //获取与目标用户之间未读消息的数量
            Integer unreadCount = cacheService.getCachedUnreadCount(userId, friend.getFriendId());
            if (unreadCount == null) {
                unreadCount = socialMapper.selectUnreadCountWithUser(userId, friend.getFriendId());
                cacheService.cacheUnreadCount(userId, friend.getFriendId(), unreadCount);
            }
            //获取与目标用户之间最后一条消息的内容
            Map<String, Object> lastMessage = socialMapper.selectLastMessageContent(userId, friend.getFriendId());
            if (lastMessage != null) {
                String content = (String) lastMessage.get("content");
                String contentType = (String) lastMessage.get("content_type");
                friend.setLastMessage(formatLastMessage(content, contentType));
            } else {
                friend.setLastMessage("暂时还没有消息，开始聊天吧");
            }
            friend.setUnreadCount(unreadCount);
            friend.setFriendId(user.getId());
            friend.setFriendUsername(user.getUsername());
            friend.setFriendNickname(user.getNickname());
            friend.setFriendAvatar(user.getAvatar());
            friend.setFriendAvatarFrame(frame.getUrl());
            friend.setIsOnline(user.getIsOnline());
            friend.setGroupName(relation.getGroupName());
            friend.setRemark(relation.getRemark());
            friend.setRelationType(relation.getRelationType());
            friend.setIsStarred(relation.getIsStarred());
            friend.setIsBlocked(relation.getIsBlocked());
            friend.setBecomeFriendTime(relation.getBecomeFriendTime());
        }
        return Result.success(friends);
    }

    @Override
    public Result<List<FriendGroupDTO>> getFriendGroups(Integer userId) {
        try {
            List<FriendGroup> groups = friendGroupMapper.findByUserId(userId);
            List<FriendGroupDTO> dtoList = new ArrayList<>();
            for (FriendGroup group : groups) {
                //根据用户id和分组名称获取好友关系信息
                List<FriendRelationDTO> friends = friendRelationMapper.findFriendsByGroup(userId, group.getGroupName());

                for (FriendRelationDTO friend : friends) {
                    //判断是否关注该用户
                    Follow follow = followMapper.selectByBothIds(userId, friend.getFriendId());
                    if (follow != null) {
                        friend.setIsFollowing(true);
                    } else {
                        friend.setIsFollowing(false);
                    }
                    User user = userMapper.findUserById(friend.getFriendId());
                    //获取用户头像框地址
                    // 1. 查询用户当前使用的头像框ID
                    Integer frameId = userMapper.selectCurrentFrameId(user.getId());
                    AvatarFrame  frame = frameMapper.selectById(frameId);
                    friend.setFriendAvatarFrame(frame.getUrl());
                }
                FriendGroupDTO dto = convertToDTO(group);
                dto.setFriendCount(friendRelationMapper.countFriendsInGroup(userId, group.getGroupName()));
                dto.setFriends(friends);
                dtoList.add(dto);
            }
            return Result.success(dtoList);
        } catch (Exception e) {
            log.error("获取好友分组失败", e);
            return Result.error("获取好友分组失败");
        }
    }

    @Override
    public Result<Void> createFriendGroup(FriendGroupDTO groupDTO) {
        try {
            // 检查分组名是否已存在
            FriendGroup exist = friendGroupMapper.findByUserIdAndGroupName(
                    groupDTO.getUserId(), groupDTO.getGroupName());
            if (exist != null) {
                return Result.error("分组名称已存在");
            }

            FriendGroup group = convertToEntity(groupDTO);
            friendGroupMapper.insert(group);
            return Result.success();
        } catch (Exception e) {
            log.error("创建好友分组失败", e);
            return Result.error("创建好友分组失败");
        }
    }

    @Override
    public Result<Void> updateFriendGroup(FriendGroupDTO groupDTO) {
        try {
            FriendGroup group = friendGroupMapper.findByIdAndUserId(groupDTO.getId(), groupDTO.getUserId());
            if (group == null) {
                return Result.error("分组不存在");
            }

            // 如果修改了分组名，检查新名称是否已存在
            if (!group.getGroupName().equals(groupDTO.getGroupName())) {
                FriendGroup exist = friendGroupMapper.findByUserIdAndGroupName(
                        groupDTO.getUserId(), groupDTO.getGroupName());
                if (exist != null) {
                    return Result.error("分组名称已存在");
                }
            }

            group.setGroupName(groupDTO.getGroupName());
            group.setColor(groupDTO.getColor());
            group.setDescription(groupDTO.getDescription());
            group.setSortOrder(groupDTO.getSortOrder());

            friendGroupMapper.update(group);
            return Result.success();
        } catch (Exception e) {
            log.error("更新好友分组失败", e);
            return Result.error("更新好友分组失败");
        }
    }

    @Override
    public Result<Void> deleteFriendGroup(Long groupId, Integer userId) {
        try {
            FriendGroup group = friendGroupMapper.findByIdAndUserId(groupId, userId);
            if (group == null) {
                return Result.error("分组不存在");
            }

            // 将该分组下的所有好友设为未分组
            friendRelationMapper.clearGroupByGroupName(userId, group.getGroupName());

            // 删除分组
            friendGroupMapper.delete(groupId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除好友分组失败", e);
            return Result.error("删除好友分组失败");
        }
    }

    @Override
    public Result<FriendGroupDTO> getFriendGroup(Long groupId, Integer userId) {
        try {
            FriendGroup group = friendGroupMapper.findByIdAndUserId(groupId, userId);
            if (group == null) {
                return Result.error("分组不存在");
            }
            return Result.success(convertToDTO(group));
        } catch (Exception e) {
            log.error("获取分组详情失败", e);
            return Result.error("获取分组详情失败");
        }
    }

    @Override
    public Result<Void> addFriendToGroup(Integer userId, Integer friendId, String groupName) {
        try {
            // 检查分组是否存在
            FriendGroup group = friendGroupMapper.findByUserIdAndGroupName(userId, groupName);
            if (group == null && groupName != null && !groupName.isEmpty()) {
                return Result.error("分组不存在");
            }

            // 更新好友分组
            friendRelationMapper.updateFriendGroup(userId, friendId, groupName);

            // 更新分组的好友数量
            if (group != null) {
                friendRelationMapper.countFriendsInGroup(userId, groupName);
                friendGroupMapper.updateFriendCount(userId, groupName);
            }

            return Result.success();
        } catch (Exception e) {
            log.error("添加好友到分组失败", e);
            return Result.error("添加好友到分组失败");
        }
    }

    @Override
    public Result<Void> removeFriendFromGroup(Integer userId, Integer friendId) {
        try {
            // 获取好友当前分组
            FriendRelation relation = friendRelationMapper.selectFriendDetail(userId, friendId);
            String oldGroupName = relation != null ? relation.getGroupName() : null;

            // 从分组移除
            friendRelationMapper.clearFriendGroup(userId, friendId);

            // 更新原分组的好友数量
            if (oldGroupName != null && !oldGroupName.isEmpty()) {
                friendGroupMapper.updateFriendCount(userId, oldGroupName);
            }

            return Result.success();
        } catch (Exception e) {
            log.error("从分组移除好友失败", e);
            return Result.error("从分组移除好友失败");
        }
    }

    @Override
    public Result<UserSimpleDTO> getUserDetail(Integer userId, Integer currentUserId) {
        try {
            // 查询用户基本信息
            User user = userMapper.findUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            UserSimpleDTO dto = new UserSimpleDTO();
            BeanUtils.copyProperties(user, dto);

            // 设置等级相关
            dto.setNextLevelExp(UserConstant.getNextLevelRequiredExp(user.getLevel()));

            // 设置社交关系
            if (currentUserId != null && !currentUserId.equals(userId)) {
                // 检查是否好友
                FriendRelation friendRelation = friendRelationMapper.selectFriendDetail(currentUserId, userId);
                if (friendRelation != null) {
                   dto.setIsFriend(friendRelation.getRelationType() == "stranger" ? false : true);
                }else {
                    dto.setIsFriend(false);
                }

                // 检查是否关注
                Follow followRelation = followMapper.selectByBothIds(currentUserId, userId);
                dto.setIsFollowed(followRelation != null);
            }

            // 设置统计信息
            dto.setFollowCount(user.getFollowCount());
            dto.setFollowerCount(user.getFollowerCount());

            dto.setIsOnline(user.getIsOnline());
            return Result.success(dto);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error("获取用户详情失败");
        }
    }

    // 辅助方法
    private FriendGroupDTO convertToDTO(FriendGroup group) {
        if (group == null) return null;
        FriendGroupDTO dto = new FriendGroupDTO();
        BeanUtils.copyProperties(group, dto);
        return dto;
    }

    private FriendGroup convertToEntity(FriendGroupDTO dto) {
        if (dto == null) return null;
        FriendGroup group = new FriendGroup();
        BeanUtils.copyProperties(dto, group);
        return group;
    }

    @Override
    public Result<List<FollowRelationDTO>> getFollowingList(Integer userId) {
        // 先尝试从缓存获取
        List<FollowRelationDTO> cachedFollowing = cacheService.getCachedFollowing(userId);
        if (cachedFollowing != null) {
            log.debug("从缓存获取关注列表 userId:{}", userId);
            return Result.success(cachedFollowing);
        }

        // 缓存不存在，从数据库查询
        List<FollowRelationDTO> following = socialMapper.selectFollowingByUserId(userId);
        //遍历获取用户信息进行赋值
        for (FollowRelationDTO follow : following) {
            User user = userMapper.findUserById(follow.getFollowingId());
            //获取用户头像框地址
            // 1. 查询用户当前使用的头像框ID
            Integer frameId = userMapper.selectCurrentFrameId(user.getId());
            AvatarFrame  frame = frameMapper.selectById(frameId);
            //获取follow用户信息
            Follow followData = followMapper.selectByBothIds(userId, follow.getFollowingId());
            //查询与当前用户是否为好友
            follow.setIsFriend(socialMapper.checkFriendRelationExists(userId, follow.getFollowingId()) > 0);
            follow.setFollowerId(userId);
            follow.setFollowingId(user.getId());
            follow.setFollowingUsername(user.getUsername());
            follow.setFollowingNickname(user.getNickname());
            follow.setFollowingAvatar(user.getAvatar());
            follow.setFollowingAvatarFrame(frame.getUrl());
            if (followData != null){
                follow.setRelationType(followData.getRelationType());
                follow.setRemark(followData.getRemark());
                follow.setCreateTime(followData.getCreateTime());
            }
            follow.setFollowing(user);
        }

        // 存入缓存
        cacheService.cacheFollowing(userId, following);

        return Result.success(following);
    }

    @Override
    public Result<List<FollowRelationDTO>> getFollowerList(Integer userId) {
        // 先尝试从缓存获取
        List<FollowRelationDTO> cachedFollowers = cacheService.getCachedFollowers(userId);
        if (cachedFollowers != null) {
            log.debug("从缓存获取粉丝列表 userId:{}", userId);
            return Result.success(cachedFollowers);
        }

        // 缓存不存在，从数据库查询
        List<FollowRelationDTO> followers = socialMapper.selectFollowersByUserId(userId);
        //遍历获取用户信息进行赋值
        for (FollowRelationDTO follower : followers) {
            User user = userMapper.findUserById(follower.getFollowerId());
            //获取用户头像框地址
            // 1. 查询用户当前使用的头像框ID
            Integer frameId = userMapper.selectCurrentFrameId(user.getId());
            AvatarFrame frame = frameMapper.selectById(frameId);
            //获取follow用户信息
            Follow followData = followMapper.selectByBothIds(userId, follower.getFollowerId());
            //查询与当前用户是否为好友
            follower.setIsFriend(socialMapper.checkFriendRelationExists(userId, follower.getFollowerId()) > 0);
            follower.setFollowerId(user.getId());
            follower.setFollowerUsername(user.getUsername());
            follower.setFollowerNickname(user.getNickname());
            follower.setFollowerAvatar(user.getAvatar());
            follower.setFollowerAvatarFrame(frame.getUrl());
            if (followData != null){
                follower.setRelationType(followData.getRelationType());
                follower.setRemark(followData.getRemark());
                follower.setCreateTime(followData.getCreateTime());
                follower.setIsFollowing(true);
            }
            follower.setFollower(user);
        }

        // 存入缓存
        cacheService.cacheFollowers(userId, followers);

        return Result.success(followers);

    }

    @Override
    @Transactional
    public Result sendMessage(Integer senderId, Integer receiverId, String content,
                              String chatType, String contentType, String messageId,
                              Map<String, Object> extraData) {
        try {
            // 1. 频率限制检查
            if (!rateLimitService.checkMessageRate(senderId)) {
                return Result.error("发送消息过于频繁，请稍后再试");
            }

            // 2. 敏感词过滤
            String filteredContent = sensitiveFilterService.filter(content);
            if (filteredContent == null) {
                return Result.error("消息内容包含敏感词，请修改后重新发送");
            }

            List<String> sensitiveWords = sensitiveFilterService.findAllSensitiveWords(content);
            boolean isFiltered = !sensitiveWords.isEmpty();

            // 3. 检查是否被拉黑
            FriendRelation relation = socialMapper.selectFriendRelation(receiverId, senderId);
            if (relation != null && relation.getIsBlocked()) {
                return Result.error("消息发送失败：你已被对方拉黑");
            }

            // 4. 处理文件消息（但排除表情包）
            ChatFile chatFile = null;

            // 判断是否是表情包消息
            boolean isEmojiPackMessage = false;
            if (extraData != null && extraData.containsKey("type")) {
                String extraType = (String) extraData.get("type");
                isEmojiPackMessage = "emoji_pack".equals(extraType);
            }

            if (isFileMessage(contentType) && !isEmojiPackMessage) {
                // 只有普通文件上传才需要处理文件记录
                chatFile = chatFileMapper.selectByMessageId(messageId);
                if (chatFile == null) {
                    chatFile = saveChatFileRecord(senderId, receiverId, extraData, messageId);
                }
            }

            // 5. 创建聊天消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessageId(messageId);
            chatMessage.setSenderId(senderId);
            chatMessage.setReceiverId(receiverId);
            chatMessage.setChatType(chatType);
            chatMessage.setContentType(contentType);
            chatMessage.setContent(filteredContent);

            // 处理扩展数据
            if (extraData != null && !extraData.isEmpty()) {
                chatMessage.setExtraData(objectMapper.writeValueAsString(extraData));

                // 普通文件消息才设置fileId
                if (!isEmojiPackMessage && extraData.get("fileId") != null) {
                    chatMessage.setFileId(Long.parseLong(extraData.get("fileId").toString()));
                }
            }

            chatMessage.setOriginalContent(content);
            chatMessage.setStatus("sent");
            chatMessage.setDeliveredTime(LocalDateTime.now());
            chatMessage.setIsFiltered(isFiltered);
            chatMessage.setFilteredWords(String.join(",", sensitiveWords));

            // 插入聊天消息
            int result = socialMapper.insertChatMessage(chatMessage);
            if (result <= 0) {
                throw new RuntimeException("插入聊天消息失败");
            }

            // 6. 缓存消息状态
            cacheService.cacheMessageStatus(messageId, SocialCacheService.MessageStatus.SENT);

            // 7. 更新未读消息数缓存
            cacheService.incrementUnreadCount(receiverId, senderId);

            // 8. 如果是好友消息，更新最后互动时间
            if ("friend".equals(chatType)) {
                socialMapper.updateLastInteractionTime(senderId, receiverId);
            }

            // 9. 清除缓存
            cacheService.evictRecentChatsCache(senderId);
            cacheService.evictRecentChatsCache(receiverId);
            cacheService.evictAllChatMessagesCache(senderId, receiverId);

            // 10. 返回结果
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("senderId", senderId);
            resultMap.put("receiverId", receiverId);
            resultMap.put("messageId", messageId);
            resultMap.put("content", filteredContent);
            resultMap.put("status", "sent");
            resultMap.put("timestamp", new Date());
            resultMap.put("chatType", chatType);
            resultMap.put("contentType", contentType);
            resultMap.put("filtered", isFiltered);

            if (isFiltered) {
                resultMap.put("filteredWords", sensitiveWords);
            }

            // 添加文件信息（仅普通文件）
            if (chatFile != null) {
                resultMap.put("fileInfo", convertToFileInfo(chatFile));
            }

            // 如果是表情包消息，添加标识
            if (isEmojiPackMessage) {
                resultMap.put("isEmojiPack", true);
                resultMap.put("emojiPackInfo", extraData);
            }

            // 记录日志
            log.info("消息发送成功: sender={}, receiver={}, type={}, filtered={}, isEmojiPack={}",
                    senderId, receiverId, contentType, isFiltered, isEmojiPackMessage);

            // 消息发送成功后清除缓存
            afterMessageSent(senderId, receiverId);

            return Result.success(resultMap);

        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Result.error("消息发送失败: " + e.getMessage());
        }
    }
    @Override
    @Transactional
    public Result deleteMessage(Integer userId, String messageId) {
        // 1. 确认消息是否存在
        ChatMessage chatMessage = socialMapper.selectChatMessageByMessageId(messageId);
        if (chatMessage == null) {
            return Result.error("消息不存在");
        }

        // 2. 检查是否已经删除过
        String deleteStatus = "deleted_by_" + userId;
        Integer count = socialMapper.checkMessageStatusHistory(messageId, deleteStatus);
        if (count > 0) {
            return Result.error("消息已被删除");
        }

        // 3. 记录当前用户的删除操作到历史表
        String reason = "用户删除";
        ChatMessageStatusHistory statusHistory = new ChatMessageStatusHistory();
        statusHistory.setMessageId(messageId);
        statusHistory.setStatus(deleteStatus);
        statusHistory.setReason(reason);
        statusHistory.setOperatorId(userId);
        socialMapper.insertMessageStatusHistory(statusHistory);

        // 4. 检查是否双方都已删除
        int deleteCount = socialMapper.countMessageDeleteStatus(messageId);

// 5. 如果已有两条删除记录（双方都已删除），则真正逻辑删除这条消息
        if (deleteCount >= 2) {
            socialMapper.updateMessageToDeleted(messageId);

            // 可选：记录最终删除状态
            ChatMessageStatusHistory finalDelete = new ChatMessageStatusHistory();
            finalDelete.setMessageId(messageId);
            finalDelete.setStatus("deleted_final");
            finalDelete.setReason("双方均已删除");
            socialMapper.insertMessageStatusHistory(finalDelete);
        }

        // 5. 清除缓存
        cacheService.evictAllChatMessagesCache(userId, chatMessage.getReceiverId());

        return Result.success("消息删除成功");
    }


    @Override
    public Result updateFriendRemark(Integer userId, Integer friendId, String remark) {
        int result = socialMapper.updateFriendRemark(userId, friendId, remark);
        if (result > 0) {
            // 更新缓存
            cacheService.updateFriendRemark(userId, friendId, remark);
            return Result.success("备注修改成功");
        }
        return Result.error("修改失败");
    }

    @Override
    public Result updateFriendGroup(Integer userId, Integer friendId, String groupName) {
        FriendRelation relation = new FriendRelation();
        relation.setUserId(userId);
        relation.setFriendId(friendId);
        relation.setGroupName(groupName);

        int result = socialMapper.updateFriendRelation(relation);
        return result > 0 ? Result.success("分组修改成功") : Result.error("修改失败");
    }

    @Override
    public Result starFriend(Integer userId, Integer friendId, Boolean isStarred) {
        int result = socialMapper.updateStarStatus(userId, friendId, isStarred);
        if (result > 0) {
            // 更新缓存
            cacheService.updateFriendStarStatus(userId, friendId, isStarred);
            return Result.success(isStarred ? "已设为星标好友" : "已取消星标");
        }
        return Result.error("操作失败");
    }

    @Override
    @Transactional
    public Result deleteFriend(Integer userId, Integer friendId) {
        try {
            // 删除双向好友关系
            socialMapper.deleteFriendRelation(userId, friendId);
            socialMapper.deleteFriendRelation(friendId, userId);

            // 发送系统通知
            sendNotification(friendId, userId, "system",
                    "好友关系解除", "对方已解除与你的好友关系",
                    null, "系统通知");
            cacheService.evictFriendsCache(userId);
            return Result.success("好友删除成功");

        } catch (Exception e) {
            log.error("删除好友失败", e);
            return Result.error("删除好友失败");
        }
    }

    @Override
    public Result blockFriend(Integer userId, Integer friendId, Boolean isBlocked) {
        int result = socialMapper.updateBlockStatus(userId, friendId, isBlocked);
       if (result > 0) {
           //查看是否关注了对方，如果是取消关注
           Follow follow = followMapper.selectByBothIds(userId, friendId);
           if (follow != null) {
               followMapper.delete(follow.getFollowerId(), follow.getFollowingId());
           }
           //如果用户是好友则改好友状态为陌生人，有分组清除分组。如果是陌生人则创建陌生人关系的新对象。
           FriendRelation relation = socialMapper.selectFriendRelation(userId, friendId);
           if (relation != null) {
               // 更新现有关系
               relation.setGroupName("");
               // 确保设置了 userId 和 friendId
               relation.setUserId(userId);
               relation.setFriendId(friendId);
               socialMapper.updateFriendRelation(relation);
           } else {
               // 创建新关系
               FriendRelation newRelation = new FriendRelation();
               newRelation.setUserId(userId);
               newRelation.setFriendId(friendId);
               newRelation.setRelationType("stranger");
               newRelation.setGroupName("");
               newRelation.setRemark(""); // 可以设置默认备注
               socialMapper.insertFriendRelation(newRelation);
           }

           // 更新缓存
           cacheService.updateFriendBlockStatus(userId, friendId, isBlocked);
           cacheService.evictAllSocialCache(userId);
           return Result.success(isBlocked ? "已拉黑好友" : "已取消拉黑");
       }
        return Result.error("操作失败");
    }

    @Override
    @Transactional
    public Result followUser(Integer followerId, Integer followingId) {
        try {
            //  频率限制检查
            if (!rateLimitService.checkFollowRate(followerId)) {
                return Result.error("关注操作过于频繁，请稍后再试");
            }
            // 检查是否已关注
            if (socialMapper.checkFollowRelationExists(followerId, followingId) > 0) {
                return Result.error("已关注该用户");
            }

            // 检查关注数量限制
            int followCount = socialMapper.countFollowing(followerId);
            User follower = userMapper.findUserById(followerId);
            int maxFollowing = follower.getVipType() > UserConstant.VIP_TYPE_NONE ?
                    RelationConstant.MAX_FOLLOWING_VIP : RelationConstant.MAX_FOLLOWING_NORMAL;

            if (maxFollowing > 0 && followCount >= maxFollowing) {
                return Result.error("关注数量已达上限");
            }
            //判断是不是好友
            int i = socialMapper.checkFriendRelationExists(followerId, followingId);

            // 创建关注关系
            FollowDTO follow = new FollowDTO();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            if ( i > 0 ) {
                follow.setRelationType(RelationConstant.FOLLOW_TYPE_SPECIAL);
            }else {
                follow.setRelationType(RelationConstant.FOLLOW_TYPE_NORMAL);
            }

            socialMapper.insertUserFollow(follow);

            // 更新关注/粉丝数量
            socialMapper.incrementFollowCount(followerId);
            socialMapper.incrementFollowerCount(followingId);

            // 发送通知给被关注者
            sendNotification(followingId, followerId, "follow",
                    "新关注", "有人关注了你", null, "user");

            //清除缓存
            cacheService.evictFollowingCache(followerId);
            cacheService.evictFollowerCache(followingId);
            return Result.success("关注成功");

        } catch (Exception e) {
            log.error("关注用户失败", e);
            return Result.error("关注失败");
        }
    }


    @Override
    @Transactional
    public Result unfollowUser(Integer followerId, Integer followingId) {
        int result = socialMapper.deleteUserFollow(followerId, followingId);
        if (result > 0) {
            socialMapper.decrementFollowCount(followerId);
            socialMapper.decrementFollowerCount(followingId);
            //清除缓存
            cacheService.evictFollowingCache(followerId);
            cacheService.evictFollowerCache(followingId);
            return Result.success("取消关注成功");
        }
        return Result.error("取消关注失败");
    }

    @Override
    public Result updateFollowRemark(Integer followerId, Integer followingId, String remark) {
        int result = socialMapper.updateFollowRemark(followerId, followingId, remark);
        return result > 0 ? Result.success("备注修改成功") : Result.error("修改失败");
    }


    @Override
    public Result<List<Notification>> getNotifications(Integer userId, Integer pageNum, Integer pageSize) {
        Integer offset = (pageNum - 1) * pageSize;
        List<Notification> notifications = socialMapper.selectNotificationsByUserIdWithPagination(
                userId, offset, pageSize);
        //遍历获取用户信息进行赋值
        for (Notification notification : notifications) {
            User user = userMapper.findUserById(notification.getRelatedUserId());
            notification.setRelatedUser(user);
        }
        return Result.success(notifications);
    }

    @Override
    public Result<Integer> getUnreadCount(Integer userId) {
        int count = socialMapper.selectUnreadCount(userId);
        return Result.success(count);
    }

    @Override
    public Result markAsRead(Integer notificationId, Integer userId) {
        int result = socialMapper.markNotificationAsRead(notificationId, userId);
        return result > 0 ? Result.success("标记为已读") : Result.error("操作失败");
    }

    @Override
    public Result markAllAsRead(Integer userId) {
        int result = socialMapper.markAllNotificationsAsRead(userId);
        return result > 0 ? Result.success("全部标记为已读") : Result.error("操作失败");
    }

    @Override
    public Result<List<RecentChatDTO>> getRecentChats(Integer userId) {
        try {
            // 1. 先尝试从缓存获取
            List<RecentChatDTO> cachedChats = cacheService.getCachedRecentChats(userId);
            if (cachedChats != null && !cachedChats.isEmpty()) {
                log.debug("从缓存获取最近聊天 userId:{}", userId);
                return Result.success(cachedChats);
            }

            // 2. 从chat_message表获取最近聊天列表
            List<RecentChatDTO> recentChats = socialMapper.selectRecentChatsFromChatMessage(userId, 20);

            if (recentChats == null || recentChats.isEmpty()) {
                // 如果没有聊天记录，返回空列表
                cacheService.cacheRecentChats(userId, new ArrayList<>());
                return Result.success(new ArrayList<>());
            }

            // 3. 为每个聊天记录补充信息
            for (RecentChatDTO chat : recentChats) {
                Integer otherUserId = chat.getRelatedUserId();

                // 获取用户信息
                User user = userMapper.findUserById(otherUserId);
                //获取用户头像框地址
                // 1. 查询用户当前使用的头像框ID
                Integer frameId = userMapper.selectCurrentFrameId(user.getId());
                AvatarFrame  frame = frameMapper.selectById(frameId);
                if (user != null) {
                    chat.setRelatedUserId(user.getId());
                    chat.setUsername(user.getUsername());
                    chat.setNickname(user.getNickname());
                    chat.setAvatar(user.getAvatar());
                    chat.setAvatarFrame(frame.getUrl());
                    chat.setIsOnline(user.getIsOnline());
                }

                //检查是否关注
                Follow follow = followMapper.selectByBothIds(userId, otherUserId);
                if (follow != null) {
                    chat.setIsFollowing(true);
                } else {
                    chat.setIsFollowing(false);
                }
                // 检查是否为好友关系
                FriendRelation relation = socialMapper.selectFriendRelation(userId, otherUserId);
                if (relation != null) {
                    chat.setIsFriend(true);
                    chat.setRemark(relation.getRemark());
                    chat.setIsStarred(relation.getIsStarred());
                }

                // 获取最后一条消息内容
                Map<String, Object> lastMessage = socialMapper.selectLastMessageContent(userId, otherUserId);
                if (lastMessage != null) {
                    String content = (String) lastMessage.get("content");
                    String contentType = (String) lastMessage.get("content_type");
                    //获取送达时间
                    chat.setLastTime((LocalDateTime) lastMessage.get("create_time"));

                    // 格式化消息内容
                    chat.setLastMessage(formatLastMessage(content, contentType));
                } else {
                    chat.setLastMessage("暂时还没有消息，开始聊天吧");
                }

                // 获取未读消息数
                Integer unreadCount = cacheService.getCachedUnreadCount(userId, otherUserId);
                if (unreadCount == null) {
                    unreadCount = socialMapper.selectUnreadCountWithUser(userId, otherUserId);
                    cacheService.cacheUnreadCount(userId, otherUserId, unreadCount);
                }
                chat.setUnreadCount(unreadCount);
            }

            // 4. 存入缓存
            cacheService.cacheRecentChats(userId, recentChats);

            return Result.success(recentChats);

        } catch (Exception e) {
            log.error("获取最近聊天列表失败 userId:{}", userId, e);
            return Result.error("获取聊天列表失败");
        }
    }

    /**
     * 格式化最后一条消息内容
     */
    private String formatLastMessage(String content, String contentType) {
        if (content == null && contentType == null) {
            return "暂时还没有消息，开始聊天吧";
        }

        switch (contentType) {
            case "image":
                return "[图片]";
            case "video":
                return "[视频]";
            case "file":
                return "[文件]";
            case "audio":
                return "[语音]";
            case "system":
                return content.length() > 20 ? content.substring(0, 20) + "..." : content;
            default: // text
                // 如果是已撤回的消息
                if (content.contains("【消息已撤回】")) {
                    return content;
                }
                // 普通文本，截取前30个字符
                return content.length() > 30 ? content.substring(0, 30) + "..." : content;
        }
    }

    @Override
    public Result<List<ChatMessageDTO>> getChatHistory(Integer userId, Integer relatedUserId,
                                                       String lastMessageId, Integer pageSize) {
        try {
            // 1. 先尝试从缓存获取
            List<ChatMessageDTO> cachedHistory = cacheService.getCachedChatHistory(userId,relatedUserId);
            if (cachedHistory != null) {
                log.debug("从缓存获取聊天历史 userId:{}, relatedUserId:{}", userId, relatedUserId);
                return Result.success(cachedHistory);
            }

            // 3. 从chat_message表获取聊天历史
            List<Map<String, Object>> rawMessages = socialMapper.selectChatHistoryFromChatMessage(userId, relatedUserId);

            if (rawMessages == null || rawMessages.isEmpty()) {
                cacheService.cacheChatHistory(userId, relatedUserId, new ArrayList<>());
                return Result.success(new ArrayList<>());
            }

            // 4. 转换为ChatMessageDTO列表
            List<ChatMessageDTO> chatHistory = convertToChatMessageDTO(rawMessages, userId);

            // 5. 如果指定了lastMessageId，进行分页过滤
            // 3. 根据lastMessageId进行分页
            if (lastMessageId != null && !lastMessageId.isEmpty()) {
                // 获取比指定消息更早的消息（上一页）
                chatHistory = filterMessagesBefore(chatHistory, lastMessageId, pageSize);
            } else {
                // 获取最新的消息（最后一页）
                chatHistory = getLatestMessages(chatHistory, pageSize);
            }

            // 6. 标记为已读
            markMessagesAsRead(userId, relatedUserId);

            // 7. 存入缓存
            cacheService.cacheChatHistory(userId, relatedUserId, chatHistory);

            return Result.success(chatHistory);

        } catch (Exception e) {
            log.error("获取聊天历史失败 userId:{}, friendId:{}", userId, relatedUserId, e);
            return Result.error("获取聊天历史失败");
        }
    }

    /**
     * 将数据库查询结果转换为ChatMessageDTO
     */
    private List<ChatMessageDTO> convertToChatMessageDTO(List<Map<String, Object>> rawMessages, Integer currentUserId) {
        List<ChatMessageDTO> result = new ArrayList<>();

        for (Map<String, Object> row : rawMessages) {
            ChatMessageDTO dto = new ChatMessageDTO();

            // 基本信息
            dto.setId((Long) row.get("id"));
            dto.setFileId(row.get("file_id") != null ? Long.parseLong(row.get("file_id").toString()) : null);
            dto.setMessageId((String) row.get("message_id"));
            dto.setSenderId((Integer) row.get("sender_id"));
            dto.setReceiverId((Integer) row.get("receiver_id"));
            dto.setChatType((String) row.get("chat_type"));
            dto.setContentType((String) row.get("content_type"));
            dto.setContent((String) row.get("content"));
            dto.setStatus((String) row.get("status"));
            dto.setIsRead("1".equals(row.get("is_read")));
            dto.setCreateTime((LocalDateTime) row.get("create_time"));

            // 判断消息方向
            dto.setIsSent(dto.getSenderId().equals(currentUserId));

            // 文件信息
                ChatFileDTO fileInfo = new ChatFileDTO();
                User user = userMapper.findUserById((Integer) row.get("sender_id"));
                UserSimpleDTO  receiverInfo = new UserSimpleDTO();
                User receiver  = userMapper.findUserById((Integer) row.get("receiver_id"));
                FriendRelation  relation = socialMapper.selectFriendRelation((Integer) row.get("receiver_id"),(Integer) row.get("sender_id") );

                //获取用户头像框地址
               // 1. 查询用户当前使用的头像框ID
                Integer uploaderFrameId = userMapper.selectCurrentFrameId(user.getId());
                Integer receiverFrameId = userMapper.selectCurrentFrameId(receiver.getId());
                AvatarFrame  uploaderFrame = frameMapper.selectById(uploaderFrameId);
                AvatarFrame  receiverFrame = frameMapper.selectById(receiverFrameId);
                //获取接收者的信息
                receiverInfo.setId(receiver.getId());
                receiverInfo.setUsername(receiver.getUsername());
                receiverInfo.setNickname(receiver.getNickname());
                receiverInfo.setAvatar(receiver.getAvatar());
                receiverInfo.setAvatarFrame(receiverFrame.getUrl());
                receiverInfo.setLevel(receiver.getLevel());
                receiverInfo.setVipType(receiver.getVipType());
                receiverInfo.setFollowerCount(receiver.getFollowerCount());
                receiverInfo.setIsFollowed(followMapper.exists((Integer) row.get("sender_id"), (Integer) row.get("receiver_id")) > 0);
                receiverInfo.setIsFriend(socialMapper.checkFriendRelationExists((Integer) row.get("sender_id"), (Integer) row.get("receiver_id")) > 0);

            if (row.get("file_path") != null) {
                String remark = null;
                if (relation != null) {
                    remark = relation.getRemark();
                }
                fileInfo.setId(dto.getFileId());
                fileInfo.setUploaderId(user.getId());
                fileInfo.setUploaderUsername(user.getUsername());
                fileInfo.setUploaderNickname(user.getNickname());
                fileInfo.setUploaderAvatar(user.getAvatar());
                fileInfo.setUploaderAvatarFrame(uploaderFrame.getUrl());
                fileInfo.setUploaderRemark(remark);
                fileInfo.setReceiverId((Integer) row.get("receiver_id"));
                fileInfo.setFileName((String) row.get("file_name"));
                fileInfo.setFileUrl((String) row.get("file_path"));
                fileInfo.setFileType((String) row.get("file_type"));
                fileInfo.setFileSize(formatFileSize((long) row.get("file_size")));
                fileInfo.setThumbnailUrl((String) row.get("thumbnail_path"));
                fileInfo.setMessageId(row.get("message_id").toString());
                fileInfo.setCreateTime((LocalDateTime) row.get("create_time"));
            }else {
                String remark = null;
                if (relation != null) {
                    remark = relation.getRemark();
                }
                fileInfo.setUploaderId(user.getId());
                fileInfo.setUploaderNickname(user.getNickname());
                fileInfo.setUploaderAvatar(user.getAvatar());
                fileInfo.setUploaderAvatarFrame(uploaderFrame.getUrl());
                fileInfo.setReceiverId((Integer) row.get("receiver_id"));
                fileInfo.setUploaderRemark(remark);
            }
            dto.setFileInfo(fileInfo);
            dto.setReceiverInfo(receiverInfo);
            // 解析扩展数据
            String extraData = (String) row.get("extra_data");
            if (extraData != null && !extraData.isEmpty()) {
                try {
                    Map<String, Object> extraDataMap = objectMapper.readValue(extraData, Map.class);
                    dto.setExtraData(extraDataMap);
                } catch (Exception e) {
                    log.warn("解析扩展数据失败: {}", extraData, e);
                }
            }

            result.add(dto);
        }

        // 按时间升序排序（便于前端展示）
        result.sort(Comparator.comparing(ChatMessageDTO::getCreateTime));

        return result;
    }

    /**
     * 获取最新的消息（最后一页）
     */
    private List<ChatMessageDTO> getLatestMessages(List<ChatMessageDTO> allMessages, Integer pageSize) {
        if (allMessages.size() <= pageSize) {
            return allMessages;
        }
        // 返回最后pageSize条
        return allMessages.subList(allMessages.size() - pageSize, allMessages.size());
    }

    /**
     * 获取比指定消息更早的消息（上一页）
     */
    private List<ChatMessageDTO> filterMessagesBefore(List<ChatMessageDTO> allMessages,
                                                      String lastMessageId, Integer pageSize) {
        // 找到lastMessageId的位置
        int targetIndex = -1;
        for (int i = 0; i < allMessages.size(); i++) {
            if (allMessages.get(i).getMessageId().equals(lastMessageId)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            // 没找到，返回最新的pageSize条
            return getLatestMessages(allMessages, pageSize);
        }

        // 获取比targetIndex更早的消息（上一页）
        // 例如：targetIndex=80（第81条消息），我们要获取61-80条
        int fromIndex = Math.max(0, targetIndex - pageSize);
        int toIndex = targetIndex;  // 不包含targetIndex本身

        if (fromIndex >= toIndex) {
            return new ArrayList<>();
        }

        return allMessages.subList(fromIndex, toIndex);
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }


    /**
     * 标记消息为已读
     */
    @Transactional
   void markMessagesAsRead(Integer userId, Integer relatedUserId) {
        try {
            // 更新chat_message表
            socialMapper.markMessagesAsRead(userId, relatedUserId);

            // 清除未读计数缓存
            cacheService.resetUnreadCount(userId, relatedUserId);

            // 清除最近聊天缓存（因为未读数变了）
            cacheService.evictRecentChatsCache(userId);

            // 可选：同时更新notification表的已读状态（保持数据一致）
            socialMapper.markChatNotificationsAsRead(userId, relatedUserId);

        } catch (Exception e) {
            log.warn("标记消息已读失败 userId:{}, relatedUserId:{}", userId, relatedUserId, e);
        }
    }

    // 在sendMessage方法中，发送成功后清除缓存
    private void afterMessageSent(Integer senderId, Integer receiverId) {
        // 清除相关缓存
        cacheService.evictRecentChatsCache(senderId);
        cacheService.evictRecentChatsCache(receiverId);
        cacheService.evictChatHistoryCache(senderId, receiverId);
        cacheService.evictChatHistoryCache(receiverId, senderId);

        // 增加未读计数
        cacheService.incrementUnreadCount(receiverId, senderId);
    }

    @Override
    @Transactional
    public Result clearChatHistory(Integer userId, Integer relatedUserId) {
        try {
            // 1. 逻辑删除聊天记录（chat_message表）
            int messageResult = socialMapper.deleteHistoryFromChat(userId, relatedUserId);

            // 2. 可选：逻辑删除关联的文件记录
            //查看该消息contentType是文本信息text还是其他，如果不是text，则删除相关记录
            List<Map<String, Object>> rawMessages = socialMapper.selectChatHistoryFromChatMessage(userId, relatedUserId);
            for (Map<String, Object> row : rawMessages) {
                if (!"text".equals(row.get("content_type"))) {
                    String messageId = (String) row.get("message_id");
                    chatFileMapper.deleteByMessageId(messageId);
                }
            }

            // 3. 清除相关缓存
            cacheService.evictChatHistoryCache(userId, relatedUserId);
            cacheService.evictRecentChatsCache(userId);
            cacheService.resetUnreadCount(userId, relatedUserId);

            // 4. 可选：删除notification表中的相关记录（根据需求决定）
            // socialMapper.deleteHistoryFromChat(userId, relatedUserId);

            log.info("清空聊天记录成功 userId:{}, relatedUserId:{}, 删除消息数:{}",
                    userId, relatedUserId, messageResult);

            return Result.success("聊天记录已清空");

        } catch (Exception e) {
            log.error("清空聊天记录失败 userId:{}, relatedUserId:{}", userId, relatedUserId, e);
            // 事务会自动回滚
            return Result.error("清空失败");
        }
    }

    @Override
    public Result<PageBean<UserSimpleDTO>> searchUsers(String keyword, Integer pageNum, Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) claims.get("id");

        PageHelper.startPage(pageNum, pageSize);
        List<UserSimpleDTO> users = socialMapper.searchUsers(keyword, currentUserId,
                (pageNum - 1) * pageSize, pageSize);
        long total = socialMapper.countUsers(keyword, currentUserId);

        PageBean<UserSimpleDTO> pageBean = new PageBean<>(total, users);
        pageBean.setPageNum(pageNum);
        pageBean.setPageSize(pageSize);

        return Result.success(pageBean);
    }

    @Override
    public Result<Map<String, Object>> getSocialInfo(Integer userId) {
        try {
            // 1. 先尝试从缓存获取
            Object cachedInfo = cacheService.getCachedUserInfo(userId);
            if (cachedInfo != null) {
                log.debug("从缓存获取用户信息 userId:{}", userId);
                return Result.success((Map<String, Object>) cachedInfo);
            }

            // 2. 缓存不存在则从数据库查询
            Map<String, Object> result = new HashMap<>();

            // 获取各类社交信息
            List<FriendRelationDTO> friends = getFriendList(userId).getData();
             result.put("friends", friends);

            List<FollowRelationDTO> following = getFollowingList(userId).getData();
            result.put("following", following);
            result.put("pendingApplies", socialMapper.selectPendingApplies(userId));
            result.put("unreadCount", socialMapper.selectUnreadCount(userId));
            List<RecentChatDTO> recentChats = socialMapper.selectRecentChatsFromChatMessage(userId, 10);
            //遍历获取用户信息进行赋值
            for (RecentChatDTO chat : recentChats) {
                User user = userMapper.findUserById(chat.getRelatedUserId());
                //获取用户头像框地址
                // 1. 查询用户当前使用的头像框ID
                Integer frameId = userMapper.selectCurrentFrameId(user.getId());
                AvatarFrame  frame = frameMapper.selectById(frameId);
                //获取最后一条消息内容
                Map<String, Object> lastMessage = socialMapper.selectLastMessageContent(userId, chat.getRelatedUserId());
                if (lastMessage != null) {
                    String content = (String) lastMessage.get("content");
                    String contentType = (String) lastMessage.get("content_type");
                    chat.setLastMessage(formatLastMessage(content, contentType));
                } else {
                    chat.setLastMessage("暂时还没有消息，开始聊天吧");
                }
                //获取未读消息数
                Integer unreadCount = cacheService.getCachedUnreadCount(userId, chat.getRelatedUserId());
                if (unreadCount == null) {
                    unreadCount = socialMapper.selectUnreadCountWithUser(userId, chat.getRelatedUserId());
                    cacheService.cacheUnreadCount(userId, chat.getRelatedUserId(), unreadCount);
                }
                chat.setUnreadCount(unreadCount);
                //获取用户信息
                chat.setRelatedUserId(user.getId());
                chat.setUsername(user.getUsername());
                chat.setNickname(user.getNickname());
                chat.setAvatar(user.getAvatar());
                chat.setAvatarFrame(frame.getUrl());
                chat.setIsFriend(socialMapper.checkFriendRelationExists(userId, chat.getRelatedUserId()) > 0);
                chat.setIsOnline(user.getIsOnline());
            }
            result.put("recentChats", socialMapper.selectRecentChatsFromChatMessage(userId, 10));

            // 用户基本信息 - 使用普通HashMap
            User user = userMapper.findUserById(userId);
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("friendCount", friendRelationMapper.countFriendsById(userId));
            userInfo.put("followCount", user.getFollowCount());
            userInfo.put("followerCount", user.getFollowerCount());
            // 3. 存入缓存
            cacheService.cacheUserInfo(userId, result);
            return Result.success(result);

        } catch (Exception e) {
            log.error("获取社交信息失败", e);
            return Result.error("获取社交信息失败");
        }
    }


    // 私有方法：发送通知
    private void sendNotification(Integer userId, Integer relatedUserId, String type,
                                  String title, String content, Long relatedId, String relatedType) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("relatedUserId", relatedUserId);

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setData(objectMapper.writeValueAsString(data));
            notification.setRelatedId(Integer.parseInt(relatedId.toString()));
            notification.setRelatedType(relatedType);
            notification.setRelatedUserId(relatedUserId);
            notification.setIsRead(false);

            socialMapper.insertNotification(notification);

        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }

    // 在SocialServiceImpl中添加

    @Override
    @Transactional
    public Result withdrawMessage(String messageId, Integer userId, String reason) {
        try {
            // 1. 解析消息ID获取发送者和接收者
            String[] parts = messageId.split("_");
            if (parts.length < 3) {
                return Result.error("消息ID格式错误");
            }

            Integer senderId = Integer.parseInt(parts[0]);
            Integer receiverId = Integer.parseInt(parts[1]);

            // 2. 验证操作权限（只能撤回自己发送的消息）
            if (!senderId.equals(userId)) {
                return Result.error("只能撤回自己发送的消息");
            }

            // 3. 检查消息发送时间（例如：只能撤回2分钟内的消息）
            long messageTime = Long.parseLong(parts[2]);
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - messageTime;

            if (timeDiff > 2 * 60 * 1000) { // 2分钟
                return Result.error("消息发送时间过长，无法撤回");
            }

            // 4. 获取消息通知记录（需要扩展mapper支持根据消息ID查询）
            // 这里假设有一个新的mapper方法
            ChatMessage chatMessage = socialMapper.selectChatMessageByMessageId(messageId);
            if (chatMessage == null) {
                return Result.error("消息不存在或已被删除");
            }

            // 5. 更新消息状态为"已撤回"
            cacheService.cacheMessageStatus(messageId, SocialCacheService.MessageStatus.WITHDRAWN);

            // 7. 更新原消息内容（可选：替换为"消息已撤回"）
            socialMapper.withdrawMessage(messageId, senderId, reason);

            // 8. 插入消息状态历史记录
            ChatMessageStatusHistory statusHistory = new ChatMessageStatusHistory();
            statusHistory.setMessageId(messageId);
            statusHistory.setStatus("withdrawn");
            statusHistory.setReason(reason);
            statusHistory.setOperatorId(senderId);
            socialMapper.insertMessageStatusHistory(statusHistory);
            // 8. 清除相关缓存
            cacheService.evictRecentChatsCache(senderId);
            cacheService.evictRecentChatsCache(receiverId);

            return Result.success("消息撤回成功");

        } catch (Exception e) {
            log.error("撤回消息失败 messageId:{}", messageId, e);
            return Result.error("消息撤回失败");
        }
    }

    /**
     * 保存聊天文件记录
     */
    @Transactional
    public ChatFile saveChatFile(Integer uploaderId, Integer receiverId,
                                 String fileName, String filePath, String fileType,
                                 Long fileSize, String thumbnailPath, String messageId) {
        ChatFile chatFile = new ChatFile();
        chatFile.setUploaderId(uploaderId);
        chatFile.setReceiverId(receiverId);
        chatFile.setFileName(fileName);
        chatFile.setFilePath(filePath);
        chatFile.setFileType(fileType);
        chatFile.setFileSize(fileSize);
        chatFile.setThumbnailPath(thumbnailPath);
        chatFile.setMessageId(messageId);

        chatFileMapper.insert(chatFile);
        return chatFile;
    }

    /**
     * 根据消息ID获取文件信息
     */
    public ChatFile getChatFileByMessageId(String messageId) {
        return chatFileMapper.selectByMessageId(messageId);
    }

    private boolean isFileMessage(String contentType) {
        return Arrays.asList("image", "video", "file", "audio",  "system").contains(contentType);
    }

    private ChatFile saveChatFileRecord(Integer senderId, Integer receiverId,
                                        Map<String, Object> extraData, String messageId) {
        try {
            if (extraData == null) {
                return null;
            }

            String fileName = (String) extraData.get("fileName");
            String filePath = (String) extraData.get("filePath");
            String fileType = (String) extraData.get("fileType");
            Long fileSize = extraData.get("fileSize") != null ?
                    Long.parseLong(extraData.get("fileSize").toString().split("\\.")[0]) : 0L;
            String thumbnailPath = (String) extraData.get("thumbnailPath");

            if (fileName == null || filePath == null || fileType == null) {
                return null;
            }

            return saveChatFile(
                    senderId, receiverId, fileName, filePath, fileType,
                    fileSize, thumbnailPath, messageId
            );

        } catch (Exception e) {
            log.error("保存聊天文件记录失败", e);
            return null;
        }
    }

    private Map<String, Object> convertToFileInfo(ChatFile chatFile) {
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("id", chatFile.getId());
        fileInfo.put("fileName", chatFile.getFileName());
        fileInfo.put("fileUrl", chatFile.getFilePath()); // 假设 filePath 就是 URL
        fileInfo.put("fileType", chatFile.getFileType());
        fileInfo.put("fileSize", chatFile.getFileSize());
        fileInfo.put("thumbnailUrl", chatFile.getThumbnailPath());
        return fileInfo;
    }
}