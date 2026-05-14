package com.itheima.service;

import com.itheima.dto.WorldMessageDTO;
import com.itheima.pojo.Result;

import java.util.List;
import java.util.Map;

public interface WorldChatService {

    Result<Map<String, Object>> sendWorldMessage(Integer senderId, String content, String contentType,
                                                  String messageId, Map<String, Object> extraData);

    Result<List<WorldMessageDTO>> getWorldMessages(String lastMessageId, int pageSize);

    Result<?> withdrawWorldMessage(Integer userId, String messageId, String reason);

    Result<?> deleteWorldMessage(Integer userId, String messageId);
}
