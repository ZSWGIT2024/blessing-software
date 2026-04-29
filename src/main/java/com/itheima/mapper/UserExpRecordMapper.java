package com.itheima.mapper;

import com.itheima.pojo.UserExpRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserExpRecordMapper {

    /**
     * 添加经验记录
     */
    @Insert("INSERT INTO user_exp_record(user_id, action_type, exp_value, description, create_time) " +
            "VALUES(#{userId}, #{actionType}, #{expValue}, #{description}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserExpRecord record);

    /**
     * 根据用户ID查询经验记录
     */
    @Select("SELECT * FROM user_exp_record WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<UserExpRecord> findByUserId(@Param("userId") Integer userId);

    /**
     * 统计今日某动作执行次数
     */
    @Select("SELECT COUNT(*) FROM user_exp_record " +
            "WHERE user_id = #{userId} AND action_type = #{actionType} " +
            "AND create_time BETWEEN #{startTime} AND #{endTime}")
    Integer countByActionToday(@Param("userId") Integer userId,
                               @Param("actionType") String actionType,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 统计今日总经验
     */
    @Select("SELECT COALESCE(SUM(exp_value), 0) FROM user_exp_record " +
            "WHERE user_id = #{userId} AND create_time BETWEEN #{startTime} AND #{endTime}")
    Integer sumExpToday(@Param("userId") Integer userId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

    /**
     * 按动作分组统计今日经验
     * 返回List<Object[]>，每个Object[]包含两个元素：[actionType, totalExp]
     */
    @Select("SELECT action_type, SUM(exp_value) FROM user_exp_record " +
            "WHERE user_id = #{userId} AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY action_type")
    List<Object[]> groupByActionToday(@Param("userId") Integer userId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 分页查询经验记录
     */
    @Select("SELECT * FROM user_exp_record WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<UserExpRecord> findPage(@Param("userId") Integer userId,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);

    /**
     * 获取最近的经验记录
     */
    @Select("SELECT * FROM user_exp_record WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT 10")
    List<UserExpRecord> findRecent(@Param("userId") Integer userId);
}
