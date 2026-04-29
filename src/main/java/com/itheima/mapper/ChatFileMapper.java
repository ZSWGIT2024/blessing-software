package com.itheima.mapper;


import com.itheima.dto.ChatFileDTO;
import com.itheima.pojo.ChatFile;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ChatFileMapper {

    //根据消息ID查询文件
    @Select("select * from chat_file where message_id = #{messageId}")
    ChatFile selectByMessageId(@Param("messageId") String messageId);

    //根据fileId获取文件信息ChatFileDTO
    @Select("select * from chat_file where id = #{fileId}")
    ChatFileDTO selectById(Long fileId);

    //新增聊天文件
    @Insert("insert into chat_file(uploader_id,receiver_id,file_name,file_path,file_type,file_size,thumbnail_path,message_id,create_time,update_time) " +
            "values(#{uploaderId},#{receiverId},#{fileName},#{filePath},#{fileType},#{fileSize},#{thumbnailPath},#{messageId},now(),now())")
    void insert(ChatFile chatFile);

    //更新消息ID
    @Update("update chat_file set message_id = #{messageId} where id = #{id}")
    void updateMessageId(@Param("id") Long id,@Param("messageId") String messageId);

    @Delete("delete from chat_file where message_id = #{messageId}")
    void deleteByMessageId(String messageId);
}
