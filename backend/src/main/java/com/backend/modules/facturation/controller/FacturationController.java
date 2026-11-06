package com.backend.modules.facturation.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.facturation.dto.FactureDto;
import com.backend.modules.facturation.service.FacturationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
@Tag(name = "Facturation", description = "Gestion de la facturation (M11)")
public class FacturationController {

    private final FacturationService facturationService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FactureDto>> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(facturationService.trouverParId(id)));
    }

    @GetMapping("/mes")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<Page<FactureDto>>> listerMesFactures(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                facturationService.listerMesFactures(PageRequest.of(page, size))));
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('PERM_FACTURATION_CONSULTER')")
    public ResponseEntity<ApiResponse<Page<FactureDto>>> listerParClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                facturationService.listerParClient(clientId, PageRequest.of(page, size))));
    }
}
