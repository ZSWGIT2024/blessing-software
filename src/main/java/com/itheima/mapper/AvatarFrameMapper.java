package com.itheima.mapper;

import com.itheima.pojo.AvatarFrame;
import com.itheima.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AvatarFrameMapper {
    // 根据ID查询头像框
    AvatarFrame selectById(Integer id);

    // 查询用户当前使用的头像框
    AvatarFrame selectCurrentFrameByUserId(Integer userId);

    // 查询符合条件的可用头像框
    List<AvatarFrame> selectAvailableFramesByUser(
            @Param("level") Integer level,
            @Param("vipType") Integer vipType,
            @Param("loginDays") Integer loginDays
    );

    // 其他必要方法...
    int insert(AvatarFrame frame);
    int update(AvatarFrame frame);

    // 获取所有头像框
    @Select("SELECT * FROM avatar_frame ORDER BY create_time")
    List<AvatarFrame> getAllFrames();
}
