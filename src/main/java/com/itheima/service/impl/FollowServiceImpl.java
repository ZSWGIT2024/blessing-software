package com.itheima.service.impl;

import com.itheima.common.CacheConstant;
import com.itheima.common.RelationConstant;
import com.itheima.dto.FollowStatusDTO;
import com.itheima.mapper.FollowMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.Follow;
import com.itheima.pojo.User;
import com.itheima.service.FollowService;
import com.itheima.service.FriendRelationService;
import com.itheima.service.UserService;
import com.itheima.vo.FollowListVO;
import com.itheima.vo.FollowRelationVO;
import com.itheima.vo.FollowUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final FriendRelationService friendRelationService;
    private final UserService userService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheConstant.CACHE_FOLLOW, CacheConstant.CACHE_FOLLOWING_COUNT, CacheConstant.CACHE_FOLLOWER_COUNT},
            key = "T(com.itheima.common.CacheConstant).getFollowKey(#followerId, #followingId)")
    public boolean followUser(Integer followerId, Integer followingId, String remark) {
        // 检查用户是否存在
        User follower = userService.findUserById(followerId);
        User following = userService.findUserById(followingId);

        if (follower == null || following == null) {
            throw new RuntimeException("用户不存在");
        }

        // 不能关注自己
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }

        // 检查是否已关注
        if (isFollowing(followerId, followingId)) {
            throw new RuntimeException("已关注该用户");
        }

        // 检查关注数量限制
        int followingCount = getFollowingCount(followerId);
        int maxFollowing = follower.isVip() ?
                RelationConstant.MAX_FOLLOWING_VIP : RelationConstant.MAX_FOLLOWING_NORMAL;

        if (followingCount >= maxFollowing) {
            throw new RuntimeException("关注数量已达上限");
        }

        // 创建关注记录
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setRemark(remark);
        follow.setRelationType(RelationConstant.FOLLOW_TYPE_NORMAL);
        follow.setMuteNotify(false);
        follow.setIsHidden(false);

        int result = followMapper.insert(follow);

        if (result > 0) {
            // 更新用户统计信息
            updateUserFollowStats(followerId, followingId);
            log.info("用户 {} 关注了用户 {}", followerId, followingId);
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheConstant.CACHE_FOLLOW, CacheConstant.CACHE_FOLLOWING_COUNT, CacheConstant.CACHE_FOLLOWER_COUNT},
            key = "T(com.itheima.common.CacheConstant).getFollowKey(#followerId, #followingId)")
    public boolean unfollowUser(Integer followerId, Integer followingId) {
        int result = followMapper.delete(followerId, followingId);

        if (result > 0) {
            // 更新用户统计信息
            updateUserFollowStats(followerId, followingId);
            log.info("用户 {} 取消关注用户 {}", followerId, followingId);
            return true;
        }

        return false;
    }

    @Override
    @CacheEvict(cacheNames = CacheConstant.CACHE_FOLLOW,
            key = "T(com.itheima.common.CacheConstant).getFollowKey(#followerId, #followingId)")
    public boolean setSpecialFollow(Integer followerId, Integer followingId) {
        Follow follow = followMapper.selectByBothIds(followerId, followingId);
        if (follow == null) {
            throw new RuntimeException("未关注该用户");
        }

        follow.setRelationType(RelationConstant.FOLLOW_TYPE_SPECIAL);
        return followMapper.update(follow) > 0;
    }

    @Override
    public FollowListVO getFollowingList(Integer userId, String relationType, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;

        // 查询关注列表
        List<Follow> followList = followMapper.selectFollowingList(userId, relationType, offset, pageSize);

        // 查询总数
        int total = followMapper.countFollowing(userId);

        // 转换VO
        List<FollowUserVO> userVOList = convertFollowListToVO(followList, true);

        return buildFollowListVO(userVOList, total, page, pageSize);
    }

    @Override
    public FollowListVO getFollowerList(Integer userId, String relationType, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;

        // 查询粉丝列表
        List<Follow> followList = followMapper.selectFollowerList(userId, relationType, offset, pageSize);

        // 查询总数
        int total = followMapper.countFollowers(userId);

        // 转换VO
        List<FollowUserVO> userVOList = convertFollowListToVO(followList, false);

        return buildFollowListVO(userVOList, total, page, pageSize);
    }

    @Override
    public FollowListVO getMutualFollowList(Integer userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;

        // 查询互关列表
        List<Follow> followList = followMapper.selectMutualFollowList(userId, offset, pageSize);

        // 查询总数（需要额外查询）
        int total = getMutualFollowCount(userId);

        // 转换VO
        List<FollowUserVO> userVOList = convertFollowListToVO(followList, true);

        return buildFollowListVO(userVOList, total, page, pageSize);
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.CACHE_FOLLOW,
            key = "T(com.itheima.common.CacheConstant).getFollowKey(#userId, #targetUserId)")
    public boolean isFollowing(Integer userId, Integer targetUserId) {
        return followMapper.exists(userId, targetUserId) > 0;
    }

    @Override
    public FollowRelationVO checkFollowStatus(Integer userId, Integer targetUserId) {
        FollowRelationVO vo = new FollowRelationVO();
        vo.setUserId(userId);
        vo.setTargetUserId(targetUserId);
        vo.setIsFollowing(isFollowing(userId, targetUserId));
        vo.setIsFollowed(isFollowing(targetUserId, userId));
        vo.setIsMutual(vo.getIsFollowing() && vo.getIsFollowed());

        // 查询关注关系详情
        if (vo.getIsFollowing()) {
            Follow follow = followMapper.selectByBothIds(userId, targetUserId);
            vo.setRelationType(follow.getRelationType());
            vo.setRemark(follow.getRemark());
        }

        // 检查是否是好友
        vo.setIsFriend(friendRelationService.isFriend(userId, targetUserId));

        return vo;
    }

    @Override
    public List<FollowStatusDTO> batchCheckFollowStatus(Integer userId, List<Integer> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询关注状态
        List<Follow> followList = followMapper.batchSelectFollowStatus(userId, targetUserIds);

        // 转换为Map，方便查询
        Map<Integer, Boolean> followStatusMap = new HashMap<>();
        for (Follow follow : followList) {
            followStatusMap.put(follow.getFollowingId(), true);
        }

        // 构造返回结果
        List<FollowStatusDTO> result = new ArrayList<>();
        for (Integer targetUserId : targetUserIds) {
            FollowStatusDTO dto = new FollowStatusDTO();
            dto.setTargetUserId(targetUserId);
            dto.setIsFollowing(followStatusMap.getOrDefault(targetUserId, false));
            result.add(dto);
        }

        return result;
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.CACHE_FOLLOWING_COUNT,
            key = "T(com.itheima.common.CacheConstant).getFollowingCountKey(#userId)")
    public int getFollowingCount(Integer userId) {
        return followMapper.countFollowing(userId);
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.CACHE_FOLLOWER_COUNT,
            key = "T(com.itheima.common.CacheConstant).getFollowerCountKey(#userId)")
    public int getFollowerCount(Integer userId) {
        return followMapper.countFollowers(userId);
    }

    // ============== 私有方法 ==============

    /**
     * 转换关注列表为VO
     */
    private List<FollowUserVO> convertFollowListToVO(List<Follow> followList, boolean isFollowingList) {
        if (followList == null || followList.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取用户ID
        List<Integer> userIds = followList.stream()
                .map(follow -> isFollowingList ? follow.getFollowingId() : follow.getFollowerId())
                .collect(Collectors.toList());

        // 批量查询用户信息
        List<User> users = userMapper.selectByIds(userIds);
        Map<Integer, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 构造Map便于查询备注
        Map<Integer, String> remarkMap = followList.stream()
                .collect(Collectors.toMap(
                        follow -> isFollowingList ? follow.getFollowingId() : follow.getFollowerId(),
                        Follow::getRemark
                ));

        // 转换VO
        return userIds.stream()
                .map(userId -> {
                    User user = userMap.get(userId);
                    if (user == null) return null;

                    FollowUserVO vo = new FollowUserVO();
                    vo.setId(user.getId());
                    vo.setUsername(user.getUsername());
                    vo.setNickname(user.getNickname());
                    vo.setAvatar(user.getAvatar());
                    vo.setGender(user.getGender());
                    vo.setBio(user.getBio());
                    vo.setLevel(user.getLevel());
                    vo.setIsVip(user.isVip());
                    vo.setRemark(remarkMap.get(userId));

                    return vo;
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());
    }

    /**
     * 构建关注列表VO
     */
    private FollowListVO buildFollowListVO(List<FollowUserVO> list, int total, int page, int pageSize) {
        FollowListVO vo = new FollowListVO();
        vo.setList(list);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setTotalPages((int) Math.ceil((double) total / pageSize));
        return vo;
    }

    /**
     * 获取互关数量
     */
    private int getMutualFollowCount(Integer userId) {
        // 简化处理，实际应该查询数据库
        List<Follow> mutualFollows = followMapper.selectMutualFollowList(userId, 0, Integer.MAX_VALUE);
        return mutualFollows.size();
    }

    /**
     * 更新用户关注统计
     */
    private void updateUserFollowStats(Integer followerId, Integer followingId) {
        // 清除缓存
        // 缓存会在下次查询时自动更新

        // 更新关注者的关注数
        int followingCount = followMapper.countFollowing(followerId);
        userMapper.updateFollowCount(followerId, followingCount);

        // 更新被关注者的粉丝数
        int followerCount = followMapper.countFollowers(followingId);
        userMapper.updateFollowerCount(followingId, followerCount);

        // 更新活跃时间
        userService.updateLastActiveTime(followerId);
    }
}