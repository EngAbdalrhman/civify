package com.app.civify.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time notifications.
 * 
 * Endpoints:
 * - /ws/notifications - WebSocket connection endpoint (with SockJS fallback)
 * 
 * Subscriptions:
 * - /topic/notifications - Broadcast notifications (all users)
 * - /user/{userId}/queue/notifications - User-specific notifications
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for /topic and /queue destinations
        config.enableSimpleBroker("/topic", "/queue");
        
        // Application destination prefix for client->server messages
        config.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*") // Configure CORS as needed
                .withSockJS();
    }
}
