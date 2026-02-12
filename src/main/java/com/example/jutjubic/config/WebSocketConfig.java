package com.example.jutjubic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket konfiguracija za Watch Party real-time komunikaciju
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitmqPassword;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple Broker za Watch Party i Video Chat WebSocket komunikaciju
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix za app destination mappings
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint sa SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Dozvoli sve origine (za testiranje sa 2 računara)
                .withSockJS(); // SockJS fallback za browsere koji ne podržavaju WebSocket
    }
}
