package com.backend.modules.facturation.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import com.backend.modules.facturation.dto.*;
import com.backend.modules.facturation.service.FacturationAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/factures-admin")
@RequiredArgsConstructor
@Tag(name = "Facturation Admin", description = "Gestion de la facturation pour les administrateurs (M11)")
public class FacturationAdminController {

    private final FacturationAdminService facturationAdminService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FACTURATION_CONSULTER')")
    public ResponseEntity<ApiResponse<PageResponse<FactureAdminDto>>> listerFactures(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) LocalDate debut,
            @RequestParam(required = false) LocalDate fin,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "date") String tri,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Page<FactureAdminDto> pageResult = 
                facturationAdminService.listerFactures(statut, debut, fin, clientId, q,
                        page, size, tri, direction);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(pageResult)));
    }

    @GetMapping("/demandes-facturables")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_CONSULTER')")
    public ResponseEntity<ApiResponse<List<DemandeFacturableDto>>> listerDemandesFacturables() {
        return ResponseEntity.ok(ApiResponse.success(facturationAdminService.listerDemandesFacturables()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_CONSULTER')")
    public ResponseEntity<ApiResponse<FactureAdminDto>> getFacture(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(facturationAdminService.getFacture(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FACTURATION_GERER')")
    public ResponseEntity<ApiResponse<FactureAdminDto>> emettreFacture(@Valid @RequestBody EmettreFactureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Facture émise", facturationAdminService.emettreFacture(request)));
    }

    @PostMapping("/{id}/paiements")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_PAYER')")
    public ResponseEntity<ApiResponse<FactureAdminDto>> enregistrerPaiement(
            @PathVariable Long id,
            @Valid @RequestBody EnregistrerPaiementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Paiement enregistré", facturationAdminService.enregistrerPaiement(id, request)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_CONSULTER')")
    public ResponseEntity<ApiResponse<FactureStatsDto>> getStats(
            @RequestParam(required = false) LocalDate debut,
            @RequestParam(required = false) LocalDate fin) {
        return ResponseEntity.ok(ApiResponse.success(facturationAdminService.getStats(debut, fin)));
    }
}
