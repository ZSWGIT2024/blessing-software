package com.itheima.mapper;

import com.itheima.dto.UserSimpleDTO;
import com.itheima.pojo.User;
import com.itheima.pojo.UserDailyUpload;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    // 根据用户id查询用户
    @Select("select * from user where id = #{id}")
    User findUserById(Integer id);

    // 根据用户名查询用户
    @Select("select * from user where username = #{username}")
    User findByUsername(String username);

    // 根据手机号查询用户
    @Select("select * from user where phone = #{phone}")
    User findByPhone(String phone);

    // 根据邮箱查询用户
    @Select("select * from user where email = #{email}")
    User findByEmail(String email);

    // 添加用户到数据库（需要添加更多字段）
    @Insert("insert into user(username, phone, password,avatar, nickname, email, user_type, status, " +
            "level, exp, next_level_exp, vip_type, register_ip, register_location, register_source, " +
            "create_time, update_time) " +
            "values(#{username}, #{phone}, #{password},#{avatar}, #{nickname}, #{email}, " +
            "#{userType}, #{status}, #{level}, #{exp}, #{nextLevelExp}, #{vipType}, " +
            "#{registerIp}, #{registerLocation}, #{registerSource}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void addUser(User user);

    // 更新用户信息（已有，但需要添加缺失的字段）
    void updateUser(UserSimpleDTO user);

    // 更新用户头像
    @Update("update user set avatar = #{avatarUrl}, update_time = now() where id = #{id}")
    void updateAvatar(@Param("id") Integer id, @Param("avatarUrl") String avatarUrl);

    // 更新用户密码
    @Update("update user set password = #{newPwd}, update_time = now() where id = #{id}")
    void updatePwd(@Param("id") Integer id, @Param("newPwd") String newPwd);

    // 更新用户名
    @Update("update user set username = #{username}, update_time = now() where id = #{id}")
    void updateUsername(@Param("id") Integer id, @Param("username") String username);

    // 更新用户邮箱
    @Update("update user set email = #{email}, update_time = now() where id = #{id}")
    void updateEmail(@Param("id") Integer id, @Param("email") String email);

    /**
     * 更新用户在线状态
     */
    @Update("UPDATE user SET is_online = #{status} WHERE id = #{userId}")
    int updateUserOnlineStatus(@Param("userId") Integer userId, @Param("status") String status);

    /**
     * 查询已过期的VIP用户
     */
    @Select("SELECT * FROM user " +
            "WHERE vip_type IN (1, 2, 3) " +  // 月度、季度、年度VIP
            "AND vip_expire_time IS NOT NULL " +
            "AND vip_expire_time < #{now} " +
            "AND status = 'active'")
    List<User> selectExpiredVipUsers(@Param("now") LocalDateTime now);

    /**
     * 更新用户VIP状态
     */
    @Update("UPDATE user SET vip_type = #{vipType}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int updateVipType(@Param("id") Integer id,
                      @Param("vipType") Integer vipType,
                      @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新用户统计信息（上传数量等）
     */
    @Update("UPDATE user SET upload_count = #{uploadCount}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int updateById(User user);

    // ===== 新增的方法 =====

    /**
     * 更新用户登录信息
     */
    @Update("UPDATE user SET last_login_time = #{lastLoginTime}, " +
            "last_login_ip = #{lastLoginIp}, last_active_time = #{lastActiveTime}, " +
            "login_days = #{loginDays}, total_login_days = #{totalLoginDays}, " +
            "last_login_date = #{lastLoginDate}, update_time = now() " +
            "WHERE id = #{id}")
    int updateLoginInfo(User user);

    /**
     * 更新用户积分/金币余额
     */
    @Update("UPDATE user SET coin_balance = #{coinBalance}, update_time = now() " +
            "WHERE id = #{id}")
    int updateCoinBalance(@Param("id") Integer id, @Param("coinBalance") Integer coinBalance);


    /**
     * 增加用户经验值
     */
    @Update("UPDATE user SET exp = exp + #{exp}, level = #{level}, " +
            "next_level_exp = #{nextLevelExp}, update_time = now() " +
            "WHERE id = #{id}")
    int updateExperience(@Param("id") Integer id,
                         @Param("exp") Long exp,
                         @Param("level") Integer level,
                         @Param("nextLevelExp") Long nextLevelExp);

    /**
     * 更新用户状态（封禁/解封）
     */
    @Update("UPDATE user SET status = #{status}, update_time = now() " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    /**
     * 增加用户积分（原子操作）
     */
    @Update("UPDATE user SET coin_balance = coin_balance + #{amount}, update_time = now() WHERE id = #{id}")
    int addCoinBalance(@Param("id") Integer id, @Param("amount") Integer amount);

    /**
     * 扣除用户积分（原子操作，带检查）
     */
    @Update("UPDATE user SET coin_balance = coin_balance - #{amount}, update_time = now() " +
            "WHERE id = #{id} AND coin_balance >= #{amount}")
    int deductCoinBalance(@Param("id") Integer id, @Param("amount") Integer amount);

    /**
     * 获取用户积分余额（单独查询）
     */
    @Select("SELECT coin_balance FROM user WHERE id = #{id}")
    Integer getCoinBalance(@Param("id") Integer id);

    /**
     * 更新VIP信息（包含类型和过期时间）
     */
    @Update("UPDATE user SET vip_type = #{vipType}, vip_expire_time = #{vipExpireTime}, " +
            "update_time = now() WHERE id = #{id}")
    int updateVipInfo(@Param("id") Integer id,
                      @Param("vipType") Integer vipType,
                      @Param("vipExpireTime") LocalDateTime vipExpireTime);


    // 修改 findPage 和 countUsers 方法，添加昵称和邮箱搜索：
    List<User> findPage(@Param("offset") Integer offset,
                        @Param("limit") Integer limit,
                        @Param("keyword") String keyword,
                        @Param("vipType") Integer vipType,
                        @Param("status") String status,
                        @Param("minLevel") Integer minLevel,
                        @Param("maxLevel") Integer maxLevel);

    Long countUsers(@Param("keyword") String keyword,
                    @Param("vipType") Integer vipType,
                    @Param("status") String status);

    /**
     * 用户统计信息
     */
    @Select("SELECT " +
            "COUNT(*) as total_users, " +
            "SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active_users, " +
            "SUM(CASE WHEN status = 'banned' THEN 1 ELSE 0 END) as banned_users, " +
            "SUM(CASE WHEN vip_type > 0 THEN 1 ELSE 0 END) as vip_users, " +
            "SUM(CASE WHEN DATE(create_time) = CURDATE() THEN 1 ELSE 0 END) as today_new_users " +
            "FROM user")
    Map<String, Object> getUserStats();


    // 在UserMapper中添加以下方法：

    /**
     * VIP类型分布统计
     */
    @Select("SELECT " +
            "SUM(CASE WHEN vip_type = 0 THEN 1 ELSE 0 END) as none_vip, " +
            "SUM(CASE WHEN vip_type = 1 THEN 1 ELSE 0 END) as monthly_vip, " +
            "SUM(CASE WHEN vip_type = 2 THEN 1 ELSE 0 END) as quarterly_vip, " +
            "SUM(CASE WHEN vip_type = 3 THEN 1 ELSE 0 END) as yearly_vip, " +
            "SUM(CASE WHEN vip_type = 4 THEN 1 ELSE 0 END) as lifetime_vip " +
            "FROM user WHERE status = 'active'")
    Map<String, Object> getVipTypeStats();

    /**
     * 月度活跃用户统计（最近30天有登录的用户）
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM ( " +
            "SELECT id as user_id FROM user WHERE last_login_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "UNION " +
            "SELECT user_id FROM user_exp_record WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "UNION " +
            "SELECT user_id FROM coin_transaction WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            ") as active_users")
    Long countMonthlyActiveUsers();

    /**
     * 今日活跃用户统计（今日有登录的用户）
     */
    @Select("SELECT COUNT(*) FROM user " +
            "WHERE DATE(last_login_time) = CURDATE() " +
            "AND status = 'active'")
    Long countTodayActiveUsers();

    /**
     * 用户等级分布统计
     */
    @Select("SELECT " +
            "level, COUNT(*) as user_count " +
            "FROM user " +
            "WHERE status = 'active' " +
            "GROUP BY level " +
            "ORDER BY level")
    List<Map<String, Object>> getUserLevelDistribution();

    /**
     * 用户增长趋势（按天统计）
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as new_users " +
            "FROM user " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date DESC")
    List<Map<String, Object>> getUserGrowthTrend();

    /**
     * 用户上传统计
     */
    @Select("SELECT " +
            "SUM(upload_count) as total_uploads, " +
            "AVG(upload_count) as avg_uploads, " +
            "MAX(upload_count) as max_uploads " +
            "FROM user " +
            "WHERE status = 'active'")
    Map<String, Object> getUserUploadStats();

    /**
     * 用户积分统计
     */
    @Select("SELECT " +
            "SUM(coin_balance) as total_coins, " +
            "AVG(coin_balance) as avg_coins, " +
            "MAX(coin_balance) as max_coins " +
            "FROM user " +
            "WHERE status = 'active'")
    Map<String, Object> getUserCoinStats();

    /**
     * 用户行为统计
     */
    @Select("SELECT " +
            "SUM(follow_count) as total_follows, " +
            "SUM(follower_count) as total_followers, " +
            "SUM(liked_count) as total_likes_received, " +
            "SUM(comment_count) as total_comments, " +
            "SUM(favorite_count) as total_favorites " +
            "FROM user " +
            "WHERE status = 'active'")
    Map<String, Object> getUserBehaviorStats();


    /**
     * 增加用户评论数
     */
    @Update("UPDATE user SET comment_count = comment_count + 1 WHERE id = #{userId}")
    int incrementCommentCount(@Param("userId") Integer userId);

    /**
     * 减少用户评论数
     */
    @Update("UPDATE user SET comment_count = comment_count - 1 WHERE id = #{userId}")
    int decrementCommentCount(@Param("userId") Integer userId);

    /**
     * 增加用户获赞数
     */
    @Update("UPDATE user SET liked_count = liked_count + 1 WHERE id = #{userId}")
    int incrementLikedCount(@Param("userId") Integer userId);

    /**
     * 减少用户获赞数
     */
    @Update("UPDATE user SET liked_count = liked_count - 1 WHERE id = #{userId}")
    int decrementLikedCount(@Param("userId") Integer userId);

    // 更新关注数
    @Update("UPDATE user SET follow_count = #{followCount}, update_time = NOW() WHERE id = #{userId}")
    int updateFollowCount(@Param("userId") Integer userId, @Param("followCount") Integer followCount);

    // 更新粉丝数
    @Update("UPDATE user SET follower_count = #{followerCount}, update_time = NOW() WHERE id = #{userId}")
    int updateFollowerCount(@Param("userId") Integer userId, @Param("followerCount") Integer followerCount);

    // 更新最后活跃时间
    @Update("UPDATE user SET last_active_time = #{lastActiveTime}, update_time = NOW() WHERE id = #{userId}")
    int updateLastActiveTime(@Param("userId") Integer userId,
                             @Param("lastActiveTime") LocalDateTime lastActiveTime);

    List<User> selectByIds(List<Integer> userIds);

    Integer selectCurrentFrameId(Integer userId);


    // 新增的统计相关方法

    /**
     * 统计总用户数
     */
    @Select("SELECT COUNT(*) FROM user")
    Integer countTotalUsers();

    /**
     * 统计活跃用户数（最近N天登录过）
     * @param days 天数
     */
    @Select("SELECT COUNT(DISTINCT u.id) FROM user u " +
            "INNER JOIN user_event_statistics e ON u.id = e.user_id " +
            "WHERE e.event_type = 'login' " +
            "AND e.event_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    Integer countActiveUsers(@Param("days") int days);

    /**
     * 统计VIP用户数
     */
    @Select("SELECT COUNT(*) FROM user WHERE vip_type > 0")
    Integer countVipUsers();

    /**
     * 统计封禁用户数
     */
    @Select("SELECT COUNT(*) FROM user WHERE status = 'banned'")
    Integer countBannedUsers();

    /**
     * 根据日期统计总用户数
     */
    @Select("SELECT COUNT(*) FROM user WHERE DATE(create_time) <= #{date}")
    Integer countTotalUsersByDate(@Param("date") LocalDate date);

    /**
     * 根据日期统计新增用户
     */
    @Select("SELECT COUNT(*) FROM user WHERE DATE(create_time) = #{date}")
    Integer countNewUsersByDate(@Param("date") LocalDate date);

    /**
     * 根据日期统计活跃用户
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_event_statistics " +
            "WHERE event_type = 'login' AND DATE(event_time) = #{date}")
    Integer countActiveUsersByDate(@Param("date") LocalDate date);

    /**
     * 根据日期统计VIP用户
     */
    @Select("SELECT COUNT(*) FROM user WHERE vip_type > 0 AND DATE(create_time) <= #{date}")
    Integer countVipUsersByDate(@Param("date") LocalDate date);

    /**
     * 根据日期统计封禁用户
     */
    @Select("SELECT COUNT(*) FROM user WHERE status = 'banned' AND DATE(create_time) <= #{date}")
    Integer countBannedUsersByDate(@Param("date") LocalDate date);

    /**
     * 根据日期统计总登录次数
     */
    @Select("SELECT COUNT(*) FROM user_event_statistics " +
            "WHERE event_type = 'login' AND DATE(event_time) = #{date}")
    Integer countTotalLoginsByDate(@Param("date") LocalDate date);

    /**
     * 根据日期统计独立登录用户数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_event_statistics " +
            "WHERE event_type = 'login' AND DATE(event_time) = #{date}")
    Integer countUniqueLoginsByDate(@Param("date") LocalDate date);

    /**
     * 根据日期计算平均等级和积分
     */
    @Select("SELECT " +
            "COALESCE(AVG(level), 0) as avg_level, " +
            "COALESCE(AVG(coin_balance), 0) as avg_coins " +
            "FROM user " +
            "WHERE DATE(create_time) <= #{date} AND status = 'active'")
    Map<String, Object> getAverageMetricsByDate(@Param("date") LocalDate date);
}