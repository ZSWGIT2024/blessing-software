// EmojiMapper.java
package com.itheima.mapper;

import com.itheima.pojo.EmojiPack;
import com.itheima.pojo.EmojiPackItem;
import com.itheima.pojo.UserEmojiFavorite;
import com.itheima.pojo.UserRecentEmoji;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface EmojiMapper {

    // ==================== 表情包相关 ====================

    @Select("SELECT * FROM emoji_pack WHERE status = 1 ORDER BY create_time DESC")
    List<EmojiPack> selectAllPacks();

    @Select("SELECT * FROM emoji_pack WHERE id = #{packId} AND status = 1")
    EmojiPack selectPackById(@Param("packId") Long packId);

    @Select("SELECT * FROM emoji_pack_item WHERE id = #{id} AND status = 1")
    EmojiPackItem selectPackItemById(@Param("id") Long id);

    @Select("SELECT * FROM emoji_pack_item WHERE pack_id = #{packId} AND status = 1 ORDER BY sort_order, create_time")
    List<EmojiPackItem> selectPackItems(@Param("packId") Long packId);

    @Select("SELECT COUNT(*) FROM emoji_pack_item WHERE pack_id = #{packId} AND status = 1")
    int countPackItems(@Param("packId") Long packId);

    // ==================== 表情包管理 ====================

    @Insert("INSERT INTO emoji_pack (pack_name, cover_url, description, status) " +
            "VALUES (#{packName}, #{coverUrl}, #{description}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertEmojiPack(EmojiPack emojiPack);

    @Update("UPDATE emoji_pack SET pack_name = #{packName}, cover_url = #{coverUrl}, " +
            "description = #{description} WHERE id = #{id}")
    int updateEmojiPack(EmojiPack emojiPack);

    @Delete("DELETE FROM emoji_pack WHERE id = #{packId}")
    int deleteEmojiPack(@Param("packId") Long packId);

// ==================== 表情包图片管理 ====================

    @Insert("INSERT INTO emoji_pack_item (pack_id, image_url, description, sort_order, status) " +
            "VALUES (#{packId}, #{imageUrl}, #{description}, #{sortOrder}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPackItem(EmojiPackItem item);

    @Update("UPDATE emoji_pack_item SET description = #{description} WHERE id = #{id}")
    int updatePackItem(EmojiPackItem item);

    @Delete("DELETE FROM emoji_pack_item WHERE id = #{itemId}")
    int deletePackItem(@Param("itemId") Long itemId);

    @Delete("DELETE FROM emoji_pack_item WHERE id IN (${itemIds})")
    int batchDeletePackItems(@Param("itemIds") String itemIds);


    // ==================== 收藏相关 ====================

    @Insert("INSERT INTO user_emoji_favorite (user_id, emoji_code, emoji_name, pack_item_id, favorite_type) " +
            "VALUES (#{userId}, #{emojiCode}, #{emojiName}, #{packItemId}, #{favoriteType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFavorite(UserEmojiFavorite favorite);

    @Delete("DELETE FROM user_emoji_favorite WHERE user_id = #{userId} AND pack_item_id = #{favoriteId}")
    int deleteFavorite(@Param("userId") Integer userId, @Param("favoriteId") Long favoriteId);

    @Delete("DELETE FROM user_emoji_favorite WHERE user_id = #{userId} AND emoji_name = #{emojiName}")
    int deleteFavoriteByEmoji(@Param("userId") Integer userId, @Param("emojiName") String emojiName);

    @Delete("DELETE FROM user_emoji_favorite WHERE user_id = #{userId} AND pack_item_id = #{packItemId}")
    int deleteFavoriteByPackItem(@Param("userId") Integer userId, @Param("packItemId") Long packItemId);

    @Select("SELECT * FROM user_emoji_favorite WHERE user_id = #{userId} AND favorite_type = 1 " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<UserEmojiFavorite> selectFavoriteEmojis(@Param("userId") Integer userId,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_emoji_favorite WHERE user_id = #{userId} AND favorite_type = 1")
    int countFavoriteEmojis(@Param("userId") Integer userId);

    @Select("SELECT f.*, i.image_url, i.description FROM user_emoji_favorite f " +
            "LEFT JOIN emoji_pack_item i ON f.pack_item_id = i.id " +
            "WHERE f.user_id = #{userId} AND f.favorite_type = 2 " +
            "ORDER BY f.create_time DESC LIMIT #{offset}, #{limit}")
    List<Map<String, Object>> selectFavoritePackItems(@Param("userId") Integer userId,
                                                      @Param("offset") int offset,
                                                      @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_emoji_favorite WHERE user_id = #{userId} AND favorite_type = 2")
    int countFavoritePackItems(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM user_emoji_favorite WHERE user_id = #{userId} AND emoji_name = #{emojiName}")
    int checkEmojiFavorite(@Param("userId") Integer userId, @Param("emojiName") String emojiName);

    @Select("SELECT COUNT(*) FROM user_emoji_favorite WHERE user_id = #{userId} AND pack_item_id = #{packItemId}")
    int checkPackItemFavorite(@Param("userId") Integer userId, @Param("packItemId") Long packItemId);

    // ==================== 最近使用相关 ====================

    @Select("SELECT * FROM user_recent_emoji WHERE user_id = #{userId} AND use_type = 1 " +
            "ORDER BY use_count DESC, last_use_time DESC LIMIT #{limit}")
    List<UserRecentEmoji> selectRecentEmojis(@Param("userId") Integer userId,
                                             @Param("limit") int limit);

    @Select("SELECT r.*, i.image_url, i.description FROM user_recent_emoji r " +
            "LEFT JOIN emoji_pack_item i ON r.pack_item_id = i.id " +
            "WHERE r.user_id = #{userId} AND r.use_type = 2 " +
            "ORDER BY r.use_count DESC, r.last_use_time DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecentPackItems(@Param("userId") Integer userId,
                                                    @Param("limit") int limit);

    // 插入新的emoji使用记录
    @Insert("INSERT INTO user_recent_emoji (user_id, emoji_code, emoji_name, pack_item_id, use_type, use_count, last_use_time) " +
            "VALUES (#{userId}, #{emojiCode}, #{emojiName}, #{packItemId}, #{useType}, 1, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserRecentEmoji recent);

    // 更新emoji使用次数（根据emojiCode）
    @Update("UPDATE user_recent_emoji SET use_count = use_count + 1, last_use_time = NOW() " +
            "WHERE user_id = #{userId} AND emoji_name = #{emojiName}")
    int updateCountByEmojiName(@Param("userId") Integer userId,
                               @Param("emojiName") String emojiName);

    // 更新表情包使用次数（根据packItemId）
    @Update("UPDATE user_recent_emoji SET use_count = use_count + 1, last_use_time = NOW() " +
            "WHERE user_id = #{userId} AND pack_item_id = #{packItemId}")
    int updateCountByPackItemId(@Param("userId") Integer userId,
                                @Param("packItemId") Long packItemId);

    //根据emojiCode查询最近使用的表情
    @Select("SELECT * FROM user_recent_emoji WHERE user_id = #{userId} AND emoji_code = #{emojiCode}")
    UserRecentEmoji selectRecentByEmojiCode(@Param("userId") Integer userId,
                                             @Param("emojiCode") String emojiCode);
    //根据表情包项ID查询最近使用的表情
    @Select("SELECT * FROM user_recent_emoji WHERE user_id = #{userId} AND pack_item_id = #{packItemId}")
    UserRecentEmoji selectRecentByPackItemId(@Param("userId") Integer userId,
                                             @Param("packItemId") Long packItemId);

    @Delete("DELETE FROM user_recent_emoji WHERE user_id = #{userId} AND use_type = #{useType}")
    int clearRecent(@Param("userId") Integer userId, @Param("useType") Integer useType);


}