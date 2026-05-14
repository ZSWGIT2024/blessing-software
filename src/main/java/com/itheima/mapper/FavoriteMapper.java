package com.itheima.mapper;

import com.itheima.pojo.Favorite;
import com.itheima.pojo.FavoriteFolder;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface FavoriteMapper {

    // ==================== Folder Methods ====================

    @Insert("INSERT INTO favorite_folder (user_id, folder_name, description, is_private, sort_order, create_time, update_time) " +
            "VALUES (#{userId}, #{folderName}, #{description}, #{isPrivate}, #{sortOrder}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFolder(FavoriteFolder folder);

    @Update("UPDATE favorite_folder SET folder_name = #{folderName}, description = #{description}, " +
            "is_private = #{isPrivate}, sort_order = #{sortOrder}, update_time = NOW() WHERE id = #{id}")
    int updateFolder(FavoriteFolder folder);

    @Delete("DELETE FROM favorite_folder WHERE id = #{id} AND user_id = #{userId}")
    int deleteFolder(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM favorite_folder WHERE id = #{id}")
    FavoriteFolder selectFolderById(@Param("id") Integer id);

    @Select("SELECT * FROM favorite_folder WHERE user_id = #{userId} ORDER BY sort_order")
    List<FavoriteFolder> selectFoldersByUser(@Param("userId") Integer userId);

    /** 查询用户的收藏夹（带收藏数量），通过 XML 实现 */
    List<FavoriteFolder> selectFoldersWithCount(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM favorite_folder WHERE user_id = #{userId}")
    int countFoldersByUser(@Param("userId") Integer userId);

    // ==================== Favorite Methods ====================

    @Insert("INSERT INTO favorite (user_id, media_id, folder_id, create_time) " +
            "VALUES (#{userId}, #{mediaId}, #{folderId}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Favorite favorite);

    @Delete("DELETE FROM favorite WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM favorite WHERE user_id = #{userId} AND media_id = #{mediaId}")
    Favorite selectByUserAndMedia(@Param("userId") Integer userId, @Param("mediaId") Integer mediaId);

    @Select("SELECT media_id FROM favorite WHERE user_id = #{userId}")
    List<Integer> selectUserFavoriteMediaIds(@Param("userId") Integer userId);

    @Delete("DELETE FROM favorite WHERE media_id = #{mediaId} AND user_id = #{userId}")
    int deleteByMediaId(@Param("mediaId") Integer mediaId, @Param("userId") Integer userId);

    @Select("SELECT f.id, f.user_id, f.media_id, f.folder_id, f.folder_name, f.tags, f.remark, f.is_private, f.create_time, f.update_time, " +
            "um.file_path as mediaFilePath, um.thumbnail_path as mediaThumbPath, " +
            "um.filename as mediaFilename, um.media_type as mediaType " +
            "FROM favorite f " +
            "LEFT JOIN user_media um ON f.media_id = um.id " +
            "WHERE f.folder_id = #{folderId} AND f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<Favorite> selectByFolder(@Param("folderId") Integer folderId,
                                  @Param("userId") Integer userId,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    @Select("SELECT f.id, f.user_id, f.media_id, f.folder_id, f.folder_name, f.tags, f.remark, f.is_private, f.create_time, f.update_time, " +
            "um.file_path as mediaFilePath, um.thumbnail_path as mediaThumbPath, " +
            "um.filename as mediaFilename, um.media_type as mediaType " +
            "FROM favorite f " +
            "LEFT JOIN user_media um ON f.media_id = um.id " +
            "WHERE f.folder_id = #{folderId} " +
            "ORDER BY f.create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<Favorite> selectByFolderPublic(@Param("folderId") Integer folderId,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    @Select("SELECT f.id, f.user_id, f.media_id, f.folder_id, f.folder_name, f.tags, f.remark, f.is_private, f.create_time, f.update_time, " +
            "um.file_path as mediaFilePath, um.thumbnail_path as mediaThumbPath, " +
            "um.filename as mediaFilename, um.media_type as mediaType " +
            "FROM favorite f " +
            "LEFT JOIN user_media um ON f.media_id = um.id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<Favorite> selectByUser(@Param("userId") Integer userId,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id = #{userId}")
    int countByUser(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM favorite WHERE folder_id = #{folderId}")
    int countByFolder(@Param("folderId") Integer folderId);

    @Update("UPDATE favorite SET folder_id = #{folderId} WHERE id = #{id}")
    int moveToFolder(@Param("id") Integer id, @Param("folderId") Integer folderId);

    @Update("UPDATE favorite SET folder_id = 0 WHERE id = #{id}")
    int removeFromFolder(@Param("id") Integer id);

    /** 批量移动收藏到指定收藏夹，通过 XML 实现 */
    int batchMoveToFolder(@Param("ids") List<Integer> ids, @Param("folderId") Integer folderId);

    @Update("UPDATE favorite SET folder_id = 0 WHERE folder_id = #{folderId}")
    int clearFolderByFolderId(@Param("folderId") Integer folderId);
}
