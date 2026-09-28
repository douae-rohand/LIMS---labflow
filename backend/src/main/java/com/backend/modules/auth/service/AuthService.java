package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.config.JwtConfig;
import com.backend.modules.auth.dto.*;
import com.backend.modules.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Service d'authentification : login, refresh token, 2FA, logout.
 *
 * <p>Flux d'authentification complet :
 * <ol>
 *   <li>{@code POST /api/auth/login} → valide email/mdp</li>
 *   <li>Si 2FA activée → génère OTP, retourne {@code requiresTwoFactor=true}</li>
 *   <li>{@code POST /api/auth/2fa/verify} → valide OTP, retourne les tokens JWT</li>
 *   <li>{@code POST /api/auth/refresh} → renouvelle le token d'accès</li>
 *   <li>{@code POST /api/auth/logout} → invalide le refresh token</li>
 * </ol>
 *
 * <p>TODO : implémenter la blacklist des refresh tokens (Redis ou table DB).
 * TODO : lier l'utilisateur à son tenantId via la table {@code utilisateur}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TwoFactorService twoFactorService;
    private final JwtConfig jwtConfig;

    // -------------------------------------------------------------------------
    // Connexion
    // -------------------------------------------------------------------------

    /**
     * Authentifie l'utilisateur et retourne les tokens JWT (ou déclenche la 2FA).
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getMotDePasse()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // TODO: charger l'utilisateur depuis la base pour obtenir tenantId, rôle, 2FA activée
        // Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
        //         .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", request.getEmail()));

        // Stub : tenant et rôle temporaires
        String tenantId = resolveTenantId(request.getEmail());
        boolean twoFactorEnabled = false; // TODO: utilisateur.isTwoFactorEnabled()

        if (twoFactorEnabled) {
            // Générer un token temporaire 2FA
            String tempToken = jwtTokenProvider.generateRefreshToken(request.getEmail());
            String otp = twoFactorService.genererOtp(tempToken);
            // TODO: envoyer l'OTP par email via EmailService
            log.debug("2FA requis pour {} – OTP généré (à envoyer par email)", request.getEmail());

            return LoginResponse.builder()
                    .requiresTwoFactor(true)
                    .twoFactorToken(tempToken)
                    .build();
        }

        return buildLoginResponse(authentication, tenantId);
    }

    // -------------------------------------------------------------------------
    // Vérification 2FA
    // -------------------------------------------------------------------------

    /**
     * Vérifie le code OTP et finalise la connexion.
     */
    public LoginResponse verifyTwoFactor(TwoFactorRequest request) {
        if (!jwtTokenProvider.validateToken(request.getTwoFactorToken())) {
            throw new BusinessRuleException("2FA_TOKEN_INVALID", "Token 2FA invalide ou expiré");
        }

        if (!twoFactorService.verifierOtp(request.getTwoFactorToken(), request.getCode())) {
            throw new BusinessRuleException("2FA_CODE_INVALID", "Code OTP incorrect ou expiré");
        }

        String email = jwtTokenProvider.getUsernameFromToken(request.getTwoFactorToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String tenantId = resolveTenantId(email);
        return buildLoginResponse(authentication, tenantId);
    }

    // -------------------------------------------------------------------------
    // Refresh token
    // -------------------------------------------------------------------------

    /**
     * Renouvelle le token d'accès à partir d'un refresh token valide.
     */
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessRuleException("REFRESH_TOKEN_INVALID", "Refresh token invalide ou expiré");
        }

        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String tenantId = resolveTenantId(email);
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication, tenantId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        // TODO: invalider l'ancien refresh token (blacklist Redis)

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .tenantId(tenantId)
                .build();
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    /**
     * Invalide le refresh token (logout côté serveur).
     */
    public void logout(String refreshToken) {
        // TODO: ajouter le refresh token à la blacklist Redis
        log.info("Logout – refresh token invalidé");
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private LoginResponse buildLoginResponse(Authentication authentication, String tenantId) {
        String accessToken = jwtTokenProvider.generateAccessToken(authentication, tenantId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                ((UserDetails) authentication.getPrincipal()).getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .tenantId(tenantId)
                .build();
    }

    /**
     * TODO: remplacer par une vraie requête en base (table utilisateur → laboratoire).
     */
    private String resolveTenantId(String email) {
        // Stub de résolution du tenant – à implémenter avec le module utilisateur
        return "central";
    }
}
