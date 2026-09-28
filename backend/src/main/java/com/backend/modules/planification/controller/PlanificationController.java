package com.backend.modules.planification.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.planification.dto.PlanificationDto;
import com.backend.modules.planification.entity.StatutPlanification;
import com.backend.modules.planification.service.PlanificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planifications")
@RequiredArgsConstructor
@Tag(name = "Planification", description = "Planification des analyses (M02)")
public class PlanificationController {

    private final PlanificationService planificationService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlanificationDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planificationService.trouverParId(id)));
    }

    @GetMapping("/demande/{demandeId}")
    public ResponseEntity<ApiResponse<List<PlanificationDto>>> listerParDemande(@PathVariable Long demandeId) {
        return ResponseEntity.ok(ApiResponse.success(planificationService.listerParDemande(demandeId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<PlanificationDto>> creer(@RequestBody PlanificationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Planification créée", planificationService.creer(dto)));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<PlanificationDto>> changerStatut(
            @PathVariable Long id, @RequestParam StatutPlanification statut) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour",
                planificationService.changerStatut(id, statut)));
    }
}
