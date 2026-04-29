package com.itheima.mapper;

import com.itheima.pojo.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;


@Mapper
public interface UserLevelMapper {

    /**
     * 更新用户经验和等级
     */
    @Update("UPDATE user SET exp = #{exp}, level = #{level}, update_time = now() WHERE id = #{userId}")
    int updateExpAndLevel(@Param("userId") Integer userId,
                          @Param("exp") Long exp,
                          @Param("level") Integer level);

    /**
     * 查询等级统计
     */
    @Select("SELECT level, COUNT(*) as count FROM user GROUP BY level ORDER BY level")
    @Results({
            @Result(property = "level", column = "level"),
            @Result(property = "count", column = "count")
    })
    List<Map<String, Object>> getLevelStatistics();

    /**
     * 查询用户排行（按经验和等级）
     */
    @Select("SELECT id, username, level, exp FROM user ORDER BY level DESC, exp DESC LIMIT #{limit}")
    List<User> getTopUsers(@Param("limit") Integer limit);

}
