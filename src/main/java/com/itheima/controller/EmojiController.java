// EmojiController.java
package com.itheima.controller;

import com.itheima.dto.EmojiPackDTO;
import com.itheima.dto.EmojiPackItemDTO;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.Result;
import com.itheima.pojo.User;
import com.itheima.service.EmojiService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/emoji")
@RequiredArgsConstructor
public class EmojiController {

    private final EmojiService emojiService;

    private final UserMapper userMapper;

    /**
     * 获取系统Emoji分类
     */
    @GetMapping("/system/categories")
    public Result getSystemEmojiCategories() {
        return Result.success(emojiService.getSystemEmojiCategories());
    }

    /**
     * 获取分类下的系统Emoji
     */
    @GetMapping("/system/{category}")
    public Result getSystemEmojis(@PathVariable("category") String category) {
        return Result.success(emojiService.getSystemEmojis(category));
    }

    /**
     * 获取表情包列表
     */
    @GetMapping("/packs")
    public Result getEmojiPacks() {
        return Result.success(emojiService.getEmojiPacks());
    }

    /**
     * 获取表情包详情
     */
    @GetMapping("/packs/{packId}/items")
    public Result getEmojiPackItems(@PathVariable("packId") Long packId) {
        return Result.success(emojiService.getEmojiPackItems(packId));
    }

    // EmojiController.java - 在原有Controller中添加以下接口

    /**
     * 创建表情包
     */
    @PostMapping("/packs")
    public Result createEmojiPack(@RequestBody EmojiPackDTO emojiPackDTO) {
        Integer userId = getCurrentUserId();
        // 验证管理员权限（这里可以根据实际权限逻辑进行判断）
        User user = userMapper.findUserById(userId);
        if (!user.isAdmin()) {
            return Result.error("无权限操作");
        }
        return Result.success(emojiService.createEmojiPack(emojiPackDTO));
    }

    /**
     * 更新表情包信息
     */
    @PutMapping("/packs/{packId}")
    public Result updateEmojiPack(@PathVariable("packId") Long packId,
                                  @RequestBody EmojiPackDTO emojiPackDTO) {
        Integer userId = getCurrentUserId();
        emojiPackDTO.setId(packId);
        emojiService.updateEmojiPack(emojiPackDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除表情包
     */
    @DeleteMapping("/packs/{packId}")
    public Result deleteEmojiPack(@PathVariable("packId") Long packId) {
        Integer userId = getCurrentUserId();
        emojiService.deleteEmojiPack(packId);
        return Result.success("删除成功");
    }

    /**
     * 上传表情包封面图片
     */
    @PostMapping("/packs/cover")
    public Result uploadPackCover(@RequestParam("file") MultipartFile file) {
        String coverUrl = emojiService.uploadPackCover(file);
        return Result.success(coverUrl);
    }

    /**
     * 批量上传表情包图片
     */
    @PostMapping("/packs/{packId}/items/upload")
    public Result uploadPackItems(@PathVariable("packId") Long packId,
                                  @RequestParam("files") List<MultipartFile> files,
                                  @RequestParam(value = "descriptions", required = false) List<String> descriptions) {
        List<EmojiPackItemDTO> items = emojiService.uploadPackItems(packId, files, descriptions);
        return Result.success(items);
    }

    /**
     * 更新表情包图片信息
     */
    @PutMapping("/packs/items/{itemId}")
    public Result updatePackItem(@PathVariable("itemId") Long itemId,
                                 @RequestBody EmojiPackItemDTO itemDTO) {
        itemDTO.setId(itemId);
        emojiService.updatePackItem(itemDTO);
        return Result.success("更新成功");
    }

    /**
     * 删除表情包图片
     */
    @DeleteMapping("/packs/items/{itemId}")
    public Result deletePackItem(@PathVariable("itemId") Long itemId) {
        emojiService.deletePackItem(itemId);
        return Result.success("删除成功");
    }

    /**
     * 批量删除表情包图片
     */
    @DeleteMapping("/packs/items/batch")
    public Result batchDeletePackItems(@RequestParam("itemIds") List<Long> itemIds) {
        emojiService.batchDeletePackItems(itemIds);
        return Result.success("批量删除成功");
    }

    /**
     * 获取收藏列表
     * @param type 收藏类型 1:Emoji 2:表情包图片
     * @param page 页码
     * @param size 每页大小
     */
    @GetMapping("/favorites")
    public Result getFavorites(@RequestParam("type") Integer type,
                               @RequestParam(name = "page", defaultValue = "1") Integer page,
                               @RequestParam(name = "size", defaultValue = "20") Integer size) {
        return Result.success(emojiService.getFavorites(type, page, size));
    }

    /**
     * 添加收藏
     */
    @PostMapping("/favorites")
    public Result addFavorite(@RequestBody Map<String, Object> params) {
        Integer userId = getCurrentUserId();
        Integer type = (Integer) params.get("type");
        String emojiCode = (String) params.get("emojiCode");
        String emojiName = (String) params.get("emojiName");
        Long packItemId = params.get("packItemId") != null ?
                Long.parseLong(params.get("packItemId").toString()) : null;

        emojiService.addFavorite(userId, type, emojiCode, emojiName, packItemId);
        return Result.success("收藏成功");
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/favorites/{favoriteId}")
    public Result removeFavorite(@PathVariable("favoriteId") Long favoriteId) {
        Integer userId = getCurrentUserId();
        emojiService.removeFavorite(userId, favoriteId);
        return Result.success("取消收藏成功");
    }

    /**
     * 根据emoji名称取消收藏
     */
    @DeleteMapping("/favorites/emoji/{emojiName}")
    public Result removeFavoriteByEmoji(@PathVariable("emojiName") String emojiName) {
        Integer userId = getCurrentUserId();
        emojiService.removeFavoriteByEmoji(userId, emojiName);
        return Result.success("取消收藏成功");
    }

    /**
     * 获取最近使用
     * @param limit 获取数量
     */
    @GetMapping("/recent")
    public Result getRecentEmojis(@RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return Result.success(emojiService.getRecentEmojis(limit));
    }

    /**
     * 记录使用
     */
    @PostMapping("/usage")
    public Result recordUsage(@RequestBody Map<String, Object> params) {
        Integer userId = getCurrentUserId();
        Integer type = (Integer) params.get("type");
        String emojiCode = (String) params.get("emojiCode");
        String emojiName = (String) params.get("emojiName");
        Long packItemId = params.get("packItemId") != null ?
                Long.parseLong(params.get("packItemId").toString()) : null;

        emojiService.recordUsage(userId, type, emojiCode, emojiName, packItemId);
        return Result.success("记录成功");
    }

    /**
     * 清除最近使用
     * @param type 使用类型 1:Emoji 2:表情包图片
     */
    @DeleteMapping("/recent")
    public Result clearRecent(@RequestParam Integer type) {
        Integer userId = getCurrentUserId();
        emojiService.clearRecent(userId, type);
        return Result.success("清除成功");
    }

    private Integer getCurrentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("id");
    }
}