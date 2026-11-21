package com.backend.integration.n8n;

import com.backend.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Authentifie les appels aux endpoints n8n avec le secret partagé. */
@Slf4j
@Component
@RequiredArgsConstructor
public class N8nSecretFilter extends OncePerRequestFilter {

    private static final String PREFIXE_N8N = "/api/n8n";
    private static final String ENTETE_SECRET = "X-Webhook-Secret";

    @Value("${n8n.webhook-secret:}")
    private String webhookSecret;

    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String chemin = request.getServletPath();
        return !chemin.equals(PREFIXE_N8N) && !chemin.startsWith(PREFIXE_N8N + "/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Endpoint n8n refusé : secret partagé non configuré");
            repondreErreur(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Configuration du secret webhook indisponible");
            return;
        }

        String secretRecu = request.getHeader(ENTETE_SECRET);
        String candidat = secretRecu == null ? "" : secretRecu;
        boolean identique = MessageDigest.isEqual(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                candidat.getBytes(StandardCharsets.UTF_8));
        if (secretRecu == null || !identique) {
            repondreErreur(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Secret webhook invalide ou manquant");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void repondreErreur(HttpServletResponse response, int statut, String message) throws IOException {
        response.setStatus(statut);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
