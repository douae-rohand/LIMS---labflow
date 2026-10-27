package com.backend.modules.catalogue.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import com.backend.modules.catalogue.dto.AnalyseTypeDto;
import com.backend.modules.catalogue.dto.DomaineCatalogueDto;
import com.backend.modules.catalogue.dto.EnregistrerAnalyseRequest;
import com.backend.modules.catalogue.service.CatalogueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestion du catalogue d'analyses du laboratoire courant.
 * Les lignes viennent du schéma tenant de l'admin ou du responsable connecté,
 * jamais du catalogue d'un autre laboratoire.
 */
@RestController
@RequestMapping("/api/catalogue")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
@Tag(name = "Catalogue", description = "Catalogue des analyses du laboratoire connecté (M04)")
public class CatalogueController {

    private final CatalogueService catalogueService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AnalyseTypeDto>>> lister(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "true") boolean actifSeulement,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                catalogueService.lister(q, statut, actifSeulement, page, size)));
    }

    @GetMapping("/domaines")
    public ResponseEntity<ApiResponse<List<DomaineCatalogueDto>>> domaines() {
        return ResponseEntity.ok(ApiResponse.success(catalogueService.listerDomaines()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<AnalyseTypeDto>> trouverParCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(catalogueService.trouverParCode(code)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnalyseTypeDto>> creer(
            @Valid @RequestBody EnregistrerAnalyseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Analyse créée", catalogueService.creer(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnalyseTypeDto>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody EnregistrerAnalyseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Analyse mise à jour", catalogueService.modifier(id, request)));
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<ApiResponse<AnalyseTypeDto>> activer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Analyse réactivée", catalogueService.activer(id)));
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<ApiResponse<AnalyseTypeDto>> desactiver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Analyse retirée du catalogue", catalogueService.desactiver(id)));
    }
}
