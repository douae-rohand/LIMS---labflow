package com.backend.config;

import com.backend.common.tempsreel.AuthentificationStomp;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * WebSocket / STOMP pour l'actualisation en temps réel des écrans.
 * <p>
 * Le navigateur se connecte à {@code /ws} avec son JWT ({@link AuthentificationStomp}) et n'écoute
 * que sa file personnelle : {@code /user/queue/evenements} (données modifiées) et
 * {@code /user/queue/notifications}. Le serveur choisit lui-même les destinataires de chaque
 * événement : aucun abonnement à un topic partagé n'est possible.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthentificationStomp authentificationStomp;

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String originesAutorisees;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue")
                .setHeartbeatValue(new long[]{20_000, 20_000})
                .setTaskScheduler(planificateurHeartbeat());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(Arrays.stream(originesAutorisees.split(","))
                        .map(String::trim).filter(o -> !o.isEmpty()).toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authentificationStomp);
    }

    private static org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler planificateurHeartbeat() {
        var planificateur = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        planificateur.setPoolSize(1);
        planificateur.setThreadNamePrefix("ws-heartbeat-");
        planificateur.initialize();
        return planificateur;
    }
}
