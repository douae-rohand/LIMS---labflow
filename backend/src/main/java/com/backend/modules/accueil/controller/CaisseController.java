package com.backend.modules.accueil.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.accueil.dto.CaisseDtos.EncaissementDto;
import com.backend.modules.accueil.dto.CaisseDtos.EncaissementRequest;
import com.backend.modules.accueil.dto.CaisseDtos.FactureCaisseDto;
import com.backend.modules.accueil.service.CaisseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Caisse de l'accueil : factures à encaisser, encaissement (partiel possible) et reçus PDF (M11). */
@RestController
@RequestMapping("/api/accueil/caisse")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_FACTURATION_PAYER')")
@Tag(name = "Caisse", description = "Encaissement des factures à l'accueil")
public class CaisseController {

    private final CaisseService caisseService;

    @GetMapping
    @Operation(summary = "Factures du laboratoire avec montants réglés et reste à payer")
    public ResponseEntity<ApiResponse<List<FactureCaisseDto>>> factures() {
        return ResponseEntity.ok(ApiResponse.success(caisseService.lister()));
    }

    @PostMapping("/{factureId}/paiements")
    @Operation(summary = "Encaisser tout ou partie d'une facture")
    public ResponseEntity<ApiResponse<EncaissementDto>> encaisser(@PathVariable Long factureId,
                                                                  @Valid @RequestBody EncaissementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Paiement enregistré", caisseService.encaisser(factureId, request)));
    }

    @GetMapping("/paiements/{paiementId}/recu")
    @Operation(summary = "Reçu de paiement (PDF)")
    public ResponseEntity<byte[]> recu(@PathVariable Long paiementId) {
        CaisseService.FichierRecu recu = caisseService.recu(paiementId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(recu.nom()).build().toString())
                .body(recu.contenu());
    }
}
