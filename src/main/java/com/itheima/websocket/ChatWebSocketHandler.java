// 新建文件：com/itheima/websocket/ChatWebSocketHandler.java
package com.itheima.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.dto.ChatMessageDTO;
import com.itheima.dto.FriendRelationDTO;
import com.itheima.dto.UserSimpleDTO;
import com.itheima.mapper.UserMapper;
import com.itheima.pojo.Result;
import com.itheima.service.SocialService;
import com.itheima.service.impl.SocialCacheService;
import com.itheima.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 存储所有在线用户的会话 <userId, WebSocketSession>
    private static final Map<Integer, WebSocketSession> onlineSessions = new ConcurrentHashMap<>();

    // 存储用户与session的映射（一个用户可能有多个session，比如多设备登录）
    private static final Map<Integer, Map<String, WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final SocialService socialService;
    private final SocialCacheService cacheService;
    private final UserMapper userMapper;

    public ChatWebSocketHandler(ObjectMapper objectMapper,
                                SocialService socialService,
                                SocialCacheService cacheService,
                                UserMapper userMapper) {
        this.objectMapper = objectMapper;
        this.socialService = socialService;
        this.cacheService = cacheService;
        this.userMapper = userMapper;
    }

    /**
     * 连接建立后的回调
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");

        // 存储session
        onlineSessions.put(userId, session);

        // 处理多设备登录
        userSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);

        log.info("用户 {} 已连接WebSocket，当前在线人数: {}", userId, onlineSessions.size());

        // 更新用户在线状态
        userMapper.updateUserOnlineStatus(userId, "online");

        // 广播用户上线消息给所有好友
        broadcastUserStatus(userId, "online");

        // 发送连接成功消息给客户端
        Map<String, Object> welcomeMsg = new HashMap<>();
        welcomeMsg.put("type", "connected");
        welcomeMsg.put("message", "连接成功");
        welcomeMsg.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcomeMsg)));
    }

    /**
     * 处理接收到的文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Integer senderId = (Integer) session.getAttributes().get("userId");

        try {
            // 解析消息
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            log.info("收到WebSocket消息 - 发送者: {}, 类型: {}", senderId, type);

            switch (type) {
                case "message":
                    // 处理聊天消息
                    handleChatMessage(senderId, payload);
                    break;

                case "typing":
                    // 处理输入状态
                    handleTypingStatus(senderId, payload);
                    break;

                case "read":
                    // 处理已读回执
                    handleReadReceipt(senderId, payload);
                    break;

                case "ping":
                    // 处理心跳
                    handlePing(session);
                    break;

                default:
                    log.warn("未知的消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);

            // 发送错误消息给客户端
            Map<String, Object> errorMsg = new HashMap<>();
            errorMsg.put("type", "error");
            errorMsg.put("message", "消息处理失败: " + e.getMessage());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(Integer senderId, Map<String, Object> payload) throws Exception {
        Integer receiverId = (Integer) payload.get("receiverId");
        String content = (String) payload.get("content");
        String contentType = (String) payload.get("contentType");
        String chatType = (String) payload.get("chatType");
        String messageId = (String) payload.get("messageId");
        Long timestamp = TimeUtils.toTimestamp(payload.get("timestamp"));

        // 构建要发送的消息
        Map<String, Object> messageToSend = new HashMap<>();
        messageToSend.put("type", "message");
        messageToSend.put("senderId", senderId);
        messageToSend.put("receiverId", receiverId);
        messageToSend.put("content", content);
        messageToSend.put("contentType", contentType);
        messageToSend.put("chatType", chatType);
        messageToSend.put("messageId", messageId);
        messageToSend.put("timestamp", timestamp != null ? timestamp : System.currentTimeMillis());

        // 获取发送者信息
        Map<String, Object> senderInfo = getUserSimpleInfo(senderId);
        messageToSend.put("senderName", senderInfo.get("nickname"));
        messageToSend.put("senderAvatar", senderInfo.get("avatar"));

        // 尝试发送给接收者（如果在线）
        WebSocketSession receiverSession = onlineSessions.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageToSend)));
            log.info("实时消息已发送: {} -> {}", senderId, receiverId);

            // 可选：发送已送达回执给发送者
            sendDeliveryReceipt(senderId, messageId, "delivered");
        } else {
            log.info("接收者 {} 不在线，消息将作为离线消息", receiverId);
            // 缓存离线消息（如果需要）
            cacheOfflineMessage(receiverId, messageToSend);

            // 通知发送者对方不在线
            Map<String, Object> offlineNotice = new HashMap<>();
            offlineNotice.put("type", "offline");
            offlineNotice.put("receiverId", receiverId);
            offlineNotice.put("messageId", messageId);
            offlineNotice.put("timestamp", System.currentTimeMillis());

            WebSocketSession senderSession = onlineSessions.get(senderId);
            if (senderSession != null && senderSession.isOpen()) {
                senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(offlineNotice)));
            }
        }

        // 更新未读消息计数缓存
        cacheService.incrementUnreadCount(receiverId, senderId);
    }

    /**
     * 处理输入状态
     */
    private void handleTypingStatus(Integer senderId, Map<String, Object> payload) throws Exception {
        Integer receiverId = (Integer) payload.get("receiverId");
        Boolean isTyping = (Boolean) ((Map<String, Object>) payload.get("content")).get("isTyping");

        Map<String, Object> typingMsg = new HashMap<>();
        typingMsg.put("type", "typing");
        typingMsg.put("senderId", senderId);
        typingMsg.put("isTyping", isTyping);
        typingMsg.put("timestamp", System.currentTimeMillis());

        // 转发输入状态给接收者
        WebSocketSession receiverSession = onlineSessions.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(typingMsg)));
        }
    }

    /**
     * 处理已读回执
     */
    private void handleReadReceipt(Integer readerId, Map<String, Object> payload) throws Exception {
        Integer senderId = (Integer) payload.get("receiverId"); // 消息的发送者
        String messageId = (String) ((Map<String, Object>) payload.get("content")).get("messageId");

        Map<String, Object> readReceipt = new HashMap<>();
        readReceipt.put("type", "read");
        readReceipt.put("readerId", readerId);
        readReceipt.put("messageId", messageId);
        readReceipt.put("timestamp", System.currentTimeMillis());

        // 转发已读回执给发送者
        WebSocketSession senderSession = onlineSessions.get(senderId);
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(readReceipt)));
        }

        // 重置未读计数
        cacheService.resetUnreadCount(senderId, readerId);
    }

    /**
     * 处理心跳
     */
    private void handlePing(WebSocketSession session) throws Exception {
        Map<String, Object> pong = new HashMap<>();
        pong.put("type", "pong");
        pong.put("timestamp", System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
    }

    /**
     * 发送送达回执
     */
    private void sendDeliveryReceipt(Integer senderId, String messageId, String status) throws Exception {
        Map<String, Object> receipt = new HashMap<>();
        receipt.put("type", "receipt");
        receipt.put("messageId", messageId);
        receipt.put("status", status);
        receipt.put("timestamp", System.currentTimeMillis());

        WebSocketSession senderSession = onlineSessions.get(senderId);
        if (senderSession != null && senderSession.isOpen()) {
            senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(receipt)));
        }
    }

    /**
     * 广播用户状态变化
     */
    private void broadcastUserStatus(Integer userId, String status) throws Exception {
        // 获取用户的所有好友
        Result<List<FriendRelationDTO>> friendsResult = socialService.getFriendList(userId);
        if (friendsResult.getCode() == 0 && friendsResult.getData() != null) {
            Map<String, Object> statusMsg = new HashMap<>();
            statusMsg.put("type", "user_status");
            statusMsg.put("userId", userId);
            statusMsg.put("status", status);
            statusMsg.put("timestamp", System.currentTimeMillis());

            String statusJson = objectMapper.writeValueAsString(statusMsg);

            // 给每个在线的好友发送状态更新
            for (FriendRelationDTO friend : friendsResult.getData()) {
                WebSocketSession friendSession = onlineSessions.get(friend.getFriendId());
                if (friendSession != null && friendSession.isOpen()) {
                    friendSession.sendMessage(new TextMessage(statusJson));
                }
            }
        }
    }

    /**
     * 获取用户简要信息
     */
    private Map<String, Object> getUserSimpleInfo(Integer userId) {
        Map<String, Object> info = new HashMap<>();
        try {
            Result<UserSimpleDTO> userResult = socialService.getUserDetail(userId, userId);
            if (userResult.getCode() == 0 && userResult.getData() != null) {
                UserSimpleDTO user = userResult.getData();
                info.put("nickname", user.getNickname());
                info.put("avatar", user.getAvatar());
            }
        } catch (Exception e) {
            log.error("获取用户信息失败 userId:{}", userId, e);
        }
        return info;
    }

    /**
     * 缓存离线消息
     */
    private void cacheOfflineMessage(Integer userId, Map<String, Object> message) {
        // 可以使用Redis存储离线消息
        // redisTemplate.opsForList().rightPush("offline:" + userId, message);
        log.info("缓存离线消息给用户: {}", userId);
    }

    /**
     * 连接关闭后的回调
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");

        if (userId != null) {
            // 移除session
            onlineSessions.remove(userId);

            Map<String, WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session.getId());
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);

                    // 更新用户离线状态
                    userMapper.updateUserOnlineStatus(userId, "offline");

                    // 广播用户下线消息
                    broadcastUserStatus(userId, "offline");
                }
            }

            log.info("用户 {} 断开WebSocket连接，当前在线人数: {}", userId, onlineSessions.size());
        }
    }

    /**
     * 发生错误时的回调
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        log.error("WebSocket传输错误 userId:{}", userId, exception);

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
}