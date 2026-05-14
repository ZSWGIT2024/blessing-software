package com.itheima.service.impl;

import com.itheima.common.RedisConstants;
import com.itheima.pojo.Favorite;
import com.itheima.pojo.FavoriteFolder;
import com.itheima.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 收藏缓存服务
 * <p>
 * 对收藏夹列表、收藏列表、收藏媒体 ID 集合提供 Redis 缓存加速。
 * 防穿透：空值缓存（短 TTL）。
 * 防雪崩：TTL 加随机抖动（±30%）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteCacheService {

    private final RedisUtil redisUtil;

    private long jitter(long baseMinutes) {
        double factor = 0.7 + ThreadLocalRandom.current().nextDouble() * 0.6;
        return (long) (baseMinutes * factor);
    }

    // ==================== 收藏夹列表缓存 ====================

    @SuppressWarnings("unchecked")
    public void cacheFolders(Integer userId, List<FavoriteFolder> folders) {
        String key = String.format(RedisConstants.FAVORITE_FOLDERS_KEY, userId);
        try {
            redisUtil.set(key, folders, jitter(RedisConstants.FAVORITE_FOLDER_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存收藏夹列表失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<FavoriteFolder> getCachedFolders(Integer userId) {
        String key = String.format(RedisConstants.FAVORITE_FOLDERS_KEY, userId);
        try {
            Object obj = redisUtil.get(key);
            if (obj instanceof List) return (List<FavoriteFolder>) obj;
        } catch (Exception e) {
            log.error("读取收藏夹缓存失败 userId:{}", userId, e);
        }
        return null;
    }

    public void cacheEmptyFolders(Integer userId) {
        String key = String.format(RedisConstants.FAVORITE_FOLDERS_KEY, userId);
        try {
            redisUtil.set(key, "EMPTY", RedisConstants.NULL_VALUE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存收藏夹空值失败 userId:{}", userId, e);
        }
    }

    public void evictFoldersCache(Integer userId) {
        String key = String.format(RedisConstants.FAVORITE_FOLDERS_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 收藏列表缓存 ====================

    @SuppressWarnings("unchecked")
    public void cacheFavorites(Integer userId, Integer folderId, int pageNum, int pageSize, Object pageBean) {
        String key = String.format(RedisConstants.FAVORITE_LIST_KEY,
                userId, folderId == null ? "all" : folderId, pageNum, pageSize);
        try {
            redisUtil.set(key, pageBean, jitter(RedisConstants.FAVORITE_LIST_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存收藏列表失败 userId:{} folderId:{}", userId, folderId, e);
        }
    }

    public Object getCachedFavorites(Integer userId, Integer folderId, int pageNum, int pageSize) {
        String key = String.format(RedisConstants.FAVORITE_LIST_KEY,
                userId, folderId == null ? "all" : folderId, pageNum, pageSize);
        try {
            return redisUtil.get(key);
        } catch (Exception e) {
            log.error("读取收藏列表缓存失败 userId:{} folderId:{}", userId, folderId, e);
            return null;
        }
    }

    public void cacheEmptyFavorites(Integer userId, Integer folderId, int pageNum, int pageSize) {
        String key = String.format(RedisConstants.FAVORITE_LIST_KEY,
                userId, folderId == null ? "all" : folderId, pageNum, pageSize);
        try {
            redisUtil.set(key, "EMPTY", RedisConstants.NULL_VALUE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存收藏列表空值失败 userId:{} folderId:{}", userId, folderId, e);
        }
    }

    public void evictFavoritesByUser(Integer userId) {
        String pattern = String.format("favorite:list:user:%d:*", userId);
        redisUtil.deletePattern(pattern);
    }

    // ==================== 收藏媒体 ID 集合缓存 ====================

    @SuppressWarnings("unchecked")
    public void cacheFavoriteMediaIds(Integer userId, List<Integer> mediaIds) {
        String key = String.format(RedisConstants.FAVORITE_MEDIA_IDS_KEY, userId);
        try {
            redisUtil.set(key, mediaIds, jitter(RedisConstants.FAVORITE_IDS_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存收藏媒体ID列表失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getCachedFavoriteMediaIds(Integer userId) {
        String key = String.format(RedisConstants.FAVORITE_MEDIA_IDS_KEY, userId);
        try {
            Object obj = redisUtil.get(key);
            if (obj instanceof List) return (List<Integer>) obj;
        } catch (Exception e) {
            log.error("读取收藏媒体ID缓存失败 userId:{}", userId, e);
        }
        return null;
    }

    public void evictFavoriteMediaIds(Integer userId) {
        String key = String.format(RedisConstants.FAVORITE_MEDIA_IDS_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 批量驱逐 ====================

    /** 任何收藏/文件夹变更后调用，清除该用户所有收藏缓存 */
    public void evictAllFavoriteCache(Integer userId) {
        evictFoldersCache(userId);
        evictFavoritesByUser(userId);
        evictFavoriteMediaIds(userId);
    }
}
