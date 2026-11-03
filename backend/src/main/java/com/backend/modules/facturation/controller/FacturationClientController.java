package com.backend.modules.facturation.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.ListeAgregeeDto;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.facturation.dto.FactureClientDetailDto;
import com.backend.modules.facturation.dto.FactureClientResumeDto;
import com.backend.modules.facturation.service.FacturationClientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mes factures — espace client. Lecture seule, aucune obligation d'en-tête
 * X-Tenant-ID : le laboratoire est porté par le chemin, l'utilisateur par le principal.
 */
@RestController
@RequestMapping("/api/client/factures")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client - Factures", description = "Factures et paiements de l'espace client")
public class FacturationClientController {

    private final FacturationClientService facturationClientService;

    @GetMapping
    public ApiResponse<ListeAgregeeDto<FactureClientResumeDto>> lister() {
        return ApiResponse.success(facturationClientService.listerFacturesClient(
                SecurityUtils.principalCourant().getId()));
    }

    @GetMapping("/{laboratoireId}/{factureId}")
    public ApiResponse<FactureClientDetailDto> detail(@PathVariable Long laboratoireId,
                                                      @PathVariable Long factureId) {
        return ApiResponse.success(facturationClientService.obtenirDetailClient(
                SecurityUtils.principalCourant().getId(), laboratoireId, factureId));
    }
}
