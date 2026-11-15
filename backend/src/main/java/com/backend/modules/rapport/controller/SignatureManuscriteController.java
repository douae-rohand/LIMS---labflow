package com.backend.modules.rapport.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.rapport.signature.SignatureManuscriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Signature manuscrite du responsable connecté, imprimée sur les rapports qu'il signe. */
@RestController
@RequestMapping("/api/responsable/signature")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESPONSABLE')")
@Tag(name = "Signature manuscrite", description = "Image de signature du responsable")
public class SignatureManuscriteController {

    private final SignatureManuscriteService service;

    @GetMapping("/statut")
    @Operation(summary = "Une signature a-t-elle été importée ?")
    public ResponseEntity<ApiResponse<SignatureManuscriteService.Statut>> statut() {
        return ResponseEntity.ok(ApiResponse.success(service.statut(SecurityUtils.principalCourant().getId())));
    }

    @GetMapping
    @Operation(summary = "Image de la signature (PNG)")
    public ResponseEntity<byte[]> image() {
        byte[] png = service.lire(SecurityUtils.principalCourant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aucune signature importée"));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .body(png);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer ou remplacer l'image de signature (PNG ou JPG, 2 Mo max)")
    public ResponseEntity<ApiResponse<SignatureManuscriteService.Statut>> importer(
            @RequestPart("fichier") MultipartFile fichier) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        return ResponseEntity.ok(ApiResponse.success("Signature enregistrée",
                service.enregistrer(acteur.getId(), acteur.getEmail(), acteur.getLaboratoireId(), fichier)));
    }

    @DeleteMapping
    @Operation(summary = "Supprimer l'image de signature")
    public ResponseEntity<ApiResponse<SignatureManuscriteService.Statut>> supprimer() {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        return ResponseEntity.ok(ApiResponse.success("Signature supprimée",
                service.supprimer(acteur.getId(), acteur.getEmail(), acteur.getLaboratoireId())));
    }
}
