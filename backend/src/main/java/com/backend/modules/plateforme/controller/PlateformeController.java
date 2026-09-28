package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.plateforme.dto.*;
import com.backend.modules.plateforme.entity.StatutIntegration;
import com.backend.modules.plateforme.service.PlateformeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints de gestion de la plateforme (M15).
 * Réservés au SUPER_ADMINISTRATEUR sauf la soumission de demande.
 * Base path : {@code /api/plateforme}
 */
@RestController
@RequestMapping("/api/plateforme")
@RequiredArgsConstructor
@Tag(name = "Plateforme", description = "Gestion des laboratoires et demandes d'intégration (M15)")
public class PlateformeController {

    private final PlateformeService plateformeService;

    // -------------------------------------------------------------------------
    // Laboratoires
    // -------------------------------------------------------------------------

    @GetMapping("/laboratoires")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Lister tous les laboratoires")
    public ResponseEntity<ApiResponse<Page<LaboratoireDto>>> listerLaboratoires(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                plateformeService.listerLaboratoires(PageRequest.of(page, size))));
    }

    @GetMapping("/laboratoires/{code}")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Obtenir un laboratoire par son code")
    public ResponseEntity<ApiResponse<LaboratoireDto>> trouverLaboratoire(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(plateformeService.trouverParCode(code)));
    }

    @PostMapping("/laboratoires")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Créer un nouveau laboratoire (tenant)")
    public ResponseEntity<ApiResponse<LaboratoireDto>> creerLaboratoire(
            @Valid @RequestBody CreerLaboratoireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Laboratoire créé", plateformeService.creerLaboratoire(request)));
    }

    @PatchMapping("/laboratoires/{id}/desactiver")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Désactiver un laboratoire")
    public ResponseEntity<ApiResponse<Void>> desactiverLaboratoire(@PathVariable Long id) {
        plateformeService.desactiverLaboratoire(id);
        return ResponseEntity.ok(ApiResponse.success("Laboratoire désactivé", null));
    }

    // -------------------------------------------------------------------------
    // Demandes d'intégration
    // -------------------------------------------------------------------------

    @PostMapping("/demandes")
    @Operation(summary = "Soumettre une demande d'intégration (public)")
    public ResponseEntity<ApiResponse<DemandeIntegrationDto>> soumettreDemande(
            @Valid @RequestBody DemandeIntegrationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demande soumise", plateformeService.soumettreDemande(dto)));
    }

    @GetMapping("/demandes")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Lister les demandes d'intégration")
    public ResponseEntity<ApiResponse<Page<DemandeIntegrationDto>>> listerDemandes(
            @RequestParam(required = false) StatutIntegration statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                plateformeService.listerDemandes(statut, PageRequest.of(page, size))));
    }

    @PatchMapping("/demandes/{id}/traiter")
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Approuver ou rejeter une demande d'intégration")
    public ResponseEntity<ApiResponse<DemandeIntegrationDto>> traiterDemande(
            @PathVariable Long id,
            @RequestParam StatutIntegration decision,
            @RequestParam(required = false) String commentaire) {
        return ResponseEntity.ok(ApiResponse.success("Demande traitée",
                plateformeService.traiterDemande(id, decision, commentaire)));
    }
}
