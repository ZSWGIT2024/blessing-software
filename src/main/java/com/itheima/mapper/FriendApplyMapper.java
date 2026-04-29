package com.itheima.mapper;

import com.itheima.pojo.FriendApply;
import com.itheima.pojo.User;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FriendApplyMapper {

    // 创建好友申请
    @Insert("INSERT INTO friend_apply (applicant_id, receiver_id, apply_msg, status, expire_time) " +
            "VALUES (#{applicantId}, #{receiverId}, #{applyMsg}, #{status}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FriendApply apply);

    // 更新申请状态
    @Update("UPDATE friend_apply SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // 获取申请详情
    @Select("SELECT * FROM friend_apply WHERE id = #{id}")
    FriendApply selectById(@Param("id") Long id);

    // 检查是否有待处理的申请
    @Select("SELECT COUNT(*) FROM friend_apply " +
            "WHERE applicant_id = #{applicantId} AND receiver_id = #{receiverId} AND status = 'pending'")
    int existsPendingApply(@Param("applicantId") Integer applicantId, @Param("receiverId") Integer receiverId);

    // 查询我发出的申请
    @Select("SELECT a.*, r.* FROM friend_apply a " +
            "LEFT JOIN user r ON a.receiver_id = r.id " +
            "WHERE a.applicant_id = #{userId} AND a.status = #{status} " +
            "ORDER BY a.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    @Results({
            @Result(property = "receiver", column = "receiver_id", javaType = User.class,
                    one = @One(select = "com.itheima.mapper.UserMapper.selectById"))
    })
    List<FriendApply> selectMyApplies(@Param("userId") Integer userId,
                                      @Param("status") String status,
                                      @Param("offset") Integer offset,
                                      @Param("pageSize") Integer pageSize);

    // 查询我收到的申请
    @Select("SELECT a.*, app.* FROM friend_apply a " +
            "LEFT JOIN user app ON a.applicant_id = app.id " +
            "WHERE a.receiver_id = #{userId} AND a.status = #{status} " +
            "ORDER BY a.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}")
    @Results({
            @Result(property = "applicant", column = "applicant_id", javaType = User.class,
                    one = @One(select = "com.itheima.mapper.UserMapper.selectById"))
    })
    List<FriendApply> selectReceivedApplies(@Param("userId") Integer userId,
                                            @Param("status") String status,
                                            @Param("offset") Integer offset,
                                            @Param("pageSize") Integer pageSize);

    // 更新过期申请
    @Update("UPDATE friend_apply SET status = 'expired' WHERE status = 'pending' AND expire_time < #{now}")
    int updateExpiredApplies(@Param("now") LocalDateTime now);

    // 查询待处理的申请
    @Select("SELECT * FROM friend_apply " +
            "WHERE applicant_id = #{userId} AND receiver_id = #{currentUserId} AND status = 'pending'")
    FriendApply findPendingApply(Integer currentUserId, Integer userId);
}