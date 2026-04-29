// mapper/UserDailyUploadMapper.java
package com.itheima.mapper;

import com.itheima.pojo.UserDailyUpload;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;

@Mapper
public interface UserDailyUploadMapper {

    @Select("SELECT * FROM user_daily_upload WHERE user_id = #{userId} AND upload_date = #{uploadDate}")
    UserDailyUpload selectByUserAndDate(@Param("userId") Integer userId,
                                        @Param("uploadDate") LocalDate uploadDate);

    @Insert("INSERT INTO user_daily_upload(user_id, upload_date, upload_count, create_time, update_time) " +
            "VALUES(#{userId}, #{uploadDate}, #{uploadCount}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserDailyUpload userDailyUpload);

    @Update("UPDATE user_daily_upload SET upload_count = #{uploadCount}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    int updateById(UserDailyUpload userDailyUpload);

    @Update("UPDATE user_daily_upload SET upload_count = upload_count + #{increment} " +
            "WHERE user_id = #{userId} AND upload_date = #{uploadDate}")
    int incrementCount(@Param("userId") Integer userId,
                       @Param("uploadDate") LocalDate uploadDate,
                       @Param("increment") Integer increment);

    @Delete("DELETE FROM user_daily_upload WHERE upload_date < #{date}")
    int deleteByDateBefore(@Param("date") LocalDate date);
}