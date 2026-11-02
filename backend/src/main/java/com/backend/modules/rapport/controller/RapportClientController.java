package com.backend.modules.rapport.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.ListeAgregeeDto;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.rapport.dto.RapportClientDetailDto;
import com.backend.modules.rapport.dto.RapportClientResumeDto;
import com.backend.modules.rapport.service.RapportClientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mes rapports — espace client. Le laboratoire est porté par le chemin, aucun
 * en-tête X-Tenant-ID n'est requis : l'utilisateur vient du principal.
 */
@RestController
@RequestMapping("/api/client/rapports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client - Rapports", description = "Rapports d'analyses de l'espace client")
public class RapportClientController {

    private final RapportClientService rapportClientService;

    @GetMapping
    public ApiResponse<ListeAgregeeDto<RapportClientResumeDto>> lister() {
        return ApiResponse.success(rapportClientService.listerRapportsClient(
                SecurityUtils.principalCourant().getId()));
    }

    @GetMapping("/{laboratoireId}/{rapportId}")
    public ApiResponse<RapportClientDetailDto> detail(@PathVariable Long laboratoireId,
                                                      @PathVariable Long rapportId) {
        return ApiResponse.success(rapportClientService.obtenirDetailClient(
                SecurityUtils.principalCourant().getId(), laboratoireId, rapportId));
    }
}
