package com.itheima.mapper;

import com.itheima.pojo.UserAvatarFrame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface UserAvatarFrameMapper {

    // 获取用户已解锁的头像框ID列表
    @Select("SELECT frame_id FROM user_avatar_frame WHERE user_id = #{userId}")
    List<Integer> selectUnlockedFrameIdsByUserId(Integer userId);

    // 获取用户当前正在使用的头像框ID
    @Select("SELECT frame_id FROM user_avatar_frame WHERE user_id = #{userId} AND is_using = 1 LIMIT 1")
    Integer selectUsingFrameIdByUserId(Integer userId);
    // 查询用户所有头像框（带详情）
    List<UserAvatarFrame> selectUserFramesWithDetail(Integer userId);

    // 更新使用状态
    int updateUsingStatus(@Param("userId") Integer userId,
                          @Param("frameId") Integer frameId);

    // 检查关联是否存在
    int exists(@Param("userId") Integer userId,
               @Param("frameId") Integer frameId);

    // 插入新关联
    int insert(UserAvatarFrame userFrame);
}

