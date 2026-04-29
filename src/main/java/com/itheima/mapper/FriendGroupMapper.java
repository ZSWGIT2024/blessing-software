package com.itheima.mapper;

import com.itheima.dto.FriendGroupDTO;
import com.itheima.pojo.FriendGroup;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendGroupMapper {

    /**
     * 获取用户的所有分组
     */
    @Select("SELECT * FROM friend_group WHERE user_id = #{userId} ORDER BY sort_order")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "groupName", column = "group_name"),
            @Result(property = "sortOrder", column = "sort_order"),
            @Result(property = "friendCount", column = "friend_count"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "updateTime", column = "update_time")
    })
    List<FriendGroup> findByUserId(Integer userId);

    /**
     * 根据ID查询分组
     */
    @Select("SELECT * FROM friend_group WHERE id = #{id} AND user_id = #{userId}")
    FriendGroup findByIdAndUserId(@Param("id") Long id, @Param("userId") Integer userId);

    /**
     * 根据名称查询分组
     */
    @Select("SELECT * FROM friend_group WHERE user_id = #{userId} AND group_name = #{groupName}")
    FriendGroup findByUserIdAndGroupName(@Param("userId") Integer userId, @Param("groupName") String groupName);

    /**
     * 插入分组
     */
    @Insert("INSERT INTO friend_group(user_id, group_name, sort_order, color, description, friend_count) " +
            "VALUES(#{userId}, #{groupName}, #{sortOrder}, #{color}, #{description}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FriendGroup group);

    /**
     * 更新分组
     */
    @Update("UPDATE friend_group SET group_name = #{groupName}, sort_order = #{sortOrder}, " +
            "color = #{color}, description = #{description} WHERE id = #{id} AND user_id = #{userId}")
    int update(FriendGroup group);

    /**
     * 删除分组
     */
    @Delete("DELETE FROM friend_group WHERE id = #{id}")
    int delete(@Param("id") Long id);

    /**
     * 更新分组好友数量
     */
    @Update("UPDATE friend_group SET friend_count = " +
            "(SELECT COUNT(*) FROM friend_relation WHERE user_id = #{userId} AND group_name = #{groupName}) " +
            "WHERE user_id = #{userId} AND group_name = #{groupName}")
    int updateFriendCount(@Param("userId") Integer userId, @Param("groupName") String groupName);
}