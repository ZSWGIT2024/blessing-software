package com.itheima.mapper;

import com.itheima.dto.FriendRelationDTO;
import com.itheima.pojo.FriendRelation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendRelationMapper {

    int insert(FriendRelation relation);

    int delete(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    int update(FriendRelation relation);

    int increaseInteraction(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    int isFriend(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    FriendRelation selectFriendDetail(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    List<FriendRelation> selectFriendList(@Param("userId") Integer userId,
                                          @Param("groupName") String groupName,
                                          @Param("isStarred") Boolean isStarred,
                                          @Param("keyword") String keyword,
                                          @Param("sortType") String sortType,
                                          @Param("offset") Integer offset,
                                          @Param("pageSize") Integer pageSize);

    int countFriends(@Param("userId") Integer userId,
                     @Param("groupName") String groupName,
                     @Param("isStarred") Boolean isStarred);

    @Select("SELECT COUNT(*) FROM friend_relation WHERE user_id = #{userId}")
    int countFriendsById(@Param("userId") Integer userId);

    int batchMoveToGroup(@Param("userId") Integer userId,
                         @Param("friendIds") List<Integer> friendIds,
                         @Param("newGroupName") String newGroupName);

    List<FriendRelation> selectCommonFriends(@Param("userId1") Integer userId1,
                                             @Param("userId2") Integer userId2,
                                             @Param("limit") Integer limit);

    /**
     * 获取指定分组的好友
     */
    @Select("SELECT fr.*, u.username as friend_username, u.nickname as friend_nickname, " +
            "u.avatar as friend_avatar, u.is_online " +
            "FROM friend_relation fr " +
            "LEFT JOIN user u ON fr.friend_id = u.id " +
            "WHERE fr.user_id = #{userId} AND fr.group_name = #{groupName}")
    List<FriendRelationDTO> findFriendsByGroup(@Param("userId") Integer userId, @Param("groupName") String groupName);

    /**
     * 更新好友分组
     */
    @Update("UPDATE friend_relation SET group_name = #{groupName} " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int updateFriendGroup(@Param("userId") Integer userId, @Param("friendId") Integer friendId,
                          @Param("groupName") String groupName);

    /**
     * 清空好友分组（从分组移除）
     */
    @Update("UPDATE friend_relation SET group_name = NULL " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int clearFriendGroup(@Param("userId") Integer userId, @Param("friendId") Integer friendId);

    /**
     * 获取指定分组的好友数量
     */
    @Select("SELECT COUNT(*) FROM friend_relation WHERE user_id = #{userId} AND group_name = #{groupName}")
    int countFriendsInGroup(@Param("userId") Integer userId, @Param("groupName") String groupName);

    //根据分组名称删除分组
    @Delete("DELETE FROM friend_relation WHERE user_id = #{userId} AND group_name = #{groupName}")
    void clearGroupByGroupName(@Param("userId") Integer userId,@Param("groupName") String groupName);
}