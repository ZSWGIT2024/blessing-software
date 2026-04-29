package com.itheima.mapper;

import com.itheima.pojo.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FollowMapper {

    // 添加关注
    int insert(Follow follow);

    // 取消关注
    int delete(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    // 更新关注信息
    int update(Follow follow);

    // 查询是否关注
    int exists(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    // 获取关注关系详情
    Follow selectByBothIds(@Param("followerId") Integer followerId, @Param("followingId") Integer followingId);

    // 查询用户的关注列表（分页）
    List<Follow> selectFollowingList(@Param("userId") Integer userId,
                                     @Param("relationType") String relationType,
                                     @Param("offset") Integer offset,
                                     @Param("pageSize") Integer pageSize);

    // 查询用户的粉丝列表（分页）
    List<Follow> selectFollowerList(@Param("userId") Integer userId,
                                    @Param("relationType") String relationType,
                                    @Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize);

    // 获取关注数量
    int countFollowing(@Param("userId") Integer userId);

    // 获取粉丝数量
    int countFollowers(@Param("userId") Integer userId);

    // 获取互关列表（双方都关注了对方）
    List<Follow> selectMutualFollowList(@Param("userId") Integer userId,
                                        @Param("offset") Integer offset,
                                        @Param("pageSize") Integer pageSize);

    // 批量查询关注状态
    List<Follow> batchSelectFollowStatus(@Param("userId") Integer userId,
                                         @Param("targetUserIds") List<Integer> targetUserIds);
}