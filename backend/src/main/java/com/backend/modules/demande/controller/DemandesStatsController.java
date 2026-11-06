package com.backend.modules.demande.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.demande.dto.DemandesStatsDto;
import com.backend.modules.demande.service.DemandesStatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/demandes/stats")
@RequiredArgsConstructor
@Tag(name = "Stats Demandes", description = "Statistiques des demandes (M11)")
public class DemandesStatsController {

    private final DemandesStatsService demandesStatsService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_INDICATEURS_CONSULTER')")
    public ResponseEntity<ApiResponse<DemandesStatsDto>> getStats(
            @RequestParam(required = false) LocalDate debut,
            @RequestParam(required = false) LocalDate fin) {
        return ResponseEntity.ok(ApiResponse.success(demandesStatsService.getStats(debut, fin)));
    }
}
