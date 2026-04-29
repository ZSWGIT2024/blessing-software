package com.itheima.mapper;


import com.itheima.pojo.MediaLike;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SubmitLikeMapper {

    /**
     * 插入点赞记录
     */
    @Insert("INSERT INTO submit_like (media_id, user_id, create_time) " +
            "VALUES (#{mediaId}, #{userId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MediaLike mediaLike);

    /**
     * 根据ID删除
     */
    @Delete("DELETE FROM submit_like WHERE id = #{id}")
    int delete(Integer id);

    /**
     * 查询用户对某个媒体的点赞记录
     */
    @Select("SELECT * FROM submit_like WHERE media_id = #{mediaId} AND user_id = #{userId}")
    MediaLike selectByMediaAndUser(@Param("mediaId") Integer mediaId, @Param("userId") Integer userId);

    /**
     * 统计用户对某个媒体的点赞次数
     */
    @Select("SELECT COUNT(*) FROM submit_like WHERE media_id = #{mediaId} AND user_id = #{userId}")
    Integer countByMediaAndUser(@Param("mediaId") Integer mediaId, @Param("userId") Integer userId);
}
