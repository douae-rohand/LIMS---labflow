package com.backend.modules.auth.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.auth.service.RefreshCookieService;
import com.backend.modules.plateforme.dto.ModifierProfilRequest;
import com.backend.modules.plateforme.dto.ProfilCompteDto;
import com.backend.modules.plateforme.dto.SessionActiveDto;
import com.backend.modules.plateforme.service.CompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/compte")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Compte", description = "Profil, sessions et 2FA du compte connecté")
public class CompteController {

    private final CompteService compteService;
    private final RefreshCookieService refreshCookieService;

    @GetMapping
    @Operation(summary = "Lire le profil du compte connecté")
    public ResponseEntity<ApiResponse<ProfilCompteDto>> profil(
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(compteService.profil(principal)));
    }

    @PatchMapping
    @Operation(summary = "Modifier les champs autorisés du profil")
    public ResponseEntity<ApiResponse<ProfilCompteDto>> modifier(
            @AuthenticationPrincipal UtilisateurPrincipal principal,
            @Valid @RequestBody ModifierProfilRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profil mis à jour", compteService.modifier(principal, request)));
    }

    @GetMapping("/sessions")
    @Operation(summary = "Sessions de renouvellement encore valides")
    public ResponseEntity<ApiResponse<List<SessionActiveDto>>> sessions(
            @AuthenticationPrincipal UtilisateurPrincipal principal,
            HttpServletRequest request) {
        String refresh = refreshCookieService.extraireRefreshToken(request).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(compteService.sessions(principal, refresh)));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Révoquer une session de renouvellement du compte connecté")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> revoquer(
            @AuthenticationPrincipal UtilisateurPrincipal principal,
            @PathVariable Long id,
            HttpServletRequest request) {
        String refresh = refreshCookieService.extraireRefreshToken(request).orElse(null);
        boolean courante = compteService.revoquerSession(principal, id, refresh);
        ResponseEntity.BodyBuilder reponse = ResponseEntity.ok();
        if (courante) {
            ResponseCookie cookie = refreshCookieService.effacerCookieRefreshToken();
            reponse.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return reponse.body(ApiResponse.success(
                courante ? "Session courante révoquée" : "Session révoquée",
                Map.of("courante", courante)));
    }
}
