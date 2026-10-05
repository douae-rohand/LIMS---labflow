package com.backend.modules.auth.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.auth.dto.*;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints d'authentification.
 *
 * <p>Base path : {@code /api/auth}
 * <ul>
 *   <li>{@code /login}, {@code /refresh}, {@code /2fa/valider} — publics</li>
 *   <li>{@code /2fa/setup}, {@code /2fa/activer} — acceptent twoFactorToken (dans le body) ou Access Token (Bearer)</li>
 *   <li>{@code /logout} — nécessite un Access Token valide (Bearer)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Login, refresh token, 2FA, logout")
public class AuthController {

    private final AuthService authService;

    // -------------------------------------------------------------------------
    // Connexion
    // -------------------------------------------------------------------------

    @PostMapping("/login")
    @Operation(summary = "Connexion avec email et mot de passe")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", response));
    }

    // -------------------------------------------------------------------------
    // Authentification à deux facteurs — validation
    // -------------------------------------------------------------------------

    @PostMapping("/2fa/valider")
    @Operation(summary = "Validation du code TOTP (2FA)")
    public ResponseEntity<ApiResponse<LoginResponse>> validerTwoFactor(
            @Valid @RequestBody TwoFactorRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.validerTwoFactor(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("2FA validée", response));
    }

    // -------------------------------------------------------------------------
    // Refresh token
    // -------------------------------------------------------------------------

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du token d'accès (rotation)")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Token renouvelé", response));
    }

    // -------------------------------------------------------------------------
    // Logout — nécessite un access token valide
    // -------------------------------------------------------------------------

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Déconnexion (révocation du refresh token)")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        authService.logout(request.getRefreshToken(), principal);
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie", null));
    }

    // -------------------------------------------------------------------------
    // 2FA — Enrôlement : génération du secret (setup)
    // -------------------------------------------------------------------------

    @PostMapping("/2fa/setup")
    @Operation(summary = "Génère le secret TOTP et retourne l'URL QR code")
    public ResponseEntity<ApiResponse<SetupTwoFactorResponse>> setupTwoFactor(
            @RequestBody(required = false) SetupTwoFactorRequest request,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        SetupTwoFactorResponse response = authService.setupTwoFactor(request, principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Secret TOTP généré. Scannez le QR code puis appelez /2fa/activer.",
                response));
    }

    // -------------------------------------------------------------------------
    // 2FA — Enrôlement : activation après vérification du premier code
    // -------------------------------------------------------------------------

    @PostMapping("/2fa/activer")
    @Operation(summary = "Active la 2FA après vérification du premier code TOTP")
    public ResponseEntity<ApiResponse<Object>> activerTwoFactor(
            @Valid @RequestBody ActivateTwoFactorRequest request,
            @AuthenticationPrincipal UtilisateurPrincipal principal,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.activerTwoFactor(request, principal, httpRequest);
        if (response != null) {
            return ResponseEntity.ok(ApiResponse.success("2FA activée avec succès. Connexion établie.", response));
        }
        return ResponseEntity.ok(ApiResponse.success("2FA activée avec succès", null));
    }
}
