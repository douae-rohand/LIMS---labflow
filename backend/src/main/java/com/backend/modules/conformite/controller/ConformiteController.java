package com.backend.modules.conformite.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.conformite.dto.NonConformiteDto;
import com.backend.modules.conformite.entity.StatutNonConformite;
import com.backend.modules.conformite.service.ConformiteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/conformite") @RequiredArgsConstructor
@Tag(name = "Conformité", description = "Gestion des non-conformités (M13)")
public class ConformiteController {

    private final ConformiteService conformiteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<Page<NonConformiteDto>>> lister(
            @RequestParam(required = false) StatutNonConformite statut,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(conformiteService.lister(statut, PageRequest.of(page, size))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECHNICIEN','RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<NonConformiteDto>> declarer(@RequestBody NonConformiteDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("NC déclarée", conformiteService.declarer(dto)));
    }

    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<NonConformiteDto>> traiter(
            @PathVariable Long id, @RequestParam String actionCorrective, @RequestParam Long responsableId) {
        return ResponseEntity.ok(ApiResponse.success("NC en traitement",
                conformiteService.traiter(id, actionCorrective, responsableId)));
    }

    @PatchMapping("/{id}/cloturer")
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<NonConformiteDto>> cloturer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("NC clôturée", conformiteService.cloturer(id)));
    }
}
