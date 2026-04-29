package com.itheima.mapper;

import com.itheima.dto.MediaQueryDTO;
import com.itheima.pojo.UserMedia;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MediaMapper {

    /**
     * 插入媒体记录
     */
    @Insert("INSERT INTO user_media (user_id, media_type, filename, original_name, file_path, " +
            "file_size,thumbnail_path,height,width,duration,wall, mime_type, description, category, is_public, upload_ip, " +
            "upload_time, update_time, status) " +
            "VALUES (#{userId}, #{mediaType}, #{filename}, #{originalName}, #{filePath}, " +
            "#{fileSize},#{thumbnailPath},#{height},#{width},#{duration},#{wall}, #{mimeType}, #{description}, #{category}, #{isPublic}, #{uploadIp}, " +
            "#{uploadTime}, #{updateTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserMedia userMedia);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM user_media WHERE id = #{id}")
    UserMedia selectById(Integer id);

    /**
     * 根据条件查询总数
     */
    Long countByCondition(MediaQueryDTO queryDTO);

    /**
     * 根据条件查询列表
     */
    List<UserMedia> selectByCondition(MediaQueryDTO queryDTO);

    /**
     * 根据用户ID和状态查询总记录数
     */
    Long countByUserAndStatus(MediaQueryDTO queryDTO);

    /**
     * 根据用户ID和状态查询数据列表
     */
    List<UserMedia> selectByUserAndStatus(MediaQueryDTO queryDTO);

    /**
     * 查询用户所有状态的作品总数（不筛选状态）
     */
    Long countAllStatusByUser(MediaQueryDTO queryDTO);

    /**
     * 查询用户所有状态的作品列表（不筛选状态）
     */
    List<UserMedia> selectAllStatusByUser(MediaQueryDTO queryDTO);

    /**
     * 更新媒体信息
     */
    @Update("<script>" +
            "UPDATE user_media " +
            "<set>" +
            "  <if test='filename != null'>filename = #{filename},</if>" +
            "  <if test='description != null'>description = #{description},</if>" +
            "  <if test='category != null'>category = #{category},</if>" +
            "  <if test='isPublic != null'>is_public = #{isPublic},</if>" +
            "  <if test='status != null'>status = #{status},</if>" +
            "  update_time = #{updateTime}" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(UserMedia userMedia);

    /**
     * 更新状态
     */
    @Update("UPDATE user_media SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    //删除媒体
    @Delete("DELETE FROM user_media WHERE id = #{id}")
    int delete(Integer id);

    //更新该媒体的点赞状态，0代表false未点赞，1代表true已点赞
    @Update("UPDATE user_media SET is_liked = #{isLiked} WHERE id = #{id}")
    int updateLikeStatus(@Param("id") Integer id, @Param("isLiked") Integer isLiked);

    /**
     * 增加查看次数
     */
    @Update("UPDATE user_media SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Integer id);

    /**
     * 增加点赞数
     */
    @Update("UPDATE user_media SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(Integer id);

    /**
     * 减少点赞数
     */
    @Update("UPDATE user_media SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(Integer id);


    /**
     * 查询用户最近上传
     */
    @Select("SELECT * FROM user_media WHERE user_id = #{userId} AND status = 'active' or status = 'pending' " +
            "ORDER BY upload_time DESC LIMIT #{limit}")
    List<UserMedia> selectRecentByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);

    //批量更新媒体状态
    int batchUpdateStatus(@Param("ids") List<Integer> ids, @Param("status") String status, @Param("tags") String tags);
}
