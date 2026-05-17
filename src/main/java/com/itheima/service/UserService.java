package com.itheima.service;

import com.itheima.dto.UserSimpleDTO;
import com.itheima.pojo.User;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserService {

    //根据手机号查询用户
    User findByPhone(String phone);
    //根据邮箱查询用户
    User findByEmail(String email);

    //注册用户
    User saveUser(String username, String phone, String password, String clientIp, String registerLocation, String registerSource);


    //更新用户信息
    void updateUser(UserSimpleDTO user);


    //更新用户头像
    void updateAvatar(@Param("avatarUrl") String avatarUrl);

    //更新用户密码
    void updatePwd(@Param("newPwd") String newPwd);


    //更新用户名
    void updateUsername(String username);

    //根据id获取用户信息
    User findUserById(Integer id);

    //更新用户登录信息
    void updateLoginInfo(User user);

    //更新用户邮箱（邮箱注册后回填）
    void updateUserEmail(Integer userId, String email);

    // 新增的管理员方法

    /**
     * 更新用户状态
     */
    boolean updateUserStatus(Integer id, String status);

    /**
     * 更新用户VIP信息
     */
    boolean updateUserVip(Integer id, Integer vipType, LocalDateTime expireTime);

    /**
     * 更新用户积分余额
     */
    boolean updateCoinBalance(Integer id, Integer coinBalance);


    /**
     * 更新用户等级和经验
     */
    boolean updateUserLevel(Integer id, Integer level, Long exp, Long nextLevelExp);

    /**
     * 搜索用户
     */
    List<User> searchUsers(String keyword);

    /**
     * 分页查询用户
     */
    Map<String, Object> getUserList(Integer page, Integer size, Map<String, Object> filters);

    /**
     * 获取用户统计信息
     */
    Map<String, Object> getUserStatistics();


    void addExperience(Integer id, String dailyLogin, String s);

    void updateLastActiveTime(Integer friendId);

    /**
     * 根据用户ID更新密码
     */
    void updatePasswordById(Integer userId, String newPassword);

    /**
     * 检查用户名是否可用（格式校验 + 数据库查重）
     * @param username 待检查的用户名
     * @param excludeUserId 排除的用户ID（编辑自身时排除自己）
     * @return {available: bool, message: str}
     */
    Map<String, Object> checkUsernameAvailability(String username, Integer excludeUserId);

}
