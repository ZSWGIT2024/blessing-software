package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.pojo.UserFeedback;
import com.itheima.service.FeedbackService;
import com.itheima.service.impl.OssUploadService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final OssUploadService ossUploadService;

    /** 上传反馈截图到OSS，返回URL */
    @PostMapping("/upload-image")
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.getSize() > 10 * 1024 * 1024) return Result.error("图片大小不能超过10MB");
            if (file.getContentType() != null && !file.getContentType().startsWith("image/"))
                return Result.error("只能上传图片文件");

            Integer userId = (Integer) ThreadLocalUtil.get().get("id");
            String fileUrl;
            try {
                // 直接流式上传到OSS
                String objectName = ossUploadService.generateObjectName(userId, file.getOriginalFilename(), "feedback");
                fileUrl = ossUploadService.directUpload(file, userId, "feedback");
            } catch (IOException e) {
                return Result.error("上传失败：" + e.getMessage());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("url", fileUrl);
            return Result.success(data);
        } catch (Exception e) {
            log.error("上传反馈截图失败", e);
            return Result.error("上传失败");
        }
    }

    /** 提交反馈 */
    @PostMapping
    public Result submit(@RequestBody UserFeedback feedback) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String username = (String) claims.getOrDefault("username", "");
        return feedbackService.submit(userId, username, feedback);
    }

    /** 我的反馈历史 */
    @GetMapping("/my")
    public Result getMyFeedbacks() {
        Integer userId = (Integer) ThreadLocalUtil.get().get("id");
        return feedbackService.getMyFeedbacks(userId);
    }

    /** 编辑我的反馈（仅待处理） */
    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody UserFeedback feedback) {
        Integer userId = (Integer) ThreadLocalUtil.get().get("id");
        return feedbackService.updateMyFeedback(userId, id, feedback);
    }

    /** 删除我的反馈 */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        Integer userId = (Integer) ThreadLocalUtil.get().get("id");
        return feedbackService.deleteMyFeedback(userId, id);
    }

    // ===== Admin =====

    /** 管理员分页查询 */
    @GetMapping("/admin/list")
    public Result getFeedbackList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return feedbackService.getFeedbackList(page, size, status);
    }

    /** 管理员处理反馈 */
    @PutMapping("/admin/{id}/handle")
    public Result handle(@PathVariable Long id,
                         @RequestBody Map<String, Object> params) {
        String status = (String) params.get("status");
        String adminReply = (String) params.get("adminReply");
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer adminId = (Integer) claims.get("id");
        return feedbackService.handleFeedback(id, status, adminReply, adminId);
    }

    /** 管理员删除 */
    @DeleteMapping("/admin/{id}")
    public Result adminDelete(@PathVariable Long id) {
        return feedbackService.deleteFeedback(id);
    }

    /** 反馈统计 */
    @GetMapping("/admin/stats")
    public Result getStatistics() {
        return feedbackService.getStatistics();
    }
}
