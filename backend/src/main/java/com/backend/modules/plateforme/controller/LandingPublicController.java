package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.plateforme.dto.LandingPublicDto;
import com.backend.modules.plateforme.service.PlateformeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Données publiques de la landing page.
 * Aucune authentification : lecture seule sur le schéma central.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Landing publique", description = "Données dynamiques de la page d'accueil")
public class LandingPublicController {

    private final PlateformeService plateformeService;

    @GetMapping("/landing")
    @Operation(summary = "Agrégat landing : statistiques, rôles, laboratoires actifs, statuts de demande")
    public ResponseEntity<ApiResponse<LandingPublicDto>> obtenirLanding() {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.obtenirLandingPublic()));
    }
}
