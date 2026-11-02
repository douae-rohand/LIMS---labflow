package com.backend.modules.satisfaction.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.ListeAgregeeDto;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.satisfaction.dto.EnqueteClientDetailDto;
import com.backend.modules.satisfaction.dto.EnqueteClientResumeDto;
import com.backend.modules.satisfaction.dto.RepondreEnqueteRequest;
import com.backend.modules.satisfaction.service.SatisfactionClientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mes enquêtes de satisfaction — espace client. Le laboratoire est porté par le
 * chemin, aucun en-tête X-Tenant-ID n'est requis : l'utilisateur vient du principal.
 */
@RestController
@RequestMapping("/api/client/enquetes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client - Enquêtes", description = "Enquêtes de satisfaction de l'espace client")
public class EnqueteClientController {

    private final SatisfactionClientService satisfactionClientService;

    @GetMapping
    public ApiResponse<ListeAgregeeDto<EnqueteClientResumeDto>> lister() {
        return ApiResponse.success(satisfactionClientService.listerEnquetesClient(
                SecurityUtils.principalCourant().getId()));
    }

    @GetMapping("/{laboratoireId}/{enqueteId}")
    public ApiResponse<EnqueteClientDetailDto> detail(@PathVariable Long laboratoireId,
                                                      @PathVariable Long enqueteId) {
        return ApiResponse.success(satisfactionClientService.obtenirDetailClient(
                SecurityUtils.principalCourant().getId(), laboratoireId, enqueteId));
    }

    @PostMapping("/{laboratoireId}/{enqueteId}/repondre")
    public ApiResponse<EnqueteClientDetailDto> repondre(@PathVariable Long laboratoireId,
                                                        @PathVariable Long enqueteId,
                                                        @Valid @RequestBody RepondreEnqueteRequest request) {
        return ApiResponse.success("Enquête enregistrée",
                satisfactionClientService.repondre(
                        SecurityUtils.principalCourant().getId(), laboratoireId, enqueteId, request));
    }
}
