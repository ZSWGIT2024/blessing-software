package com.itheima.service.impl;

import com.itheima.mapper.UserFeedbackMapper;
import com.itheima.pojo.Result;
import com.itheima.pojo.UserFeedback;
import com.itheima.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final UserFeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public Result submit(Integer userId, String username, UserFeedback feedback) {
        if (feedback.getContent() == null || feedback.getContent().trim().isEmpty()) {
            return Result.error("反馈内容不能为空");
        }
        if (feedback.getContent().length() > 500) {
            return Result.error("反馈内容不能超过500字");
        }
        if (feedback.getType() == null || feedback.getType().isEmpty()) {
            feedback.setType("other");
        }
        feedback.setUserId(userId);
        feedback.setUsername(username);
        feedback.setStatus("pending");
        feedbackMapper.insert(feedback);
        log.info("用户 {} 提交了反馈: id={}", userId, feedback.getId());
        return Result.success("反馈已提交");
    }

    @Override
    public Result getMyFeedbacks(Integer userId) {
        List<UserFeedback> list = feedbackMapper.selectByUserId(userId);
        return Result.success(list);
    }

    @Override
    @Transactional
    public Result updateMyFeedback(Integer userId, Long feedbackId, UserFeedback feedback) {
        UserFeedback existing = feedbackMapper.selectById(feedbackId);
        if (existing == null) return Result.error("反馈记录不存在");
        if (!existing.getUserId().equals(userId)) return Result.error("无权操作");
        if (!"pending".equals(existing.getStatus())) return Result.error("仅待处理状态的反馈可编辑");
        if (feedback.getContent() != null && feedback.getContent().length() > 500) {
            return Result.error("反馈内容不能超过500字");
        }
        feedback.setId(feedbackId);
        feedback.setUserId(userId);
        feedbackMapper.updateByUser(feedback);
        log.info("用户 {} 更新了反馈: id={}", userId, feedbackId);
        return Result.success("更新成功");
    }

    @Override
    @Transactional
    public Result deleteMyFeedback(Integer userId, Long feedbackId) {
        UserFeedback existing = feedbackMapper.selectById(feedbackId);
        if (existing == null) return Result.error("反馈记录不存在");
        if (!existing.getUserId().equals(userId)) return Result.error("无权操作");
        feedbackMapper.softDeleteByUser(feedbackId, userId);
        log.info("用户 {} 删除了反馈: id={}", userId, feedbackId);
        return Result.success("删除成功");
    }

    // ===== Admin =====

    @Override
    public Result getFeedbackList(int page, int size, String status) {
        int offset = (page - 1) * size;
        List<UserFeedback> list = feedbackMapper.selectPage(offset, size, status);
        Long total = feedbackMapper.count(status);
        Map<String, Object> data = new HashMap<>();
        data.put("items", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        data.put("totalPages", (int) Math.ceil((double) total / size));
        return Result.success(data);
    }

    @Override
    @Transactional
    @CacheEvict(value = "feedbackStats", allEntries = true)
    public Result handleFeedback(Long id, String status, String adminReply, Integer adminId) {
        UserFeedback existing = feedbackMapper.selectById(id);
        if (existing == null) return Result.error("反馈记录不存在");
        feedbackMapper.updateByAdmin(id, status, adminReply, adminId);
        log.info("管理员 {} 处理反馈: id={}, status={}", adminId, id, status);
        return Result.success("处理成功");
    }

    @Override
    @Transactional
    @CacheEvict(value = "feedbackStats", allEntries = true)
    public Result deleteFeedback(Long id) {
        feedbackMapper.softDelete(id);
        log.info("管理员删除反馈: id={}", id);
        return Result.success("删除成功");
    }

    @Override
    @Cacheable(value = "feedbackStats", unless = "#result == null")
    public Result getStatistics() {
        List<Map<String, Object>> stats = feedbackMapper.countByStatus();
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> row : stats) {
            result.put((String) row.get("status"), ((Number) row.get("cnt")).longValue());
        }
        return Result.success(result);
    }
}
