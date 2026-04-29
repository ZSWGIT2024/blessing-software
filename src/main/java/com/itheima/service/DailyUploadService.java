package com.itheima.service;

public interface DailyUploadService {

    // 获取用户今日上传数量
    int getTodayUploadCount(Integer userId);

    // 修复：检查上传限制的方法（现在会根据VIP类型判断）
   void checkUploadLimit(Integer userId, int fileCount);

    // 辅助方法：获取VIP类型名称
    String getVipTypeName(Integer vipType);

    // 修复：更新上传计数的方法
    void updateUploadCount(Integer userId, int increment);
}
