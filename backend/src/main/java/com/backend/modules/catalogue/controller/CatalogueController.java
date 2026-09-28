package com.backend.modules.catalogue.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.catalogue.dto.AnalyseTypeDto;
import com.backend.modules.catalogue.service.CatalogueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogue")
@RequiredArgsConstructor
@Tag(name = "Catalogue", description = "Catalogue des types d'analyses (M04)")
public class CatalogueController {

    private final CatalogueService catalogueService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AnalyseTypeDto>>> lister(
            @RequestParam(defaultValue = "true") boolean actifSeulement,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                catalogueService.lister(actifSeulement, PageRequest.of(page, size))));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<AnalyseTypeDto>> trouverParCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(catalogueService.trouverParCode(code)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<AnalyseTypeDto>> creer(@RequestBody AnalyseTypeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Analyse créée", catalogueService.creer(dto)));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<AnalyseTypeDto>> desactiver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Désactivé", catalogueService.desactiver(id)));
    }
}
