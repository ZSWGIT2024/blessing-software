package com.itheima.mapper;

import com.itheima.pojo.OperationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 操作日志Mapper
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 插入操作日志
     */
    @Insert("INSERT INTO operation_log (module, operation, type, method, request_url, request_ip, " +
            "request_params, response_result, user_id, username, duration, status, error_msg, create_time) " +
            "VALUES (#{module}, #{operation}, #{type}, #{method}, #{requestUrl}, #{requestIp}, " +
            "#{requestParams}, #{responseResult}, #{userId}, #{username}, #{duration}, #{status}, #{errorMsg}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM operation_log WHERE id = #{id}")
    OperationLog selectById(Long id);

    /**
     * 根据用户ID查询
     */
    @Select("SELECT * FROM operation_log WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<OperationLog> selectByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * 分页查询
     */
    List<OperationLog> selectPage(@Param("offset") Integer offset,
                                   @Param("size") Integer size,
                                   @Param("module") String module,
                                   @Param("type") String type,
                                   @Param("userId") Integer userId,
                                   @Param("status") String status,
                                   @Param("startTime") String startTime,
                                   @Param("endTime") String endTime);

    /**
     * 统计总数
     */
    Long countTotal(@Param("module") String module,
                    @Param("type") String type,
                    @Param("userId") Integer userId,
                    @Param("status") String status,
                    @Param("startTime") String startTime,
                    @Param("endTime") String endTime);

    /**
     * 删除指定日期之前的日志
     */
    @Delete("DELETE FROM operation_log WHERE create_time < #{date}")
    int deleteBeforeDate(@Param("date") String date);

    /**
     * 获取模块统计
     */
    @Select("SELECT module, COUNT(*) as count FROM operation_log " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY module ORDER BY count DESC")
    List<Map<String, Object>> selectModuleStats();

    /**
     * 获取类型统计
     */
    @Select("SELECT type, COUNT(*) as count FROM operation_log " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY type ORDER BY count DESC")
    List<Map<String, Object>> selectTypeStats();

    /**
     * 获取每日操作统计
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM operation_log " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> selectDailyStats();

    /**
     * 获取失败操作统计
     */
    @Select("SELECT operation, COUNT(*) as count FROM operation_log " +
            "WHERE status = 'fail' AND create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY operation ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> selectFailStats();
}
