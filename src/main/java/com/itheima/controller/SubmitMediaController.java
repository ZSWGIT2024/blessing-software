package com.itheima.controller;


import com.itheima.dto.MediaQueryDTO;
import com.itheima.dto.MediaUpdateDTO;
import com.itheima.dto.MediaUploadDTO;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.pojo.UserMedia;
import com.itheima.service.SubmitMediaService;
import com.itheima.utils.IpUtil;
import com.itheima.vo.MediaVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/submit")
@Validated
@Slf4j
@RequiredArgsConstructor
public class SubmitMediaController {


    private final SubmitMediaService submitMediaService;

    /**
     * 批量上传媒体文件
     */
    @PostMapping("/batch-upload")
    public Result<List<MediaVO>> batchUpload(@Valid @ModelAttribute MediaUploadDTO dto,
                                             HttpServletRequest request) {
        try {
            String clientIp = IpUtil.getClientIp(request);
            dto.setUploadIp(clientIp);

            List<MediaVO> vos = submitMediaService.batchUpload(dto);
            return Result.success(vos);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }


    /**
     * 查询投稿媒体列表
     */
    @GetMapping("/submitList")
    public Result<PageBean<MediaVO>> getSubmitList(@Valid MediaQueryDTO queryDTO) {
        try {
            PageBean<MediaVO> result = submitMediaService.querySubmitList(queryDTO);
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
    @GetMapping("/user/{userId}/submitList")
    public Result<PageBean<MediaVO>> getSubmitByUserAndStatus(
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

            PageBean<MediaVO> result = submitMediaService.querySubmitByUserAndStatus(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    //    /**
//     * 获取用户所有状态的作品（不筛选状态）
//     */
    @GetMapping("/user/{userId}/all-status")
    public Result<PageBean<MediaVO>> getAllStatusSubmitByUser(
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

            PageBean<MediaVO> result = submitMediaService.queryAllStatusSubmitByUser(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}/recent")
    public Result<List<UserMedia>> getRecentMedia(
            @PathVariable("userId") Integer userId,
            @RequestParam(required = false, defaultValue = "20", value = "limit") Integer limit) {
        // 添加最大限制保护
        limit = Math.min(limit, 100); // 防止过大查询
        return Result.success(submitMediaService.getRecentMedia(userId, limit));
    }

    /**
     * 删除媒体
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMedia(@PathVariable("id") Integer id,
                                    @RequestParam("userId") Integer userId) {
        try {
            boolean success = submitMediaService.deleteMedia(id, userId);
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
            MediaVO vo = submitMediaService.updateMedia(id, updateDTO);
            return Result.success(vo);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    //更新媒体状态
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Integer id, @RequestParam("status") String status) {
        try {
            boolean success = submitMediaService.updateStatus(id, status);
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
            boolean success = submitMediaService.batchUpdateStatus(ids, status, tags);
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
            boolean success = submitMediaService.toggleLike(id, userId);
            return success ? Result.success() : Result.error("操作失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }


}
