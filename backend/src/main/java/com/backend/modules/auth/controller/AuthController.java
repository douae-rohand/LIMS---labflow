package com.backend.modules.auth.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.auth.dto.*;
import com.backend.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints d'authentification (publics — exclus de la chaîne JWT).
 *
 * <p>Base path : {@code /api/auth}
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
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", response));
    }

    // -------------------------------------------------------------------------
    // Authentification à deux facteurs
    // -------------------------------------------------------------------------

    @PostMapping("/2fa/verify")
    @Operation(summary = "Vérification du code OTP (2FA)")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyTwoFactor(
            @Valid @RequestBody TwoFactorRequest request) {
        LoginResponse response = authService.verifyTwoFactor(request);
        return ResponseEntity.ok(ApiResponse.success("2FA validée", response));
    }

    // -------------------------------------------------------------------------
    // Refresh token
    // -------------------------------------------------------------------------

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du token d'accès")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token renouvelé", response));
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion (invalidation du refresh token)")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie", null));
    }
}
