package com.backend.modules.demande.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.demande.dto.DemandeDto;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.service.DemandeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
@Tag(name = "Demandes", description = "Gestion des demandes d'analyse (M01)")
public class DemandeController {

    private final DemandeService demandeService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DemandeDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(demandeService.trouverParId(id)));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ACCUEIL','TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<Page<DemandeDto>>> listerParStatut(
            @PathVariable StatutDemande statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeService.listerParStatut(statut, PageRequest.of(page, size))));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<DemandeDto>> creer(@RequestBody DemandeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demande créée", demandeService.creer(dto)));
    }

    @PatchMapping("/{id}/soumettre")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<DemandeDto>> soumettre(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Demande soumise", demandeService.soumettre(id)));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ACCUEIL','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<DemandeDto>> changerStatut(
            @PathVariable Long id, @RequestParam StatutDemande statut) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour",
                demandeService.changerStatut(id, statut)));
    }

    // TODO: GET /api/demandes/stats
}
