package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.config.JwtConfig;
import com.backend.modules.auth.dto.*;
import com.backend.modules.auth.security.JwtTokenProvider;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TwoFactorService twoFactorService;
    private final RefreshTokenService refreshTokenService;
    private final JwtConfig jwtConfig;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    private final TwoFactorRateLimiter twoFactorRateLimiter;

    // -------------------------------------------------------------------------
    // Connexion
    // -------------------------------------------------------------------------

    @Transactional
    public Object login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getMotDePasse()));
        } catch (DisabledException ex) {
            // Compte inactif — distinguer "non confirmé" de "désactivé par admin"
            // UNIQUEMENT si le mot de passe est correct pour éviter l'énumération
            Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                    .orElse(null);
            if (utilisateur != null
                    && passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasseHash())
                    && !utilisateur.isCompteConfirme()) {
                // Mot de passe correct + compte en attente de confirmation → 403 spécifique
                throw new BusinessRuleException("COMPTE_NON_ACTIVE",
                        "Votre compte n'est pas encore activé. Vérifiez votre boîte email.");
            }
            // Tout autre cas (désactivé par admin, mauvais MDP sur compte inactif) → 401 générique
            log.warn("Tentative de connexion échouée pour {}: {}", request.getEmail(), ex.getMessage());
            throw new BadCredentialsException("Identifiants invalides");
        } catch (Exception ex) {
            log.warn("Tentative de connexion échouée pour {}: {}", request.getEmail(), ex.getMessage());
            throw new BadCredentialsException("Identifiants invalides");
        }

        UtilisateurPrincipal principal = (UtilisateurPrincipal) authentication.getPrincipal();

        if (!principal.isEnabled()) {
            log.warn("Tentative de connexion sur compte inactif : {}", request.getEmail());
            throw new DisabledException("Compte désactivé");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Utilisateur introuvable"));

        boolean is2faRequiredByRole = is2faRequiredForRole(principal.getRole());
        boolean is2faConfigured = utilisateur.isDoubleAuthentification() && utilisateur.getSecret2fa() != null && !utilisateur.getSecret2fa().isBlank();

        // Cas (a) : 2FA requis/activé (soit rôle obligatoire soit activation volontaire)
        if (is2faConfigured) {
            String twoFactorToken = jwtTokenProvider.generateTwoFactorToken(principal.getEmail());
            log.debug("2FA requise pour {} — token 2FA généré", principal.getEmail());
            return LoginResponse.builder()
                    .requiresTwoFactor(true)
                    .setupRequired(false)
                    .twoFactorToken(twoFactorToken)
                    .userId(principal.getId())
                    .email(principal.getEmail())
                    .nomComplet(principal.getNomComplet())
                    .role(principal.getRole().name())
                    .build();
        }

        // Cas (b) : 2FA obligatoire par le rôle mais non encore configuré
        if (is2faRequiredByRole) {
            String twoFactorToken = jwtTokenProvider.generateTwoFactorToken(principal.getEmail());
            log.debug("2FA obligatoire mais non configurée pour {} — token 2FA généré pour enrôlement", principal.getEmail());
            return LoginResponse.builder()
                    .requiresTwoFactor(true)
                    .setupRequired(true)
                    .twoFactorToken(twoFactorToken)
                    .userId(principal.getId())
                    .email(principal.getEmail())
                    .nomComplet(principal.getNomComplet())
                    .role(principal.getRole().name())
                    .build();
        }

        // Cas (c) : 2FA non requis -> émission des jetons finaux
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return buildLoginResult(principal, httpRequest);
    }

    /**
     * Règle de décision centralisée : détermine si le 2FA est OBLIGATOIRE pour un rôle.
     *
     * TODO(2FA) : réactiver l'obligation pour RESPONSABLE et ADMINISTRATEUR
     * une fois le flux 2FA finalisé côté frontend.
     * Retirer ce TODO et remettre :
     *   return role == RoleUtilisateur.RESPONSABLE || role == RoleUtilisateur.ADMINISTRATEUR;
     */
    public boolean is2faRequiredForRole(RoleUtilisateur role) {
        // Temporairement désactivé pour tous les rôles — à réactiver après finalisation du frontend 2FA
        return false;
    }

    // -------------------------------------------------------------------------
    // Validation 2FA (code TOTP)
    // -------------------------------------------------------------------------

    @Transactional
    public LoginResult validerTwoFactor(TwoFactorRequest request, HttpServletRequest httpRequest) {
        // Refuser un access token utilisé comme twoFactorToken
        if (jwtTokenProvider.validateToken(request.getTwoFactorToken(), "access")) {
            throw new BusinessRuleException("2FA_TOKEN_INVALID", "Un token d'accès ne peut pas être utilisé comme twoFactorToken");
        }

        // Valider le token temporaire de type 2fa
        if (!jwtTokenProvider.validateToken(request.getTwoFactorToken(), "2fa")) {
            throw new BusinessRuleException("2FA_TOKEN_INVALID", "Token 2FA invalide ou expiré");
        }

        String email = jwtTokenProvider.getUsernameFromToken(request.getTwoFactorToken());

        // Compteur 5 échecs / 5 minutes
        if (twoFactorRateLimiter.isBlocked(email)) {
            throw new BusinessRuleException("2FA_RATE_LIMIT", "Trop de tentatives échouées. Compte temporairement bloqué 5 minutes.");
        }

        Utilisateur utilisateur = utilisateurRepository.findByEmailWithRoleAndLaboratoire(email)
                .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Utilisateur introuvable"));

        // Vérifier le code TOTP
        if (!twoFactorService.verifierTotp(utilisateur.getSecret2fa(), request.getCode())) {
            twoFactorRateLimiter.recordFailure(email);
            throw new BusinessRuleException("2FA_CODE_INVALID", "Code TOTP incorrect ou expiré");
        }

        twoFactorRateLimiter.resetAttempts(email);

        // Charger le principal et construire la réponse complète
        UtilisateurPrincipal principal = (UtilisateurPrincipal) userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return buildLoginResult(principal, httpRequest);
    }

    // -------------------------------------------------------------------------
    // Setup 2FA (accept access token OR twoFactorToken)
    // -------------------------------------------------------------------------

    @Transactional
    public SetupTwoFactorResponse setupTwoFactor(SetupTwoFactorRequest request, UtilisateurPrincipal principal) {
        Utilisateur utilisateur = getUtilisateurFromRequestOrPrincipal(request != null ? request.twoFactorToken() : null, principal);
        String otpAuthUrl = twoFactorService.setupTotp(utilisateur);
        return new SetupTwoFactorResponse(otpAuthUrl);
    }

    // -------------------------------------------------------------------------
    // Activer 2FA (accept access token OR twoFactorToken)
    // -------------------------------------------------------------------------

    @Transactional
    public LoginResult activerTwoFactor(ActivateTwoFactorRequest request, UtilisateurPrincipal principal, HttpServletRequest httpRequest) {
        Utilisateur utilisateur = getUtilisateurFromRequestOrPrincipal(request != null ? request.twoFactorToken() : null, principal);
        twoFactorService.activerTotp(utilisateur, request.code());

        // Si l'activation se fait pendant un enrôlement obligatoire (via twoFactorToken), émettre directement les tokens finaux
        if (request != null && request.twoFactorToken() != null && !request.twoFactorToken().isBlank()) {
            UtilisateurPrincipal userPrincipal = (UtilisateurPrincipal) userDetailsService.loadUserByUsername(utilisateur.getEmail());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, userPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return buildLoginResult(userPrincipal, httpRequest);
        }

        return null;
    }

    private Utilisateur getUtilisateurFromRequestOrPrincipal(String twoFactorToken, UtilisateurPrincipal principal) {
        if (twoFactorToken != null && !twoFactorToken.isBlank()) {
            if (jwtTokenProvider.validateToken(twoFactorToken, "access")) {
                throw new BusinessRuleException("2FA_TOKEN_INVALID", "Un token d'accès ne peut pas être passé dans deuxFactorToken");
            }
            if (!jwtTokenProvider.validateToken(twoFactorToken, "2fa")) {
                throw new BusinessRuleException("2FA_TOKEN_INVALID", "Token 2FA invalide ou expiré");
            }
            String email = jwtTokenProvider.getUsernameFromToken(twoFactorToken);
            return utilisateurRepository.findByEmailWithRoleAndLaboratoire(email)
                    .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Utilisateur introuvable"));
        } else if (principal != null) {
            return utilisateurRepository.findById(principal.getId())
                    .orElseThrow(() -> new BusinessRuleException("USER_NOT_FOUND", "Utilisateur introuvable"));
        } else {
            throw new BusinessRuleException("AUTH_REQUIRED", "Authentification d'accès ou token 2FA requis");
        }
    }

    public record LoginResult(LoginResponse response, String rawRefreshToken) {}

    // -------------------------------------------------------------------------
    // Refresh token (rotation)
    // -------------------------------------------------------------------------

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public LoginResult refreshToken(String rawRefreshToken, HttpServletRequest httpRequest) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessRuleException("REFRESH_TOKEN_INVALID", "Refresh token absent des cookies");
        }

        // Refuser un twoFactorToken utilisé comme refresh token
        if (jwtTokenProvider.validateToken(rawRefreshToken, "2fa")) {
            throw new BusinessRuleException("REFRESH_TOKEN_INVALID", "Un token 2FA ne peut pas être utilisé comme refresh token");
        }

        RefreshTokenService.RotationResult rotation =
                refreshTokenService.rotation(rawRefreshToken, userAgent(httpRequest));

        UtilisateurPrincipal principal =
                (UtilisateurPrincipal) userDetailsService.loadUserByUsername(rotation.email());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .userId(principal.getId())
                .email(principal.getEmail())
                .nomComplet(principal.getNomComplet())
                .role(principal.getRole().name())
                .tenantId(principal.getNomSchema())
                .mustChangePassword(principal.isMustChangePassword())
                .build();

        return new LoginResult(response, rotation.nouveauToken());
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Transactional
    public void logout(String rawRefreshToken, UtilisateurPrincipal principal) {
        if (principal == null) {
            throw new BusinessRuleException("UNAUTHORIZED", "Authentification d'accès requise pour la déconnexion");
        }
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoquer(rawRefreshToken, principal.getId());
        }
        SecurityContextHolder.clearContext();
        log.info("Logout — refresh token révoqué pour userId={}", principal.getId());
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    public LoginResult buildLoginResult(UtilisateurPrincipal principal, HttpServletRequest httpRequest) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal.getEmail());

        // Mise à jour de la date de dernière connexion uniquement à l'émission des tokens finaux
        utilisateurRepository.findById(principal.getId()).ifPresent(utilisateur -> {
            utilisateur.setDerniereConnexion(Instant.now());
            refreshTokenService.persister(refreshToken, utilisateur, userAgent(httpRequest));
            utilisateurRepository.save(utilisateur);
        });

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .userId(principal.getId())
                .email(principal.getEmail())
                .nomComplet(principal.getNomComplet())
                .role(principal.getRole().name())
                .tenantId(principal.getNomSchema())
                .mustChangePassword(principal.isMustChangePassword())
                .build();

        return new LoginResult(response, refreshToken);
    }

    private static String userAgent(HttpServletRequest request) {
        if (request == null) return null;
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua.substring(0, Math.min(ua.length(), 255)) : null;
    }
}
