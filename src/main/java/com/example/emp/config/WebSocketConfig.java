package com.example.emp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket / STOMP 설정
 * - 엔드포인트: /ws  (SockJS 폴백 지원)
 * - 구독 토픽  : /topic/chat
 * - 발행 경로  : /app/chat.send
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 인메모리 브로커: /topic/** 구독
        config.enableSimpleBroker("/topic");
        // 클라이언트 → 서버 메시지 prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Vercel(HTTPS) + 로컬(HTTP) 모두 허용
                .setAllowedOriginPatterns("*")
                // SockJS 폴백: xhr-polling 등으로 WebSocket 미지원 환경 대응
                .withSockJS();
    }
}
