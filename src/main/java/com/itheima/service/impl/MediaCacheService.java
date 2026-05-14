package com.itheima.service.impl;

import com.itheima.common.RedisConstants;
import com.itheima.pojo.UserMedia;
import com.itheima.utils.RedisUtil;
import com.itheima.vo.MediaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 媒体资源缓存服务
 * <p>
 * 对 user_media 的列表查询、详情查询、统计查询提供 Redis 缓存加速。
 * 缓存策略：读时回源写缓存 + 写时主动驱逐。
 * 防穿透：空值缓存（短 TTL）。
 * 防雪崩：TTL 加随机抖动（±30%）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaCacheService {

    private final RedisUtil redisUtil;

    /**
     * 为 TTL 附加随机抖动，防止批量 key 同时过期引发缓存雪崩
     */
    private long jitter(long baseMinutes) {
        double factor = 0.7 + ThreadLocalRandom.current().nextDouble() * 0.6; // 0.7~1.3
        return (long) (baseMinutes * factor);
    }

    // ==================== 媒体列表缓存 ====================

    /**
     * 生成媒体列表缓存键（按状态过滤）
     */
    public String mediaListKey(Integer userId, String status, String mediaType, int pageNum, int pageSize) {
        return String.format(RedisConstants.MEDIA_LIST_KEY,
                userId, status == null ? "all" : status,
                mediaType == null ? "all" : mediaType, pageNum, pageSize);
    }

    public void cacheMediaList(String key, Object data) {
        try {
            long ttl = jitter(RedisConstants.MEDIA_LIST_TTL);
            redisUtil.set(key, data, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存媒体列表失败 key:{}", key, e);
        }
    }

    public Object getCachedMediaList(String key) {
        try {
            return redisUtil.get(key);
        } catch (Exception e) {
            log.error("读取媒体列表缓存失败 key:{}", key, e);
            return null;
        }
    }

    /** 缓存空值防止穿透 */
    public void cacheMediaListNull(String key) {
        try {
            redisUtil.set(key, "NULL", RedisConstants.NULL_VALUE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存媒体列表空值失败 key:{}", key, e);
        }
    }

    public void evictMediaListByUser(Integer userId) {
        String pattern = String.format("media:list:user:%d:*", userId);
        redisUtil.deletePattern(pattern);
        // 同时清除全状态列表
        String allStatusPattern = String.format("media:all_status:user:%d:*", userId);
        redisUtil.deletePattern(allStatusPattern);
    }

    // ==================== 媒体详情缓存 ====================

    public void cacheMediaDetail(Integer mediaId, MediaVO vo) {
        String key = String.format(RedisConstants.MEDIA_DETAIL_KEY, mediaId);
        try {
            redisUtil.set(key, vo, jitter(RedisConstants.MEDIA_DETAIL_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存媒体详情失败 mediaId:{}", mediaId, e);
        }
    }

    public MediaVO getCachedMediaDetail(Integer mediaId) {
        String key = String.format(RedisConstants.MEDIA_DETAIL_KEY, mediaId);
        try {
            Object obj = redisUtil.get(key);
            if (obj instanceof MediaVO) return (MediaVO) obj;
        } catch (Exception e) {
            log.error("读取媒体详情缓存失败 mediaId:{}", mediaId, e);
        }
        return null;
    }

    /** 不存在的媒体 ID 也缓存空值，防穿透 */
    public void cacheMediaDetailNull(Integer mediaId) {
        String key = String.format(RedisConstants.MEDIA_DETAIL_KEY, mediaId);
        try {
            redisUtil.set(key, "NULL", RedisConstants.NULL_VALUE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存媒体详情空值失败 mediaId:{}", mediaId, e);
        }
    }

    public void evictMediaDetail(Integer mediaId) {
        String key = String.format(RedisConstants.MEDIA_DETAIL_KEY, mediaId);
        redisUtil.delete(key);
    }

    // ==================== 最近上传缓存 ====================

    public void cacheRecentMedia(Integer userId, List<UserMedia> list) {
        String key = String.format(RedisConstants.MEDIA_RECENT_KEY, userId);
        try {
            redisUtil.set(key, list, jitter(RedisConstants.MEDIA_RECENT_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存最近上传失败 userId:{}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<UserMedia> getCachedRecentMedia(Integer userId) {
        String key = String.format(RedisConstants.MEDIA_RECENT_KEY, userId);
        try {
            Object obj = redisUtil.get(key);
            if (obj instanceof List) return (List<UserMedia>) obj;
        } catch (Exception e) {
            log.error("读取最近上传缓存失败 userId:{}", userId, e);
        }
        return null;
    }

    public void evictRecentMedia(Integer userId) {
        String key = String.format(RedisConstants.MEDIA_RECENT_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 媒体统计缓存 ====================

    public void cacheMediaStats(Integer userId, Object stats) {
        String key = String.format(RedisConstants.MEDIA_STATS_KEY, userId);
        try {
            redisUtil.set(key, stats, jitter(RedisConstants.MEDIA_STATS_TTL), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存媒体统计失败 userId:{}", userId, e);
        }
    }

    public Object getCachedMediaStats(Integer userId) {
        String key = String.format(RedisConstants.MEDIA_STATS_KEY, userId);
        try {
            return redisUtil.get(key);
        } catch (Exception e) {
            log.error("读取媒体统计缓存失败 userId:{}", userId, e);
            return null;
        }
    }

    public void evictMediaStats(Integer userId) {
        String key = String.format(RedisConstants.MEDIA_STATS_KEY, userId);
        redisUtil.delete(key);
    }

    // ==================== 批量驱逐 ====================

    /** 媒体变更（上传/删除/更新）时调用，清除该用户所有媒体相关缓存 */
    public void evictAllMediaCacheByUser(Integer userId) {
        evictMediaListByUser(userId);
        evictRecentMedia(userId);
        evictMediaStats(userId);
    }

    /** 单条媒体更新/删除时同时清除列表和详情缓存 */
    public void evictOnMediaChange(Integer mediaId, Integer userId) {
        evictMediaDetail(mediaId);
        evictMediaListByUser(userId);
        evictMediaStats(userId);
    }
}
