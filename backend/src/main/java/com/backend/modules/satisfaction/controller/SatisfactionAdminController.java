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
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/satisfaction")
@RequiredArgsConstructor
@Tag(name = "Satisfaction Admin", description = "Statistiques de satisfaction (M10)")
public class SatisfactionAdminController {

    private final SatisfactionAdminService satisfactionAdminService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_SATISFACTION_CONSULTER')")
    public ResponseEntity<ApiResponse<SatisfactionStatsDto>> getStats(
            @RequestParam(required = false) String debut,
            @RequestParam(required = false) String fin) {
        Instant debutInstant = null;
        Instant finInstant = null;
        if (debut != null) {
            debutInstant = LocalDate.parse(debut).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        if (fin != null) {
            finInstant = LocalDate.parse(fin).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        }
        return ResponseEntity.ok(ApiResponse.success(satisfactionAdminService.getStats(debutInstant, finInstant)));
    }

    @GetMapping("/enquetes")
    @PreAuthorize("hasAuthority('PERM_SATISFACTION_CONSULTER')")
    public ResponseEntity<ApiResponse<Page<Object[]>>> listerEnquetes(
            @RequestParam(required = false) String debut,
            @RequestParam(required = false) String fin,
            @RequestParam(required = false) Integer note,
            @RequestParam(required = false) Boolean avecCommentaire,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Instant debutInstant = null;
        Instant finInstant = null;
        if (debut != null) {
            debutInstant = LocalDate.parse(debut).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        if (fin != null) {
            finInstant = LocalDate.parse(fin).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        }
        return ResponseEntity.ok(ApiResponse.success(satisfactionAdminService.listerEnquetesRepondues(debutInstant, finInstant, note, avecCommentaire,
                org.springframework.data.domain.PageRequest.of(page, size))));
    }
}
