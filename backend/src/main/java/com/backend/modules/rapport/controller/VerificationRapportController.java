package com.backend.modules.rapport.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.rapport.service.RapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Vérification publique d'un rapport signé, ouverte par le QR code imprimé sur le PDF.
 * Aucun résultat d'analyse n'est exposé.
 */
@RestController
@RequestMapping("/api/public/rapports/verification")
@RequiredArgsConstructor
@Tag(name = "Vérification des rapports", description = "Authenticité d'un rapport signé (public)")
public class VerificationRapportController {

    private final RapportService rapportService;

    @GetMapping
    @Operation(summary = "Statut d'un rapport signé (laboratoire, numéro)")
    public ResponseEntity<ApiResponse<RapportService.VerificationPublique>> statut(
            @RequestParam String laboratoire, @RequestParam String numero) {
        return ResponseEntity.ok(ApiResponse.success(rapportService.verifierPublic(laboratoire, numero, null)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Comparer un PDF déposé au rapport signé enregistré")
    public ResponseEntity<ApiResponse<RapportService.VerificationPublique>> verifierFichier(
            @RequestParam String laboratoire,
            @RequestParam String numero,
            @RequestPart("fichier") MultipartFile fichier) {
        if (fichier.isEmpty() || fichier.getSize() > 10 * 1024 * 1024) {
            throw new BusinessRuleException("FICHIER_INVALIDE", "Déposez le PDF du rapport (10 Mo maximum).");
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    rapportService.verifierPublic(laboratoire, numero, fichier.getBytes())));
        } catch (IOException ex) {
            throw new BusinessRuleException("FICHIER_ILLISIBLE", "Le fichier n'a pas pu être lu.");
        }
    }
}
