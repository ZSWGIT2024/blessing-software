package com.itheima.service.impl;

import com.itheima.common.CacheConstant;
import com.itheima.mapper.FriendRelationMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.FriendRelation;
import com.itheima.pojo.User;
import com.itheima.service.FriendRelationService;
import com.itheima.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRelationServiceImpl implements FriendRelationService {

    private final FriendRelationMapper friendRelationMapper;

    private final UserMapper userMapper;

    private final UserService userService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheConstant.CACHE_FRIEND,
            CacheConstant.CACHE_FRIEND_COUNT},
            allEntries = true)
    public boolean deleteFriend(Integer userId, Integer friendId) {
        // 删除单向关系
        int result1 = friendRelationMapper.delete(userId, friendId);
        int result2 = friendRelationMapper.delete(friendId, userId);

        if (result1 > 0 || result2 > 0) {
            // 更新用户统计
            updateUserFriendStats(userId);
            updateUserFriendStats(friendId);

            log.info("用户 {} 删除了好友 {}", userId, friendId);
            return true;
        }

        return false;
    }

    @Override
    @CacheEvict(cacheNames = CacheConstant.CACHE_FRIEND,
            key = "T(com.itheima.common.CacheConstant).getFriendKey(#relation.userId, #relation.friendId)")
    public boolean updateFriendInfo(FriendRelation relation) {
        return friendRelationMapper.update(relation) > 0;
    }

    @Override
    @CacheEvict(cacheNames = CacheConstant.CACHE_FRIEND,
            key = "T(com.itheima.common.CacheConstant).getFriendKey(#userId, #friendId)")
    public boolean toggleStarFriend(Integer userId, Integer friendId, Boolean starred) {
        FriendRelation relation = friendRelationMapper.selectFriendDetail(userId, friendId);
        if (relation == null) {
            throw new RuntimeException("好友不存在");
        }

        relation.setIsStarred(starred);
        return friendRelationMapper.update(relation) > 0;
    }

    @Override
    @CacheEvict(cacheNames = CacheConstant.CACHE_FRIEND,
            key = "T(com.itheima.common.CacheConstant).getFriendKey(#userId, #friendId)")
    public boolean toggleBlockFriend(Integer userId, Integer friendId, Boolean blocked) {
        FriendRelation relation = friendRelationMapper.selectFriendDetail(userId, friendId);
        if (relation == null) {
            throw new RuntimeException("好友不存在");
        }

        relation.setIsBlocked(blocked);
        return friendRelationMapper.update(relation) > 0;
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.CACHE_FRIEND,
            key = "T(com.itheima.common.CacheConstant).getFriendKey(#userId, #friendId)")
    public FriendRelation getFriendDetail(Integer userId, Integer friendId) {
        return friendRelationMapper.selectFriendDetail(userId, friendId);
    }

    @Override
    public FriendListVO getFriendList(Integer userId, String groupName, Boolean starred,
                                      String keyword, String sortType, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;

        List<FriendRelation> friends = friendRelationMapper.selectFriendList(
                userId, groupName, starred, keyword, sortType, offset, pageSize);

        int total = friendRelationMapper.countFriends(userId, groupName, starred);

        FriendListVO vo = new FriendListVO();
        vo.setFriends(friends);
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setTotalPages((int) Math.ceil((double) total / pageSize));

        return vo;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConstant.CACHE_FRIEND, allEntries = true)
    public boolean batchMoveFriendsToGroup(Integer userId, List<Integer> friendIds, String newGroupName) {
        if (friendIds == null || friendIds.isEmpty()) {
            return true;
        }

        int result = friendRelationMapper.batchMoveToGroup(userId, friendIds, newGroupName);
        return result > 0;
    }

    @Override
    public List<FriendRelation> searchFriends(Integer userId, String keyword) {
        return friendRelationMapper.selectFriendList(userId, null, null, keyword, null, 0, 20);
    }

    @Override
    public List<FriendRelation> getCommonFriends(Integer userId, Integer otherUserId) {
        return friendRelationMapper.selectCommonFriends(userId, otherUserId, 10);
    }

    @Override
    public void recordInteraction(Integer userId, Integer friendId) {
        friendRelationMapper.increaseInteraction(userId, friendId);
        userService.updateLastActiveTime(userId);
        userService.updateLastActiveTime(friendId);
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.CACHE_FRIEND,
            key = "T(com.itheima.common.CacheConstant).getFriendKey(#userId, #friendId)")
    public boolean isFriend(Integer userId, Integer friendId) {
        return friendRelationMapper.isFriend(userId, friendId) > 0;
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.CACHE_FRIEND_COUNT,
            key = "T(com.itheima.common.CacheConstant).getFriendCountKey(#userId)")
    public int getFriendCount(Integer userId) {
        return friendRelationMapper.countFriends(userId, null, null);
    }

    @Override
    public List<GroupStatDTO> getGroupStats(Integer userId) {
        // 这里简化处理，实际应该查询数据库按group_name分组统计
        return List.of(
                new GroupStatDTO("全部好友", getFriendCount(userId)),
                new GroupStatDTO("星标好友", friendRelationMapper.countFriends(userId, null, true)),
                new GroupStatDTO("最近互动", 0) // 需要额外查询
        );
    }

    /**
     * 更新用户好友统计
     */
    private void updateUserFriendStats(Integer userId) {
        int friendCount = getFriendCount(userId);
        userMapper.updateFollowCount(userId, friendCount); // 复用字段
    }

    @Data
    public static class FriendListVO {
        private List<FriendRelation> friends;
        private Integer total;
        private Integer page;
        private Integer pageSize;
        private Integer totalPages;
    }

    @Data
    public static class GroupStatDTO {
        private String groupName;
        private Integer friendCount;

        public GroupStatDTO(String groupName, Integer friendCount) {
            this.groupName = groupName;
            this.friendCount = friendCount;
        }
    }
}