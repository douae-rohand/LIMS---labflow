package com.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket / STOMP pour les notifications temps réel.
 * Les clients s'abonnent à /topic/notifications/{tenantId}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker en mémoire pour les destinations /topic et /queue
        registry.enableSimpleBroker("/topic", "/queue");
        // Préfixe pour les messages envoyés côté client vers le serveur
        registry.setApplicationDestinationPrefixes("/app");
        // Préfixe pour les destinations utilisateur personnelles
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // fallback SockJS pour les navigateurs incompatibles
    }
}
