package com.itheima.service;

import com.itheima.pojo.Result;
import com.itheima.pojo.UserFeedback;

import java.util.Map;

public interface FeedbackService {

    /** 用户提交反馈 */
    Result submit(Integer userId, String username, UserFeedback feedback);

    /** 用户查看自己的反馈历史 */
    Result getMyFeedbacks(Integer userId);

    /** 用户编辑待处理的反馈 */
    Result updateMyFeedback(Integer userId, Long feedbackId, UserFeedback feedback);

    /** 用户删除待处理的反馈 */
    Result deleteMyFeedback(Integer userId, Long feedbackId);

    // ===== Admin =====

    /** 管理员分页查询反馈 */
    Result getFeedbackList(int page, int size, String status);

    /** 管理员处理反馈 */
    Result handleFeedback(Long id, String status, String adminReply, Integer adminId);

    /** 管理员删除反馈 */
    Result deleteFeedback(Long id);

    /** 反馈统计 */
    Result getStatistics();
}
