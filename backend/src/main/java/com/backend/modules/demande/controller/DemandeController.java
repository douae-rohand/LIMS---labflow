package com.backend.modules.demande.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import com.backend.modules.demande.dto.DemandeDto;
import com.backend.modules.demande.dto.DemandeLaboratoireDto;
import com.backend.modules.demande.dto.DemandeResumeDto;
import com.backend.modules.demande.dto.IndicateursDemandesDto;
import com.backend.modules.demande.dto.RefuserDemandeRequest;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.service.DemandeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
@Tag(name = "Demandes", description = "Gestion des demandes d'analyse (M01)")
public class DemandeController {

    private final DemandeService demandeService;

    /**
     * Consultation et décision sur les demandes du laboratoire courant.
     * Le schéma lu est celui du compte connecté : un identifiant ou un
     * en-tête {@code X-Tenant-ID} d'un autre laboratoire ne change pas le tenant.
     */
    @GetMapping("/laboratoire")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<PageResponse<DemandeResumeDto>>> listerPourLaboratoire(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeService.listerPourLaboratoire(q, statut, page, size)));
    }

    @GetMapping("/laboratoire/indicateurs")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<IndicateursDemandesDto>> indicateursLaboratoire() {
        return ResponseEntity.ok(ApiResponse.success(demandeService.indicateursLaboratoire()));
    }

    @GetMapping("/laboratoire/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<DemandeLaboratoireDto>> detailLaboratoire(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(demandeService.detailLaboratoire(id)));
    }

    @PostMapping("/laboratoire/{id}/accepter")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<DemandeLaboratoireDto>> accepter(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Demande acceptée", demandeService.accepter(id)));
    }

    @PostMapping("/laboratoire/{id}/refuser")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<DemandeLaboratoireDto>> refuser(
            @PathVariable Long id,
            @Valid @RequestBody RefuserDemandeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Demande refusée", demandeService.refuser(id, request.getMotif())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DemandeDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(demandeService.trouverParId(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<Page<DemandeDto>>> listerMesDemandes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                demandeService.listerMesDemandes(PageRequest.of(page, size))));
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

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ACCUEIL','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<DemandeDto>> changerStatut(
            @PathVariable Long id, @RequestParam StatutDemande statut) {
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour",
                demandeService.changerStatut(id, statut)));
    }
}
