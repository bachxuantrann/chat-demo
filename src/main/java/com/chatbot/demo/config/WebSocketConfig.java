package com.chatbot.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket với STOMP protocol.
 *
 * - /ws         : endpoint để client kết nối WebSocket (hỗ trợ SockJS fallback)
 * - /topic      : prefix cho server → client broadcast (pub/sub)
 * - /app        : prefix cho client → server message mapping
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Server dùng simple in-memory broker để broadcast tin nhắn tới các subscriber
        registry.enableSimpleBroker("/topic");
        // Prefix cho các @MessageMapping method trong controller
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Cho phép tất cả origin (dev only - production nên restrict)
                .setAllowedOriginPatterns("*")
                // SockJS fallback cho browser không hỗ trợ native WebSocket
                .withSockJS();
    }
}
