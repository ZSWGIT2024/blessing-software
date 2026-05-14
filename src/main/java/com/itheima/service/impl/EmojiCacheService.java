package com.itheima.service.impl;

import com.itheima.common.RedisConstants;
import com.itheima.dto.EmojiPackDTO;
import com.itheima.dto.EmojiPackItemDTO;
import com.itheima.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 表情包缓存服务
 * <p>
 * 表情包分类、表情包列表、表情包内容均为低频变更数据，适合长 TTL 缓存。
 * 防穿透：空值缓存（短 TTL）。
 * 防雪崩：TTL 加随机抖动。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmojiCacheService {

    private final RedisUtil redisUtil;

    private long jitter(long baseMinutes) {
        double factor = 0.7 + ThreadLocalRandom.current().nextDouble() * 0.6;
        return (long) (baseMinutes * factor);
    }

    // ==================== 表情包列表缓存 ====================

    @SuppressWarnings("unchecked")
    public void cachePacks(List<EmojiPackDTO> packs) {
        try {
            redisUtil.set(RedisConstants.EMOJI_PACKS_KEY, packs,
                    jitter(RedisConstants.EMOJI_PACK_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存表情包列表失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<EmojiPackDTO> getCachedPacks() {
        try {
            Object obj = redisUtil.get(RedisConstants.EMOJI_PACKS_KEY);
            if (obj instanceof List) return (List<EmojiPackDTO>) obj;
        } catch (Exception e) {
            log.error("读取表情包列表缓存失败", e);
        }
        return null;
    }

    public void cacheEmptyPacks() {
        try {
            redisUtil.set(RedisConstants.EMOJI_PACKS_KEY, "EMPTY",
                    RedisConstants.NULL_VALUE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存表情包空值失败", e);
        }
    }

    /** 表情包增删改后调用 */
    public void evictPacks() {
        redisUtil.delete(RedisConstants.EMOJI_PACKS_KEY);
    }

    // ==================== 表情包内容缓存 ====================

    @SuppressWarnings("unchecked")
    public void cachePackItems(Long packId, List<EmojiPackItemDTO> items) {
        String key = String.format(RedisConstants.EMOJI_PACK_ITEMS_KEY, packId);
        try {
            redisUtil.set(key, items, jitter(RedisConstants.EMOJI_ITEM_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存表情包内容失败 packId:{}", packId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<EmojiPackItemDTO> getCachedPackItems(Long packId) {
        String key = String.format(RedisConstants.EMOJI_PACK_ITEMS_KEY, packId);
        try {
            Object obj = redisUtil.get(key);
            if (obj instanceof List) return (List<EmojiPackItemDTO>) obj;
        } catch (Exception e) {
            log.error("读取表情包内容缓存失败 packId:{}", packId, e);
        }
        return null;
    }

    public void cacheEmptyPackItems(Long packId) {
        String key = String.format(RedisConstants.EMOJI_PACK_ITEMS_KEY, packId);
        try {
            redisUtil.set(key, "EMPTY", RedisConstants.NULL_VALUE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存表情包内容空值失败 packId:{}", packId, e);
        }
    }

    public void evictPackItems(Long packId) {
        String key = String.format(RedisConstants.EMOJI_PACK_ITEMS_KEY, packId);
        redisUtil.delete(key);
    }

    // ==================== 批量驱逐 ====================

    /** 表情包变更（创建/更新/删除/上传图片）后调用 */
    public void evictAllEmojiPackCache(Long packId) {
        evictPacks();
        if (packId != null) evictPackItems(packId);
    }
}
