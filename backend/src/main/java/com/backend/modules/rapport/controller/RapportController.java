package com.backend.modules.rapport.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.rapport.dto.RapportDto;
import com.backend.modules.rapport.entity.StatutRapport;
import com.backend.modules.rapport.service.RapportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/rapports") @RequiredArgsConstructor
@Tag(name = "Rapports", description = "Génération et envoi des rapports d'analyse (M07)")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RapportDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(rapportService.trouverParId(id)));
    }

    @GetMapping("/demande/{demandeId}")
    public ResponseEntity<ApiResponse<Page<RapportDto>>> listerParDemande(
            @PathVariable Long demandeId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(rapportService.listerParDemande(demandeId, PageRequest.of(page, size))));
    }

    @PostMapping("/generer/{demandeId}")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<RapportDto>> generer(
            @PathVariable Long demandeId, @RequestParam Long generateurId) {
        return ResponseEntity.ok(ApiResponse.success("Rapport généré", rapportService.generer(demandeId, generateurId)));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<RapportDto>> changerStatut(
            @PathVariable Long id, @RequestParam StatutRapport statut) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", rapportService.changerStatut(id, statut)));
    }
}
