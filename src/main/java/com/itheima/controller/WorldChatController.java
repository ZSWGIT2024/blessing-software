package com.itheima.controller;

import com.itheima.dto.ChatFileDTO;
import com.itheima.dto.WorldMessageDTO;
import com.itheima.pojo.GroupChatFile;
import com.itheima.pojo.Result;
import com.itheima.service.WorldChatService;
import com.itheima.service.impl.FileUploadService;
import com.itheima.utils.ThreadLocalUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/world")
@RequiredArgsConstructor
public class WorldChatController {

    private final WorldChatService worldChatService;
    private final FileUploadService fileUploadService;

    @PostMapping("/message/send")
    public Result<Map<String, Object>> sendWorldMessage(@RequestBody Map<String, Object> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String content = (String) params.get("content");
        String contentType = (String) params.getOrDefault("contentType", "text");
        String messageId = (String) params.get("messageId");
        @SuppressWarnings("unchecked")
        Map<String, Object> extraData = (Map<String, Object>) params.get("extraData");
        return worldChatService.sendWorldMessage(userId, content, contentType, messageId, extraData);
    }

    @GetMapping("/messages")
    public Result<List<WorldMessageDTO>> getWorldMessages(
            @RequestParam(required = false, value = "lastMessageId") String lastMessageId,
            @RequestParam(defaultValue = "50", value = "pageSize") int pageSize) {
        return worldChatService.getWorldMessages(lastMessageId, pageSize);
    }

    @PostMapping("/message/withdraw")
    public Result<?> withdrawMessage(@RequestBody Map<String, String> params) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        String messageId = params.get("messageId");
        String reason = params.get("reason");
        return worldChatService.withdrawWorldMessage(userId, messageId, reason);
    }

    @DeleteMapping("/message/{messageId}")
    public Result<?> deleteMessage(@PathVariable("messageId") String messageId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return worldChatService.deleteWorldMessage(userId, messageId);
    }

    @PostMapping("/file/upload")
    public Result<ChatFileDTO> uploadGroupFile(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "groupId", required = false) String groupId,
                                          @RequestParam(value = "chatType", defaultValue = "world") String chatType,
                                          @RequestParam("messageId") String messageId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return fileUploadService.uploadGroupFile(file, userId, groupId, chatType, messageId);
    }

    @GetMapping("/files")
    public Result<List<GroupChatFile>> getFiles(
            @RequestParam(value = "groupId", required = false) String groupId,
            @RequestParam(defaultValue = "1", value = "pageNum") int pageNum,
            @RequestParam(defaultValue = "20", value = "pageSize") int pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        if (groupId != null && !groupId.isEmpty()) {
            return fileUploadService.getGroupFiles(groupId, pageNum, pageSize);
        }
        return fileUploadService.getWorldFiles(userId, pageNum, pageSize);
    }

    @DeleteMapping("/file/{fileId}")
    public Result<?> deleteFile(@PathVariable("fileId") Long fileId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        return fileUploadService.deleteGroupFile(fileId, userId);
    }
}
