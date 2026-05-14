package com.itheima.controller;

import com.itheima.pojo.Favorite;
import com.itheima.pojo.FavoriteFolder;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // ==================== Folder Endpoints ====================

    @PostMapping("/folder")
    public Result<FavoriteFolder> createFolder(@RequestBody Map<String, Object> body) {
        try {
            String folderName = (String) body.get("folderName");
            if (folderName == null) {
                folderName = (String) body.get("name"); // 兼容前端旧参数名
            }
            String description = (String) body.get("description");
            Boolean isPrivate = body.get("isPrivate") instanceof Boolean b ? b : null;
            Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;

            FavoriteFolder folder = favoriteService.createFolder(folderName, description, isPrivate, sortOrder);
            return Result.success(folder);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/folder/{id}")
    public Result<FavoriteFolder> updateFolder(@PathVariable("id") Integer id,
                                               @RequestBody Map<String, Object> body) {
        try {
            String folderName = (String) body.get("folderName");
            if (folderName == null) {
                folderName = (String) body.get("name");
            }
            String description = (String) body.get("description");
            Boolean isPrivate = body.get("isPrivate") instanceof Boolean b ? b : null;
            Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;

            FavoriteFolder folder = favoriteService.updateFolder(id, folderName, description, isPrivate, sortOrder);
            return Result.success(folder);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/folder/{id}")
    public Result<Void> deleteFolder(@PathVariable("id") Integer id) {
        try {
            favoriteService.deleteFolder(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/folders")
    public Result<List<FavoriteFolder>> listFolders() {
        try {
            List<FavoriteFolder> folders = favoriteService.listFolders();
            return Result.success(folders);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== Favorite Endpoints ====================

    @PostMapping("/toggle")
    public Result<Favorite> toggleFavorite(@RequestBody Map<String, Object> body) {
        try {
            Integer mediaId = body.get("mediaId") != null ? ((Number) body.get("mediaId")).intValue() : null;
            Integer folderId = body.get("folderId") != null ? ((Number) body.get("folderId")).intValue() : null;

            if (mediaId == null) {
                return Result.error("mediaId不能为空");
            }

            Favorite favorite = favoriteService.toggleFavorite(mediaId, folderId);
            return Result.success(favorite);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/check")
    public Result<java.util.Map<String, Object>> checkFavorite(@RequestParam("mediaId") Integer mediaId) {
        try {
            Favorite favorite = favoriteService.checkFavorite(mediaId);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            if (favorite != null) {
                result.put("favorited", true);
                result.put("favoriteId", favorite.getId());
                result.put("folderId", favorite.getFolderId());
            } else {
                result.put("favorited", false);
                result.put("favoriteId", null);
                result.put("folderId", null);
            }
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<PageBean<Favorite>> listFavorites(
            @RequestParam(value = "folderId", required = false) Integer folderId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        try {
            PageBean<Favorite> result = favoriteService.listFavorites(folderId, pageNum, pageSize);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/media-ids")
    public Result<List<Integer>> getFavoriteMediaIds() {
        try {
            List<Integer> mediaIds = favoriteService.getFavoriteMediaIds();
            return Result.success(mediaIds);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/move")
    public Result<Void> moveToFolder(@PathVariable("id") Integer id,
                                     @RequestBody Map<String, Object> body) {
        try {
            Integer folderId = body.get("folderId") != null ? ((Number) body.get("folderId")).intValue() : null;
            favoriteService.moveToFolder(id, folderId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/remove-folder")
    public Result<Void> removeFromFolder(@PathVariable("id") Integer id) {
        try {
            favoriteService.removeFromFolder(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFavorite(@PathVariable("id") Integer id) {
        try {
            favoriteService.deleteFavorite(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/batch-move")
    public Result<Void> batchMoveToFolder(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            Integer folderId = body.get("folderId") != null ? ((Number) body.get("folderId")).intValue() : null;

            if (ids == null || ids.isEmpty()) {
                return Result.error("ids不能为空");
            }

            favoriteService.batchMoveToFolder(ids, folderId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
