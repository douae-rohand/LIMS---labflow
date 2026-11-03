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

@RestController
@RequestMapping("/api/factures-admin")
@RequiredArgsConstructor
@Tag(name = "Facturation Admin", description = "Gestion de la facturation pour les administrateurs (M11)")
public class FacturationAdminController {

    private final FacturationAdminService facturationAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<PageResponse<FactureAdminDto>>> listerFactures(
            @RequestParam(required = false) Boolean statutPayee,
            @RequestParam(required = false) Boolean echeanceDepassee,
            @RequestParam(required = false) LocalDate debut,
            @RequestParam(required = false) LocalDate fin,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Page<FactureAdminDto> pageResult = 
                facturationAdminService.listerFactures(statutPayee, echeanceDepassee, debut, fin, clientId, q,
                        org.springframework.data.domain.PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(pageResult)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<FactureAdminDto>> getFacture(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(facturationAdminService.getFacture(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<FactureAdminDto>> emettreFacture(@Valid @RequestBody EmettreFactureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Facture émise", facturationAdminService.emettreFacture(request)));
    }

    @PostMapping("/{id}/paiements")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR', 'ACCUEIL')")
    public ResponseEntity<ApiResponse<FactureAdminDto>> enregistrerPaiement(
            @PathVariable Long id,
            @Valid @RequestBody EnregistrerPaiementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Paiement enregistré", facturationAdminService.enregistrerPaiement(id, request)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<FactureStatsDto>> getStats(
            @RequestParam(required = false) LocalDate debut,
            @RequestParam(required = false) LocalDate fin) {
        return ResponseEntity.ok(ApiResponse.success(facturationAdminService.getStats(debut, fin)));
    }
}
