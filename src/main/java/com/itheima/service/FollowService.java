package com.itheima.service;

import com.itheima.dto.FollowStatusDTO;
import com.itheima.vo.FollowListVO;
import com.itheima.vo.FollowRelationVO;

import java.util.List;

public interface FollowService {

    /**
     * 关注用户
     */
    boolean followUser(Integer followerId, Integer followingId, String remark);

    /**
     * 取消关注
     */
    boolean unfollowUser(Integer followerId, Integer followingId);

    /**
     * 设置为特别关注
     */
    boolean setSpecialFollow(Integer followerId, Integer followingId);

    /**
     * 获取关注列表
     */
    FollowListVO getFollowingList(Integer userId, String relationType, Integer page, Integer pageSize);

    /**
     * 获取粉丝列表
     */
    FollowListVO getFollowerList(Integer userId, String relationType, Integer page, Integer pageSize);

    /**
     * 获取互关列表
     */
    FollowListVO getMutualFollowList(Integer userId, Integer page, Integer pageSize);

    /**
     * 检查关注状态
     */
    FollowRelationVO checkFollowStatus(Integer userId, Integer targetUserId);

    /**
     * 批量检查关注状态
     */
    List<FollowStatusDTO> batchCheckFollowStatus(Integer userId, List<Integer> targetUserIds);

    /**
     * 检查是否关注
     */
    boolean isFollowing(Integer userId, Integer targetUserId);

    /**
     * 获取关注数量
     */
    int getFollowingCount(Integer userId);

    /**
     * 获取粉丝数量
     */
    int getFollowerCount(Integer userId);
}