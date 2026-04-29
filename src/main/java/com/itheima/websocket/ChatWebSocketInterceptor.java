// 新建文件：com/itheima/websocket/ChatWebSocketInterceptor.java
package com.itheima.websocket;

import com.itheima.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class ChatWebSocketInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public ChatWebSocketInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // 从URL参数中获取token
            String token = servletRequest.getServletRequest().getParameter("token");
            String userId = servletRequest.getServletRequest().getParameter("userId");

            log.info("WebSocket握手 - userId: {}, token存在: {}", userId, token != null);

            // 使用优化后的JwtUtil验证token
            if (token != null && jwtUtil.validateToken(token)) {
                // 从token中获取用户ID
                Integer tokenUserId = jwtUtil.getUserIdFromToken("Bearer " + token);

                if (userId != null && tokenUserId != null &&
                        userId.equals(String.valueOf(tokenUserId))) {

                    attributes.put("userId", tokenUserId);
                    attributes.put("token", token);

                    log.info("WebSocket认证成功 - userId: {}", tokenUserId);
                    return true;
                }
            }

            log.warn("WebSocket认证失败 - userId: {}", userId);
            return false;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 可选：记录握手成功日志
        if (exception != null) {
            log.error("WebSocket握手异常", exception);
        } else {
            log.debug("WebSocket握手完成");
        }
    }
}