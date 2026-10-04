package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.config.JwtConfig;
import com.backend.modules.auth.dto.*;
import com.backend.modules.auth.security.JwtTokenProvider;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TwoFactorService twoFactorService;
    private final JwtConfig jwtConfig;
    private final UtilisateurRepository utilisateurRepository;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getMotDePasse()));
        } catch (Exception ex) {
            log.warn("Tentative de connexion échouée pour {}: {}", request.getEmail(), ex.getMessage());
            throw new BadCredentialsException("Identifiants invalides");
        }

        UtilisateurPrincipal principal = (UtilisateurPrincipal) authentication.getPrincipal();

        if (!principal.isEnabled()) {
            log.warn("Tentative de connexion sur compte inactif : {}", request.getEmail());
            throw new DisabledException("Compte désactivé");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Mettre à jour la date de dernière connexion
        utilisateurRepository.findById(principal.getId()).ifPresent(u -> {
            u.setDerniereConnexion(Instant.now());
            utilisateurRepository.save(u);
        });

        return buildLoginResponse(principal);
    }

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
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return buildLoginResponse((UtilisateurPrincipal) userDetails);
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken, "refresh")) {
            throw new BusinessRuleException("REFRESH_TOKEN_INVALID", "Refresh token invalide ou expiré");
        }
        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        UtilisateurPrincipal principal =
                (UtilisateurPrincipal) userDetailsService.loadUserByUsername(email);
        return buildLoginResponse(principal);
    }

    public void logout(String refreshToken) {
        log.info("Logout – refresh token invalidé");
        SecurityContextHolder.clearContext();
    }

    private LoginResponse buildLoginResponse(UtilisateurPrincipal principal) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal.getEmail());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtConfig.getExpiration() / 1000)
                .userId(principal.getId())
                .email(principal.getEmail())
                .nomComplet(principal.getNomComplet())
                .role(principal.getRole().name())
                .tenantId(principal.getNomSchema())
                .mustChangePassword(principal.isMustChangePassword())
                .build();
    }
}
