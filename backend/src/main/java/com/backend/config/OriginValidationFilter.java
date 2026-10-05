package com.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filtre de défense en profondeur pour la vérification dynamique de l'en-tête Origin
 * sur les routes sensibles d'authentification (/api/auth/refresh, /api/auth/logout, /api/auth/2fa/**).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OriginValidationFilter extends OncePerRequestFilter {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsRaw;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Appliquer la vérification uniquement sur /api/auth/refresh, /api/auth/logout, /api/auth/2fa/**
        if (path.startsWith("/api/auth/refresh") || path.startsWith("/api/auth/logout") || path.startsWith("/api/auth/2fa/")) {
            String origin = request.getHeader("Origin");

            if (origin != null && !origin.isBlank()) {
                List<String> allowedOrigins = Arrays.stream(allowedOriginsRaw.split(","))
                        .map(String::trim)
                        .toList();

                // Construire l'origine de la requête elle-même (schéma + hôte + port, ex: http://localhost:8081 pour Swagger)
                String requestSelfOrigin = request.getScheme() + "://" + request.getServerName() +
                        ((request.getServerPort() == 80 && "http".equals(request.getScheme())) ||
                         (request.getServerPort() == 443 && "https".equals(request.getScheme())) ? "" : ":" + request.getServerPort());

                boolean isAllowed = allowedOrigins.contains(origin) || origin.equalsIgnoreCase(requestSelfOrigin);

                if (!isAllowed) {
                    log.warn("Origine non autorisée bloquée sur route sensible [{}] : {}", path, origin);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"Origine non autorisée (CORS/Origin check)\",\"data\":null}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
