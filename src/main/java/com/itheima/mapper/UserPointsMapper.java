package com.itheima.mapper;

import com.itheima.pojo.UserPoints;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserPointsMapper {


    // 根据用户ID查询积分流水
    @Select("SELECT * FROM coin_transaction WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<UserPoints> findByUserId(@Param("userId") Integer userId);

    // 根据类型查询
    @Select("SELECT * FROM coin_transaction WHERE user_id = #{userId} AND transaction_type = #{transactionType} ORDER BY create_time DESC")
    List<UserPoints> findByTransactionType(@Param("userId") Integer userId,
                                           @Param("transactionType") String transactionType);

    // 根据业务类型查询
    @Select("SELECT * FROM coin_transaction WHERE user_id = #{userId} AND business_type = #{businessType} ORDER BY create_time DESC")
    List<UserPoints> findByBusinessType(@Param("userId") Integer userId,
                                        @Param("businessType") String businessType);

    // 添加积分流水记录
    @Insert("INSERT INTO coin_transaction(user_id, transaction_type, amount, balance_after, " +
            "business_type, business_id, description, extra_data, create_time) " +
            "VALUES(#{userId}, #{transactionType}, #{amount}, #{balanceAfter}, " +
            "#{businessType}, #{businessId}, #{description}, #{extraData, typeHandler=com.itheima.handler.JsonTypeHandler}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserPoints userPoints);

    // 统计用户今日获取积分
    @Select("SELECT COALESCE(SUM(amount), 0) FROM coin_transaction " +
            "WHERE user_id = #{userId} AND amount > 0 " +
            "AND DATE(create_time) = CURDATE()")
    Integer sumTodayEarned(@Param("userId") Integer userId);

    // 统计用户总积分变动
    @Select("SELECT " +
            "COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) as totalEarned, " +
            "COALESCE(SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END), 0) as totalSpent " +
            "FROM coin_transaction WHERE user_id = #{userId}")
    Map<String, Long> getStatistics(@Param("userId") Integer userId);

    // 分页查询积分流水
    @Select("SELECT * FROM coin_transaction WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<UserPoints> findPage(@Param("userId") Integer userId,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);

    // 查询时间段内的积分流水
    @Select("SELECT * FROM coin_transaction " +
            "WHERE user_id = #{userId} AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "ORDER BY create_time DESC")
    List<UserPoints> findByTimeRange(@Param("userId") Integer userId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    // 获取最近一条记录
    @Select("SELECT * FROM coin_transaction WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT 1")
    UserPoints findLatest(@Param("userId") Integer userId);

}