package com.itheima.service;

import com.itheima.pojo.Favorite;
import com.itheima.pojo.FavoriteFolder;
import com.itheima.pojo.PageBean;

import java.util.List;

public interface FavoriteService {

    // ==================== Folder Operations ====================

    FavoriteFolder createFolder(String folderName, String description, Boolean isPrivate, Integer sortOrder);

    FavoriteFolder updateFolder(Integer folderId, String folderName, String description, Boolean isPrivate, Integer sortOrder);

    void deleteFolder(Integer folderId);

    List<FavoriteFolder> listFolders();

    FavoriteFolder getFolderById(Integer folderId);

    // ==================== Favorite Operations ====================

    Favorite toggleFavorite(Integer mediaId, Integer folderId);

    Favorite checkFavorite(Integer mediaId);

    PageBean<Favorite> listFavorites(Integer folderId, Integer pageNum, Integer pageSize);

    List<Integer> getFavoriteMediaIds();

    void moveToFolder(Integer favoriteId, Integer folderId);

    void removeFromFolder(Integer favoriteId);

    void deleteFavorite(Integer favoriteId);

    void batchMoveToFolder(List<Integer> ids, Integer folderId);
}
