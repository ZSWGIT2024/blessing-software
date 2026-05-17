package com.itheima.controller;

import com.itheima.dto.MediaQueryDTO;
import com.itheima.dto.MediaUpdateDTO;
import com.itheima.dto.MediaUploadDTO;
import com.itheima.dto.SubmitBatchRequest;
import com.itheima.pojo.UserMedia;
import com.itheima.utils.IpUtil;
import com.itheima.utils.ThreadLocalUtil;
import com.itheima.vo.MediaVO;
import com.itheima.pojo.PageBean;
import com.itheima.service.MediaService;
import com.itheima.pojo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * 批量提交AI作品（两阶段上传的第二阶段：元数据写入user_media表）
     * POST /api/media/batch
     */
    @PostMapping("/batch")
    public Result<List<MediaVO>> batchSubmit(@RequestBody SubmitBatchRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            String clientIp = IpUtil.getClientIp(httpRequest);
            request.setUploadIp(clientIp);
            Integer userId = (Integer) ThreadLocalUtil.get().get("id");
            List<MediaVO> vos = mediaService.batchSubmit(userId, request);
            return Result.success(vos);
        } catch (Exception e) {
            log.error("批量提交AI作品失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量上传媒体文件
     */
    @PostMapping("/batch-upload")
    public Result<List<MediaVO>> batchUpload(@Valid @ModelAttribute MediaUploadDTO dto,
                                             HttpServletRequest request) {
        try {
            String clientIp = IpUtil.getClientIp(request);
            dto.setUploadIp(clientIp);

            List<MediaVO> vos = mediaService.batchUpload(dto);
            return Result.success(vos);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询媒体列表
     */
    @GetMapping("/list")
    public Result<PageBean<MediaVO>> getMediaList(@Valid MediaQueryDTO queryDTO) {
        try {
            PageBean<MediaVO> result = mediaService.queryMediaList(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询指定状态和媒体类型的媒体列表
     * @param userId 用户ID（必需）
     * @param status 状态：active/pending,hidden（必需）
     * @param mediaType 媒体类型：image/video（可选）
     * @param pageNum 页码
     * @param pageSize 每页大小
     */
    @GetMapping("/user/{userId}/mediaList")
    public Result<PageBean<MediaVO>> getMediaByUserAndStatus(
            @PathVariable("userId") @NotNull(message = "用户ID不能为空") Integer userId,
            @RequestParam("status") @NotBlank(message = "状态不能为空") String status,
            @RequestParam(required = false, value = "mediaType") String mediaType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {

        try {
            // 验证状态参数
            if (!"active".equals(status) && !"pending".equals(status) && !"hidden".equals(status)) {
                return Result.error("状态参数必须是 active,pending或hidden");
            }

            MediaQueryDTO queryDTO = new MediaQueryDTO();
            queryDTO.setUserId(userId);
            queryDTO.setStatus(status);
            queryDTO.setMediaType(mediaType);
            queryDTO.setPageNum(pageNum);
            queryDTO.setPageSize(pageSize);

            PageBean<MediaVO> result = mediaService.queryMediaByUserAndStatus(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    //    /**
//     * 获取用户所有状态的作品（不筛选状态）
//     */
    @GetMapping("/user/{userId}/all-status")
    public Result<PageBean<MediaVO>> getAllStatusMediaByUser(
            @PathVariable("userId") @NotNull(message = "用户ID不能为空") Integer userId,
            @RequestParam(required = false, value = "mediaType") String mediaType,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {

        try {
            MediaQueryDTO queryDTO = new MediaQueryDTO();
            queryDTO.setUserId(userId);
            queryDTO.setMediaType(mediaType);
            queryDTO.setStatus(null); // 状态为null表示不筛选
            queryDTO.setPageNum(pageNum);
            queryDTO.setPageSize(pageSize);

            PageBean<MediaVO> result = mediaService.queryAllStatusMediaByUser(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取媒体详情
     */
    @GetMapping("/detail/{id}")
    public Result<MediaVO> getDetail(@PathVariable("id") Integer id,
                                     @RequestParam("userId") Integer userId) {
        try {
            MediaVO vo = mediaService.getMediaDetail(id, userId);
            return Result.success(vo);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/recent")
    public Result<List<UserMedia>> getRecentMedia(
            @PathVariable("userId") Integer userId,
            @RequestParam(required = false, defaultValue = "20", value = "limit") Integer limit) {
        // 添加最大限制保护
        limit = Math.min(limit, 100); // 防止过大查询
        return Result.success(mediaService.getRecentMedia(userId, limit));
    }

    /**
     * 删除媒体
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMedia(@PathVariable("id") Integer id,
                                    @RequestParam("userId") Integer userId) {
        try {
            boolean success = mediaService.deleteMedia(id, userId);
            return success ? Result.success() : Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新媒体信息
     */
    @PutMapping("/{id}")
    public Result<MediaVO> updateMedia(@PathVariable("id") Integer id,
                                       @RequestBody MediaUpdateDTO updateDTO) {
        try {
            MediaVO vo = mediaService.updateMedia(id, updateDTO);
            return Result.success(vo);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    //更新媒体状态
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Integer id, @RequestParam("status") String status) {
        try {
            boolean success = mediaService.updateStatus(id, status);
            return success ? Result.success() : Result.error("修改状态失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 批量更新媒体状态
    @PutMapping("/batch-status")
    public Result<Void> batchUpdateStatus(
            @RequestParam("status") String status,
            @RequestParam(name = "tags", required = false) String tags,
            @RequestBody List<Integer> ids) {

        log.info("=== 批量更新状态接口被调用 ===");
        log.info("请求参数 - IDs: {}, Status: {}, Tags: {}", ids, status, tags);

        try {
            boolean success = mediaService.batchUpdateStatus(ids, status, tags);
            log.info("Service返回结果: {}", success);

            if (success) {
                log.info("操作成功，返回Result.success()");
                return Result.success();
            } else {
                log.warn("操作失败，返回Result.error()");
                // 这里的问题：返回的msg是null！
                return Result.error("批量修改状态失败");
            }
        } catch (RuntimeException e) {
            log.error("批量更新状态异常", e);
            return Result.error(e.getMessage());
        } finally {
            log.info("=== 批量更新状态接口结束 ===");
        }
    }

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/like/{id}")
    public Result<Void> toggleLike(@PathVariable("id") Integer id,
                                   @RequestParam("userId") Integer userId) {
        try {
            boolean success = mediaService.toggleLike(id, userId);
            return success ? Result.success() : Result.error("操作失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }


    /**
     * 获取用户媒体统计
     */
    @GetMapping("/stats")
    public Result<Object> getStats(@RequestParam("userId") Integer userId) {
        try {
            // 查询统计数据
            Long total = mediaService.getTotalCount(userId);
            Long images = mediaService.getImageCount(userId);
            Long videos = mediaService.getVideoCount(userId);

            return Result.success(new Object() {
                public final Long totalCount = total;
                public final Long imageCount = images;
                public final Long videoCount = videos;
                public final Long otherCount = total - images - videos;
            });
        } catch (Exception e) {
            return Result.error("获取统计失败: " + e.getMessage());
        }
    }
}