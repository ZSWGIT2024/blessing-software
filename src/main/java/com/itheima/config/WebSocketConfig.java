// 新建文件：com/itheima/config/WebSocketConfig.java
package com.itheima.config;

import com.itheima.websocket.ChatWebSocketHandler;
import com.itheima.websocket.ChatWebSocketInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatWebSocketInterceptor chatWebSocketInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler,
                           ChatWebSocketInterceptor chatWebSocketInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.chatWebSocketInterceptor = chatWebSocketInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(chatWebSocketInterceptor)
                .setAllowedOrigins("*");  // 生产环境应该配置具体的域名
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(600000L); // 10分钟空闲超时
        return container;
    }
}