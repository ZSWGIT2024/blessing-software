package com.itheima.service.impl;


import com.itheima.dto.AvatarFrameWithStatusDTO;
import com.itheima.mapper.AvatarFrameMapper;
import com.itheima.mapper.UserAvatarFrameMapper;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.AvatarFrame;
import com.itheima.pojo.User;
import com.itheima.pojo.UserAvatarFrame;
import com.itheima.service.AvatarFrameService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class AvatarFrameServiceImpl implements AvatarFrameService {
    private final AvatarFrameMapper frameMapper;
    private final UserAvatarFrameMapper userFrameMapper;
    private final UserMapper userMapper;

    @Override
    public AvatarFrame getCurrentFrame(Integer userId) {
        // 1. 查询用户当前使用的头像框ID
        Integer frameId = userMapper.selectCurrentFrameId(userId);

        // 2. 查询头像框详情
        return frameId != null ? frameMapper.selectById(frameId) : null;
    }


    @Override
    public List<AvatarFrame> getAvailableFrames(Integer userId) {
        User user = userMapper.findUserById(userId);
        List<AvatarFrame> allFrames = frameMapper.selectAvailableFramesByUser(user.getLevel(), user.getVipType(), user.getTotalLoginDays());

        return allFrames;
    }

    @Transactional
    @Override
    public boolean changeUserFrame(Integer userId, Integer frameId) {
        // 检查用户是否拥有该头像框
        if (userFrameMapper.exists(userId, frameId) == 0) {
            throw new RuntimeException("您尚未解锁该头像框");
        }

        // 原子性更新状态
        int updated = userFrameMapper.updateUsingStatus(userId, frameId);
        return updated > 0;
    }

    @Override
    @Transactional
    public void unlockFrameForUser(Integer userId, Integer frameId) {
        if (userFrameMapper.exists(userId, frameId) > 0) {
            return;
        }

        UserAvatarFrame uaf = new UserAvatarFrame();
        uaf.setUserId(userId);
        uaf.setFrameId(frameId);
        uaf.setUnlockTime(new Date());
        uaf.setIsUsing(false);
        userFrameMapper.insert(uaf);
    }

    //获取所有头像框列表带解锁状态
    // 修改返回类型为DTO
    @Override
    public List<AvatarFrameWithStatusDTO> getAllFramesWithStatus(Integer userId) {
        // 1. 获取所有头像框
        List<AvatarFrame> allFrames = frameMapper.getAllFrames();

        // 2. 获取用户已解锁的头像框ID列表
        List<Integer> unlockedFrameIds = userFrameMapper.selectUnlockedFrameIdsByUserId(userId);

        // 3. 获取用户当前使用的头像框ID
        Integer usingFrameId = userFrameMapper.selectUsingFrameIdByUserId(userId);

        // 4. 构建DTO列表
        return allFrames.stream().map(frame -> {
            AvatarFrameWithStatusDTO dto = new AvatarFrameWithStatusDTO();
            // 复制基本属性
            BeanUtils.copyProperties(frame, dto);

            // 设置状态
            dto.setUnlocked(unlockedFrameIds.contains(frame.getId()));
            dto.setIsUsing(frame.getId().equals(usingFrameId));

            return dto;
        }).collect(Collectors.toList());
    }
}

