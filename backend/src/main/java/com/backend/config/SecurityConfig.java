package com.backend.config;

import com.backend.common.security.CustomAccessDeniedHandler;
import com.backend.common.security.CustomAuthenticationEntryPoint;
import com.backend.modules.auth.security.JwtAuthenticationFilter;
import com.backend.modules.auth.security.MustChangePasswordFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuration de la chaîne de filtres Spring Security.
 *
 * <p>Politique : JWT stateless + RBAC via @PreAuthorize sur les controllers.
 *
 * <p>Endpoints publics :
 * <ul>
 *   <li>{@code /api/auth/**}          – login, refresh, 2FA</li>
 *   <li>{@code /swagger-ui/**}        – documentation API</li>
 *   <li>{@code /v3/api-docs/**}       – spec OpenAPI</li>
 *   <li>{@code /ws/**}                – handshake WebSocket</li>
 *   <li>{@code /actuator/health}      – health check</li>
 *   <li>{@code GET /api/public/**}    – données landing (rôles, labs, stats)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MustChangePasswordFilter mustChangePasswordFilter;
    private final OriginValidationFilter originValidationFilter;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    // -------------------------------------------------------------------------
    // Chaîne de filtres principale
    // -------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS géré par CorsConfig
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Désactiver CSRF (API REST stateless)
            .csrf(AbstractHttpConfigurer::disable)
            // Pas de session HTTP côté serveur
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Gestion des erreurs d'authentification et d'accès en JSON
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            // Règles d'autorisation
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (login, refresh, 2FA valider/setup/activer sans auth Bearer)
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/inscription",
                    "/api/auth/activation/confirmer",
                    "/api/auth/2fa/valider",
                    "/api/auth/2fa/setup",
                    "/api/auth/2fa/activer",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/ws/**",
                    "/actuator/**"
                ).permitAll()
                // Landing page : lecture seule des données publiques (rôles, labs actifs, stats)
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                // Logout exige explicitement une authentification (access token)
                .requestMatchers("/api/auth/logout").authenticated()
                // Formulaire de demande d'intégration laboratoire (landing publique)
                // Doit être déclaré AVANT la règle /api/plateforme/** ci-dessous
                .requestMatchers(HttpMethod.POST, "/api/plateforme/demandes").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/laboratoires", "/api/laboratoires/**").authenticated()
                .requestMatchers("/api/client/**").hasRole("CLIENT")
                // Super-admin uniquement pour tous les autres endpoints plateforme
                .requestMatchers("/api/plateforme/**").hasRole("SUPER_ADMINISTRATEUR")
                // Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            )
            // Fournisseur d'authentification DAO
            .authenticationProvider(authenticationProvider())
            // Defense-in-depth Origin check
            .addFilterBefore(originValidationFilter, UsernamePasswordAuthenticationFilter.class)
            // Filtre JWT avant le filtre Spring par défaut
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Filtre must_change_password — après JWT (principal déjà chargé), avant @PreAuthorize
            .addFilterAfter(mustChangePasswordFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    // -------------------------------------------------------------------------
    // Beans d'authentification
    // -------------------------------------------------------------------------

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
