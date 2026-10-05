package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.plateforme.dto.AnalysePubliqueDto;
import com.backend.modules.plateforme.dto.LaboratoirePublicDto;
import com.backend.modules.plateforme.service.PlateformeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogue public des laboratoires actifs et de leurs analyses.
 * Lecture seule : schéma central pour les labs, schéma tenant pour le catalogue.
 */
@RestController
@RequestMapping("/api/public/laboratoires")
@RequiredArgsConstructor
@Tag(name = "Laboratoires publics", description = "Liste et fiches des laboratoires ACTIF")
public class LaboratoiresPublicController {

    private final PlateformeService plateformeService;

    @GetMapping
    @Operation(summary = "Lister les laboratoires actifs")
    public ResponseEntity<ApiResponse<List<LaboratoirePublicDto>>> lister() {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.listerLaboratoiresPublics()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un laboratoire actif par identifiant")
    public ResponseEntity<ApiResponse<LaboratoirePublicDto>> obtenir(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.trouverPublicParId(id)));
    }

    @GetMapping("/{id}/analyses")
    @Operation(summary = "Analyses actives proposées par le laboratoire")
    public ResponseEntity<ApiResponse<List<AnalysePubliqueDto>>> analyses(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.listerAnalysesPubliques(id)));
    }
}
