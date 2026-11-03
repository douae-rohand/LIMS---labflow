package com.backend.modules.satisfaction.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.satisfaction.dto.SatisfactionStatsDto;
import com.backend.modules.satisfaction.service.SatisfactionAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/satisfaction")
@RequiredArgsConstructor
@Tag(name = "Satisfaction Admin", description = "Statistiques de satisfaction (M10)")
public class SatisfactionAdminController {

    private final SatisfactionAdminService satisfactionAdminService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<SatisfactionStatsDto>> getStats(
            @RequestParam(required = false) Instant debut,
            @RequestParam(required = false) Instant fin) {
        return ResponseEntity.ok(ApiResponse.success(satisfactionAdminService.getStats(debut, fin)));
    }

    @GetMapping("/enquetes")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<Page<Object[]>>> listerEnquetes(
            @RequestParam(required = false) Instant debut,
            @RequestParam(required = false) Instant fin,
            @RequestParam(required = false) Integer note,
            @RequestParam(required = false) Boolean avecCommentaire,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(satisfactionAdminService.listerEnquetesRepondues(debut, fin, note, avecCommentaire,
                org.springframework.data.domain.PageRequest.of(page, size))));
    }
}
