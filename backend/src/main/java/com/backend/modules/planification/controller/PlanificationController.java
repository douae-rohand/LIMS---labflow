package com.backend.modules.planification.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.planification.dto.AffecterAnalysesRequest;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.planification.dto.PlanificationDto;
import com.backend.modules.planification.dto.SuiviResponsableDto;
import com.backend.modules.planification.dto.TechnicienChargeDto;
import com.backend.modules.planification.entity.StatutPlanification;
import com.backend.modules.planification.service.PlanificationService;
import com.backend.modules.planification.service.SuiviResponsableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planifications")
@RequiredArgsConstructor
@Tag(name = "Planification", description = "Affectation des analyses aux techniciens et suivi (M02)")
public class PlanificationController {

    private final PlanificationService planificationService;
    private final SuiviResponsableService suiviResponsableService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanificationDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planificationService.trouverParId(id)));
    }

    @GetMapping("/demande/{demandeId}")
    public ResponseEntity<ApiResponse<List<PlanificationDto>>> listerParDemande(@PathVariable Long demandeId) {
        return ResponseEntity.ok(ApiResponse.success(planificationService.listerParDemande(demandeId)));
    }

    @GetMapping("/analyses")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_GERER')")
    @Operation(summary = "Analyses des demandes en cours (filtre : A_AFFECTER, AFFECTEES, EN_RETARD)")
    public ResponseEntity<ApiResponse<List<LigneTravailDto>>> analyses(@RequestParam(required = false) String filtre) {
        return ResponseEntity.ok(ApiResponse.success(planificationService.listerAnalyses(filtre)));
    }

    @GetMapping("/techniciens")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_GERER')")
    @Operation(summary = "Techniciens actifs du laboratoire et leur charge")
    public ResponseEntity<ApiResponse<List<TechnicienChargeDto>>> techniciens() {
        return ResponseEntity.ok(ApiResponse.success(planificationService.techniciens()));
    }

    @PostMapping("/affectations")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_GERER')")
    @Operation(summary = "Affecter des analyses à un technicien")
    public ResponseEntity<ApiResponse<List<LigneTravailDto>>> affecter(@Valid @RequestBody AffecterAnalysesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Analyses affectées", planificationService.affecter(request)));
    }

    @GetMapping("/suivi")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_GERER')")
    @Operation(summary = "Tableau de suivi du responsable : charge et retards")
    public ResponseEntity<ApiResponse<SuiviResponsableDto>> suivi() {
        return ResponseEntity.ok(ApiResponse.success(suiviResponsableService.suivi()));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('PERM_PLANIFICATION_STATUT')")
    public ResponseEntity<ApiResponse<PlanificationDto>> changerStatut(
            @PathVariable Long id, @RequestParam StatutPlanification statut) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour",
                planificationService.changerStatut(id, statut)));
    }
}
