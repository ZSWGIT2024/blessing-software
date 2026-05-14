package com.itheima.mapper;

import com.itheima.pojo.GroupChatFile;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 群组/世界聊天文件 Mapper 接口
 */
@Mapper
public interface GroupChatFileMapper {

    @Insert("INSERT INTO group_file (uploader_id, group_id, chat_type, file_name, file_path, " +
            "file_type, file_size, thumbnail_path, message_id, create_time) VALUES (#{uploaderId}, " +
            "#{groupId}, #{chatType}, #{fileName}, #{filePath}, #{fileType}, #{fileSize}, " +
            "#{thumbnailPath}, #{messageId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GroupChatFile file);

    @Select("SELECT * FROM group_file WHERE id = #{id} AND is_deleted = 0")
    GroupChatFile selectById(@Param("id") Long id);

    @Select("SELECT * FROM group_file WHERE uploader_id = #{uploaderId} AND is_deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<GroupChatFile> selectByUploader(@Param("uploaderId") Integer uploaderId,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);

    @Select("SELECT * FROM group_file WHERE group_id = #{groupId} AND is_deleted = 0 " +
            "ORDER BY create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<GroupChatFile> selectByGroup(@Param("groupId") String groupId,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);

    @Update("UPDATE group_file SET is_deleted = 1 WHERE id = #{id}")
    int softDelete(@Param("id") Long id);

    @Delete("DELETE FROM group_file WHERE id = #{id}")
    int delete(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM group_file WHERE uploader_id = #{uploaderId} AND is_deleted = 0")
    int countByUploader(@Param("uploaderId") Integer uploaderId);
}
