package com.backend.modules.accueil.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import com.backend.modules.accueil.service.SuiviAccueilService;
import com.backend.modules.demande.dto.DemandeResumeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Suivi des demandes du laboratoire en lecture seule, pour renseigner les clients à l'accueil. */
@RestController
@RequestMapping("/api/accueil/demandes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ACCUEIL') and hasAuthority('PERM_DEMANDES_SUIVI')")
@Tag(name = "Accueil - suivi des demandes", description = "Consultation des demandes sans modification")
public class SuiviAccueilController {

    private final SuiviAccueilService suiviService;

    @GetMapping
    @Operation(summary = "Demandes du laboratoire (recherche, filtre de statut), les plus récentes d'abord")
    public ResponseEntity<ApiResponse<PageResponse<DemandeResumeDto>>> lister(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(suiviService.lister(q, statut, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une demande : analyses, échantillons, historique, facture et rapport")
    public ResponseEntity<ApiResponse<SuiviAccueilService.SuiviDemandeDto>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(suiviService.detail(id)));
    }
}
