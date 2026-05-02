package com.itheima.mapper;

import com.itheima.pojo.LoginRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 登录记录Mapper
 */
@Mapper
public interface LoginRecordMapper {

    /**
     * 插入登录记录
     */
    @Insert("INSERT INTO login_record (user_id, username, phone, login_type, login_ip, login_location, " +
            "device_info, browser, os, status, fail_reason, login_time) " +
            "VALUES (#{userId}, #{username}, #{phone}, #{loginType}, #{loginIp}, #{loginLocation}, " +
            "#{deviceInfo}, #{browser}, #{os}, #{status}, #{failReason}, #{loginTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LoginRecord record);

    /**
     * 更新登出时间
     */
    @Update("UPDATE login_record SET logout_time = #{logoutTime}, online_duration = #{onlineDuration} " +
            "WHERE id = #{id}")
    int updateLogoutTime(@Param("id") Long id,
                         @Param("logoutTime") String logoutTime,
                         @Param("onlineDuration") Long onlineDuration);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM login_record WHERE id = #{id}")
    LoginRecord selectById(Long id);

    /**
     * 根据用户ID查询
     */
    @Select("SELECT * FROM login_record WHERE user_id = #{userId} ORDER BY login_time DESC LIMIT #{limit}")
    List<LoginRecord> selectByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);

    /**
     * 获取用户最近一次登录记录
     */
    @Select("SELECT * FROM login_record WHERE user_id = #{userId} AND status = 'success' " +
            "ORDER BY login_time DESC LIMIT 1")
    LoginRecord selectLatestByUserId(@Param("userId") Integer userId);

    /**
     * 分页查询
     */
    List<LoginRecord> selectPage(@Param("offset") Integer offset,
                                  @Param("size") Integer size,
                                  @Param("userId") Integer userId,
                                  @Param("loginType") String loginType,
                                  @Param("status") String status,
                                  @Param("startTime") String startTime,
                                  @Param("endTime") String endTime);

    /**
     * 统计总数
     */
    Long countTotal(@Param("userId") Integer userId,
                    @Param("loginType") String loginType,
                    @Param("status") String status,
                    @Param("startTime") String startTime,
                    @Param("endTime") String endTime);

    /**
     * 今日登录人数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM login_record " +
            "WHERE DATE(login_time) = CURDATE() AND status = 'success'")
    Long countTodayLogin();

    /**
     * 获取登录统计
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as success_count, " +
            "SUM(CASE WHEN status = 'fail' THEN 1 ELSE 0 END) as fail_count " +
            "FROM login_record WHERE login_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)")
    Map<String, Object> selectLoginStats();

    /**
     * 获取每日登录统计
     */
    @Select("SELECT DATE(login_time) as date, COUNT(*) as count " +
            "FROM login_record WHERE status = 'success' " +
            "AND login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(login_time) ORDER BY date")
    List<Map<String, Object>> selectDailyStats();

    /**
     * 获取登录地点分布
     */
    @Select("SELECT login_location, COUNT(*) as count FROM login_record " +
            "WHERE status = 'success' AND login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY login_location ORDER BY count DESC LIMIT 10")
    List<Map<String, Object>> selectLocationStats();

    /**
     * 检测同一用户的异常登录（多地登录）
     */
    @Select("SELECT user_id, COUNT(DISTINCT login_ip) as ip_count, " +
            "GROUP_CONCAT(DISTINCT login_ip) as ips, " +
            "GROUP_CONCAT(DISTINCT login_location) as locations " +
            "FROM login_record WHERE status = 'success' " +
            "AND login_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
            "GROUP BY user_id HAVING ip_count > 2")
    List<Map<String, Object>> selectMultiIpLogins();

    /**
     * 获取用户最近的登录IP列表
     */
    @Select("SELECT DISTINCT login_ip, login_location, login_time FROM login_record " +
            "WHERE user_id = #{userId} AND status = 'success' " +
            "ORDER BY login_time DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecentIps(@Param("userId") Integer userId, @Param("limit") Integer limit);
}
