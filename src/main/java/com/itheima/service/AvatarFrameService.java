package com.itheima.service;

import com.itheima.dto.AvatarFrameWithStatusDTO;
import com.itheima.pojo.AvatarFrame;

import java.util.List;

public interface AvatarFrameService {
    AvatarFrame getCurrentFrame(Integer userId);
    List<AvatarFrame> getAvailableFrames(Integer userId);
    boolean changeUserFrame(Integer userId, Integer frameId);
    void unlockFrameForUser(Integer userId, Integer frameId);

    // 修改返回类型为DTO
    List<AvatarFrameWithStatusDTO> getAllFramesWithStatus(Integer userId);
}