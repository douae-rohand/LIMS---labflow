package com.backend.modules.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Filtre appliqué APRÈS {@link JwtAuthenticationFilter} (qui a déjà chargé le principal).
 *
 * <p>Si {@code mustChangePassword == true}, toute requête authentifiée est refusée
 * avec un 403 JSON {@code {"code":"PASSWORD_CHANGE_REQUIRED", ...}}, SAUF les
 * endpoints autorisés ci-dessous.
 *
 * <p><strong>Position dans la chaîne :</strong> enregistré dans {@code SecurityConfig}
 * avec {@code addFilterAfter(mustChangePasswordFilter, JwtAuthenticationFilter.class)}.
 * Il s'exécute donc après que le principal est positionné dans le
 * {@link SecurityContextHolder}, mais avant que les règles d'autorisation de
 * Spring Security ne soient évaluées — ce qui permet de retourner 403 avant
 * même que {@code @PreAuthorize} ne soit consulté.
 *
 * <p><strong>Endpoints exemptés :</strong>
 * <ul>
 *   <li>{@code POST /api/auth/mot-de-passe/changer} — l'action de changement elle-même</li>
 *   <li>{@code POST /api/auth/logout}                — permet de se déconnecter même si bloqué</li>
 *   <li>{@code POST /api/auth/refresh}               — ne doit pas être bloqué (pas d'access token requis)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> CHEMINS_AUTORISES = Set.of(
            "/api/auth/mot-de-passe/changer",
            "/api/auth/mot-de-passe/oublie",
            "/api/auth/mot-de-passe/reinitialiser",
            "/api/auth/logout",
            "/api/auth/refresh",
            "/api/auth/inscription",
            "/api/auth/activation/confirmer"
    );

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UtilisateurPrincipal principal
                && principal.isMustChangePassword()) {

            String chemin = request.getServletPath();

            if (CHEMINS_AUTORISES.contains(chemin)) {
                // Chemin exempté : laisser passer
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("Requête bloquée — mustChangePassword=true pour userId={}, chemin={}", principal.getId(), chemin);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> body = Map.of(
                    "success", false,
                    "code", "PASSWORD_CHANGE_REQUIRED",
                    "message", "Vous devez changer votre mot de passe avant de continuer.",
                    "timestamp", Instant.now().toString()
            );
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
