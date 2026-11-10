package com.backend.common.tempsreel;

import com.backend.modules.auth.security.JwtTokenProvider;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Sécurise le canal STOMP entrant :
 * <ul>
 *   <li>CONNECT : le JWT d'accès (en-tête {@code Authorization: Bearer ...}) est obligatoire et vérifié
 *       comme pour l'API REST ; compte inactif ou laboratoire bloqué → connexion refusée ;</li>
 *   <li>SUBSCRIBE : seules les files personnelles {@code /user/queue/**} sont autorisées ;</li>
 *   <li>SEND : refusé (le navigateur ne fait qu'écouter).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthentificationStomp implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand commande = accessor.getCommand();
        if (commande == StompCommand.CONNECT) {
            accessor.setUser(authentifier(accessor.getFirstNativeHeader("Authorization")));
        } else if (commande == StompCommand.SUBSCRIBE) {
            String destination = accessor.getDestination();
            if (!(accessor.getUser() instanceof UtilisateurConnecte)
                    || destination == null || !destination.startsWith("/user/queue/")) {
                throw new MessagingException("Abonnement refusé : " + destination);
            }
        } else if (commande == StompCommand.SEND) {
            throw new MessagingException("Envoi de messages non autorisé");
        }
        return message;
    }

    private UtilisateurConnecte authentifier(String entete) {
        String jeton = entete != null && entete.startsWith("Bearer ") ? entete.substring(7).trim() : null;
        if (!StringUtils.hasText(jeton) || !jwtTokenProvider.validateToken(jeton, "access")) {
            throw new MessagingException("Jeton d'accès absent ou invalide");
        }
        UserDetails details = userDetailsService.loadUserByUsername(jwtTokenProvider.getUsernameFromToken(jeton));
        if (!(details instanceof UtilisateurPrincipal principal) || !details.isEnabled()) {
            throw new MessagingException("Compte inactif");
        }
        if (principal.isEmployeLaboratoire() && principal.getStatutLaboratoire() != null
                && !"ACTIF".equals(principal.getStatutLaboratoire())) {
            throw new MessagingException("Laboratoire suspendu ou désactivé");
        }
        return new UtilisateurConnecte(principal.getId(), principal.getRole(), principal.getLaboratoireId());
    }
}
