package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.auth.service.RefreshCookieService;
import com.backend.modules.plateforme.dto.MonCompteDtos.MaSessionDto;
import com.backend.modules.plateforme.dto.MonCompteDtos.ModifierMonCompteRequest;
import com.backend.modules.plateforme.dto.MonCompteDtos.MonCompteDto;
import com.backend.modules.plateforme.service.MonCompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
@Tag(name = "Plateforme — Mon compte", description = "Compte personnel du Super Administrateur")
public class MonCompteController {

    /**
     * Les sessions sont servies sous /api/auth : le cookie HttpOnly du jeton de
     * rafraîchissement n'est envoyé que sur ce chemin, ce qui permet de
     * reconnaître la session du navigateur appelant.
     */
    private static final String SESSIONS = "/auth/mes-sessions";

    private final MonCompteService monCompteService;
    private final RefreshCookieService refreshCookieService;

    @GetMapping("/plateforme/mon-compte")
    @Operation(summary = "Mes informations")
    public ResponseEntity<ApiResponse<MonCompteDto>> obtenir(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return ResponseEntity.ok(ApiResponse.success(monCompteService.obtenir(moi.getId())));
    }

    @PatchMapping("/plateforme/mon-compte")
    @Operation(summary = "Modifier mes informations personnelles")
    public ResponseEntity<ApiResponse<MonCompteDto>> modifier(
            @AuthenticationPrincipal UtilisateurPrincipal moi,
            @Valid @RequestBody ModifierMonCompteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Informations enregistrées",
                monCompteService.modifier(moi.getId(), request, moi.getEmail())));
    }

    @GetMapping(SESSIONS)
    @Operation(summary = "Mes sessions ouvertes")
    public ResponseEntity<ApiResponse<List<MaSessionDto>>> sessions(
            @AuthenticationPrincipal UtilisateurPrincipal moi, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.success(
                monCompteService.sessions(moi.getId(), refreshCookieService.extraireRefreshToken(http))));
    }

    @DeleteMapping(SESSIONS + "/{sessionId}")
    @Operation(summary = "Fermer une de mes sessions (hors session en cours)")
    public ResponseEntity<ApiResponse<Void>> revoquer(
            @AuthenticationPrincipal UtilisateurPrincipal moi,
            @PathVariable Long sessionId,
            HttpServletRequest http) {
        monCompteService.revoquerSession(moi.getId(), sessionId,
                refreshCookieService.extraireRefreshToken(http), moi.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Session fermée", null));
    }

    @PostMapping(SESSIONS + "/revoquer-autres")
    @Operation(summary = "Déconnecter toutes mes autres sessions")
    public ResponseEntity<ApiResponse<Integer>> revoquerAutres(
            @AuthenticationPrincipal UtilisateurPrincipal moi, HttpServletRequest http) {
        int fermees = monCompteService.revoquerAutres(moi.getId(),
                refreshCookieService.extraireRefreshToken(http), moi.getEmail());
        return ResponseEntity.ok(ApiResponse.success(fermees + " session(s) fermée(s)", fermees));
    }
}
