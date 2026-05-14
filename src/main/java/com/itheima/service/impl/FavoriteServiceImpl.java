package com.itheima.service.impl;

import com.itheima.mapper.FavoriteMapper;
import com.itheima.pojo.Favorite;
import com.itheima.pojo.FavoriteFolder;
import com.itheima.pojo.PageBean;
import com.itheima.service.FavoriteService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final FavoriteCacheService favoriteCacheService;

    private static final int MAX_FOLDERS_PER_USER = 20;

    // ==================== Folder Operations ====================

    @Override
    @Transactional
    public FavoriteFolder createFolder(String folderName, String description, Boolean isPrivate, Integer sortOrder) {
        Integer userId = getUserId();

        if (!StringUtils.hasText(folderName)) {
            throw new RuntimeException("收藏夹名称不能为空");
        }
        if (folderName.length() > 50) {
            throw new RuntimeException("收藏夹名称不能超过50个字符");
        }

        int count = favoriteMapper.countFoldersByUser(userId);
        if (count >= MAX_FOLDERS_PER_USER) {
            throw new RuntimeException("每个用户最多创建 " + MAX_FOLDERS_PER_USER + " 个收藏夹");
        }

        FavoriteFolder folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setFolderName(folderName);
        folder.setDescription(description);
        folder.setIsPrivate(isPrivate != null ? isPrivate : false);
        folder.setSortOrder(sortOrder != null ? sortOrder : 0);
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());

        favoriteMapper.insertFolder(folder);
        favoriteCacheService.evictFoldersCache(userId); // 收藏夹列表变更
        log.info("用户 {} 创建了收藏夹: {}", userId, folder.getId());
        return folder;
    }

    @Override
    @Transactional
    public FavoriteFolder updateFolder(Integer folderId, String folderName, String description,
                                       Boolean isPrivate, Integer sortOrder) {
        Integer userId = getUserId();
        FavoriteFolder folder = favoriteMapper.selectFolderById(folderId);
        if (folder == null) {
            throw new RuntimeException("收藏夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改该收藏夹");
        }

        if (folderName != null) {
            if (!StringUtils.hasText(folderName)) {
                throw new RuntimeException("收藏夹名称不能为空");
            }
            if (folderName.length() > 50) {
                throw new RuntimeException("收藏夹名称不能超过50个字符");
            }
            folder.setFolderName(folderName);
        }
        if (description != null) {
            folder.setDescription(description);
        }
        if (isPrivate != null) {
            folder.setIsPrivate(isPrivate);
        }
        if (sortOrder != null) {
            folder.setSortOrder(sortOrder);
        }

        favoriteMapper.updateFolder(folder);
        log.info("用户 {} 更新了收藏夹: {}", userId, folderId);
        return favoriteMapper.selectFolderById(folderId);
    }

    @Override
    @Transactional
    public void deleteFolder(Integer folderId) {
        Integer userId = getUserId();
        FavoriteFolder folder = favoriteMapper.selectFolderById(folderId);
        if (folder == null) {
            throw new RuntimeException("收藏夹不存在");
        }
        if (!folder.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该收藏夹");
        }

        favoriteMapper.clearFolderByFolderId(folderId);
        favoriteMapper.deleteFolder(folderId, userId);
        log.info("用户 {} 删除了收藏夹: {}", userId, folderId);
    }

    @Override
    public List<FavoriteFolder> listFolders() {
        Integer userId = getUserId();
        // 查缓存
        List<FavoriteFolder> cached = favoriteCacheService.getCachedFolders(userId);
        if (cached != null) return cached;
        // 查数据库
        List<FavoriteFolder> folders = favoriteMapper.selectFoldersWithCount(userId);
        if (folders.isEmpty()) {
            favoriteCacheService.cacheEmptyFolders(userId);
        } else {
            favoriteCacheService.cacheFolders(userId, folders);
        }
        return folders;
    }

    @Override
    public FavoriteFolder getFolderById(Integer folderId) {
        return favoriteMapper.selectFolderById(folderId);
    }

    // ==================== Favorite Operations ====================

    @Override
    @Transactional
    public Favorite toggleFavorite(Integer mediaId, Integer folderId) {
        Integer userId = getUserId();

        Favorite existing = favoriteMapper.selectByUserAndMedia(userId, mediaId);
        if (existing != null) {
            favoriteMapper.delete(existing.getId(), userId);
            favoriteCacheService.evictAllFavoriteCache(userId); // 取消收藏，清除缓存
            log.info("用户 {} 取消收藏媒体: {}", userId, mediaId);
            return null;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setMediaId(mediaId);
        favorite.setFolderId(folderId != null ? folderId : 0);
        favorite.setCreateTime(LocalDateTime.now());

        favoriteMapper.insert(favorite);
        favoriteCacheService.evictAllFavoriteCache(userId); // 收藏变更，清除所有收藏缓存
        log.info("用户 {} 收藏了媒体: {}, 收藏夹: {}", userId, mediaId, folderId);
        return favorite;
    }

    @Override
    public Favorite checkFavorite(Integer mediaId) {
        Integer userId = getUserId();
        return favoriteMapper.selectByUserAndMedia(userId, mediaId);
    }

    @Override
    public PageBean<Favorite> listFavorites(Integer folderId, Integer pageNum, Integer pageSize) {
        Integer userId = getUserId();
        // 查缓存（仅首页）
        if (pageNum == 1) {
            Object cached = favoriteCacheService.getCachedFavorites(userId, folderId, pageNum, pageSize);
            if ("EMPTY".equals(cached)) return new PageBean<>();
            if (cached instanceof PageBean) return (PageBean<Favorite>) cached;
        }

        int offset = (pageNum - 1) * pageSize;

        List<Favorite> records;
        long total;

        if (folderId != null && folderId > 0) {
            records = favoriteMapper.selectByFolder(folderId, userId, offset, pageSize);
            total = favoriteMapper.countByFolder(folderId);
        } else {
            records = favoriteMapper.selectByUser(userId, offset, pageSize);
            total = favoriteMapper.countByUser(userId);
        }

        PageBean<Favorite> pageBean = new PageBean<>();
        pageBean.setItems(records);
        pageBean.setTotal(total);
        pageBean.setPageNum(pageNum);
        pageBean.setPageSize(pageSize);

        // 写缓存（首页，防穿透）
        if (pageNum == 1) {
            if (records.isEmpty()) {
                favoriteCacheService.cacheEmptyFavorites(userId, folderId, pageNum, pageSize);
            } else {
                favoriteCacheService.cacheFavorites(userId, folderId, pageNum, pageSize, pageBean);
            }
        }
        return pageBean;
    }

    @Override
    public List<Integer> getFavoriteMediaIds() {
        Integer userId = getUserId();
        List<Integer> cached = favoriteCacheService.getCachedFavoriteMediaIds(userId);
        if (cached != null) return cached;
        List<Integer> ids = favoriteMapper.selectUserFavoriteMediaIds(userId);
        favoriteCacheService.cacheFavoriteMediaIds(userId, ids);
        return ids;
    }

    @Override
    @Transactional
    public void moveToFolder(Integer favoriteId, Integer folderId) {
        Integer userId = getUserId();
        if (folderId != null && folderId > 0) {
            FavoriteFolder folder = favoriteMapper.selectFolderById(folderId);
            if (folder == null) {
                throw new RuntimeException("收藏夹不存在");
            }
            if (!folder.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作该收藏夹");
            }
        }

        int rows = favoriteMapper.moveToFolder(favoriteId, folderId != null ? folderId : 0);
        if (rows == 0) {
            throw new RuntimeException("收藏记录不存在或无权操作");
        }
        log.info("用户 {} 将收藏 {} 移动到收藏夹: {}", userId, favoriteId, folderId);
    }

    @Override
    @Transactional
    public void removeFromFolder(Integer favoriteId) {
        int rows = favoriteMapper.removeFromFolder(favoriteId);
        if (rows == 0) {
            throw new RuntimeException("收藏记录不存在或无权操作");
        }
        Integer userId = getUserId();
        log.info("用户 {} 将收藏 {} 移出收藏夹", userId, favoriteId);
    }

    @Override
    @Transactional
    public void deleteFavorite(Integer favoriteId) {
        Integer userId = getUserId();
        int rows = favoriteMapper.delete(favoriteId, userId);
        if (rows == 0) {
            throw new RuntimeException("收藏记录不存在或无权操作");
        }
        log.info("用户 {} 删除了收藏: {}", userId, favoriteId);
    }

    @Override
    @Transactional
    public void batchMoveToFolder(List<Integer> ids, Integer folderId) {
        Integer userId = getUserId();
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("请选择要移动的收藏记录");
        }

        if (folderId != null && folderId > 0) {
            FavoriteFolder folder = favoriteMapper.selectFolderById(folderId);
            if (folder == null) {
                throw new RuntimeException("收藏夹不存在");
            }
            if (!folder.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作该收藏夹");
            }
        }

        favoriteMapper.batchMoveToFolder(ids, folderId != null ? folderId : 0);
        log.info("用户 {} 批量移动 {} 条收藏到收藏夹: {}", userId, ids.size(), folderId);
    }

    // ==================== Private Helpers ====================

    private Integer getUserId() {
        Integer userId = ThreadLocalUtil.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }
}
