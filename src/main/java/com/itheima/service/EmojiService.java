// EmojiService.java
package com.itheima.service;

import com.itheima.dto.*;
import com.itheima.pojo.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface EmojiService {

    /**
     * 获取系统Emoji分类
     */
    List<Map<String, Object>> getSystemEmojiCategories();

    /**
     * 获取分类下的系统Emoji
     */
    List<EmojiDTO> getSystemEmojis(String category);

    /**
     * 获取表情包列表
     */
    List<EmojiPackDTO> getEmojiPacks();

    /**
     * 获取表情包详情
     */
    List<EmojiPackItemDTO> getEmojiPackItems(Long packId);


    // EmojiService.java - 在原有Service接口中添加以下方法

    /**
     * 创建表情包
     */
    EmojiPackDTO createEmojiPack(EmojiPackDTO emojiPackDTO);

    /**
     * 更新表情包
     */
    void updateEmojiPack(EmojiPackDTO emojiPackDTO);

    /**
     * 删除表情包
     */
    void deleteEmojiPack(Long packId);

    /**
     * 上传表情包封面
     */
    String uploadPackCover(MultipartFile file);

    /**
     * 批量上传表情包图片
     */
    List<EmojiPackItemDTO> uploadPackItems(Long packId, List<MultipartFile> files, List<String> descriptions);

    /**
     * 更新表情包图片
     */
    void updatePackItem(EmojiPackItemDTO itemDTO);

    /**
     * 删除表情包图片
     */
    void deletePackItem(Long itemId);

    /**
     * 批量删除表情包图片
     */
    void batchDeletePackItems(List<Long> itemIds);

    /**
     * 获取收藏列表（分页）
     * @param type 收藏类型 1:Emoji 2:表情包图片
     * @param page 页码
     * @param pageSize 每页大小
     */
    Map<String, Object> getFavorites(Integer type, Integer page, Integer pageSize);

    /**
     * 添加收藏
     * @param userId 用户ID
     * @param type 收藏类型 1:Emoji 2:表情包图片
     * @param emojiCode Emoji代码（type=1时必填）
     * @param packItemId 表情包项ID（type=2时必填）
     */
    void addFavorite(Integer userId, Integer type, String emojiCode, String emojiName, Long packItemId);

    /**
     * 取消收藏
     * @param userId 用户ID
     * @param favoriteId 收藏记录ID
     */
    void removeFavorite(Integer userId, Long favoriteId);


    void removeFavoriteByEmoji(Integer userId, String emojiName);

    /**
     * 获取最近使用
     * @param limit 获取数量
     */
    Map<String, Object> getRecentEmojis(Integer limit);

    /**
     * 记录使用
     * @param userId 用户ID
     * @param type 使用类型 1:Emoji 2:表情包图片
     * @param emojiCode Emoji代码（type=1时必填）
     * @param packItemId 表情包项ID（type=2时必填）
     */
    void recordUsage(Integer userId, Integer type, String emojiCode, String emojiName, Long packItemId);

    /**
     * 清除最近使用
     * @param userId 用户ID
     * @param type 使用类型 1:Emoji 2:表情包图片
     */
    void clearRecent(Integer userId, Integer type);


}