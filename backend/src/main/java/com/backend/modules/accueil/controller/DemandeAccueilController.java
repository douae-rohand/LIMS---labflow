package com.backend.modules.accueil.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.accueil.dto.DemandeAccueilDtos.CreerDemandeAccueilRequest;
import com.backend.modules.accueil.dto.DemandeAccueilDtos.DemandeCreeeDto;
import com.backend.modules.accueil.dto.DemandeAccueilDtos.EssaiCatalogueDto;
import com.backend.modules.demande.service.DemandeService;
import com.backend.modules.essai.repository.EssaiRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/** Demande d'analyse enregistrée au guichet pour un client présent sur site. */
@RestController
@RequestMapping("/api/accueil")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ACCUEIL')")
@Tag(name = "Accueil — Demande sur site", description = "Création d'une demande pour un client présent")
public class DemandeAccueilController {

    private final DemandeService demandeService;
    private final EssaiRepository essaiRepository;

    @GetMapping("/catalogue")
    @Transactional(readOnly = true)
    @Operation(summary = "Essais actifs du laboratoire, avec leur prix TTC")
    public ResponseEntity<ApiResponse<List<EssaiCatalogueDto>>> catalogue() {
        List<EssaiCatalogueDto> essais = essaiRepository.findActifsAvecDomaine().stream()
                .filter(e -> e.getTarif() != null)
                .map(e -> new EssaiCatalogueDto(e.getId(), e.getCode(), e.getDesignation(),
                        e.getDomaine() == null ? null : e.getDomaine().getCode(),
                        e.getDomaine() == null ? null : e.getDomaine().getLibelle(),
                        e.getTarif(), e.getDureeEstimee()))
                .sorted(Comparator.comparing((EssaiCatalogueDto e) -> e.domaineLibelle() == null ? "" : e.domaineLibelle())
                        .thenComparing(EssaiCatalogueDto::designation))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(essais));
    }

    @PostMapping("/demandes")
    @Operation(summary = "Enregistrer une demande pour un client présent à l'accueil")
    public ResponseEntity<ApiResponse<DemandeCreeeDto>> creer(@Valid @RequestBody CreerDemandeAccueilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demande enregistrée", demandeService.creerParAccueil(request)));
    }
}
