package com.backend.modules.echantillon.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.echantillon.dto.EchantillonDto;
import com.backend.modules.echantillon.entity.StatutEchantillon;
import com.backend.modules.echantillon.service.EchantillonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/echantillons")
@RequiredArgsConstructor
@Tag(name = "Echantillons", description = "Gestion des échantillons (M03)")
public class EchantillonController {

    private final EchantillonService echantillonService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EchantillonDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(echantillonService.trouverParId(id)));
    }

    @GetMapping("/demande/{demandeId}")
    public ResponseEntity<ApiResponse<Page<EchantillonDto>>> listerParDemande(
            @PathVariable Long demandeId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                echantillonService.listerParDemande(demandeId, PageRequest.of(page, size))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ACCUEIL','TECHNICIEN','RESPONSABLE')")
    public ResponseEntity<ApiResponse<EchantillonDto>> enregistrer(@RequestBody EchantillonDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Échantillon enregistré", echantillonService.enregistrer(dto)));
    }

    @PatchMapping("/{id}/receptionner")
    @PreAuthorize("hasRole('ACCUEIL')")
    public ResponseEntity<ApiResponse<EchantillonDto>> receptionner(
            @PathVariable Long id, @RequestParam Long receptionnaireId) {
        return ResponseEntity.ok(ApiResponse.success("Réceptionné",
                echantillonService.receptionner(id, receptionnaireId)));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<EchantillonDto>> changerStatut(
            @PathVariable Long id, @RequestParam StatutEchantillon statut) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour",
                echantillonService.changerStatut(id, statut)));
    }
}
