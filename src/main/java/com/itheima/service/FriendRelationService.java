package com.itheima.service;

import com.itheima.pojo.FriendRelation;
import com.itheima.service.impl.FriendRelationServiceImpl;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface FriendRelationService {

    boolean deleteFriend(Integer userId, Integer friendId);

    boolean updateFriendInfo(FriendRelation relation);

    boolean toggleStarFriend(Integer userId, Integer friendId, Boolean starred);

    boolean toggleBlockFriend(Integer userId, Integer friendId, Boolean blocked);

    FriendRelation getFriendDetail(Integer userId, Integer friendId);

    FriendRelationServiceImpl.FriendListVO getFriendList(Integer userId, String groupName, Boolean starred,
                                                         String keyword, String sortType,
                                                         Integer page, Integer pageSize);

    boolean batchMoveFriendsToGroup(Integer userId, List<Integer> friendIds, String newGroupName);

    List<FriendRelation> searchFriends(Integer userId, String keyword);

    List<FriendRelation> getCommonFriends(Integer userId, Integer otherUserId);

    void recordInteraction(Integer userId, Integer friendId);

    @Cacheable(value = "friend", key = "#userId + ':' + #friendId")
    boolean isFriend(Integer userId, Integer friendId);

    @Cacheable(value = "friend_count", key = "#userId")
    int getFriendCount(Integer userId);

    List<FriendRelationServiceImpl.GroupStatDTO> getGroupStats(Integer userId);
}