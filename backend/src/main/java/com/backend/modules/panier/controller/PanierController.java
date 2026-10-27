package com.backend.modules.panier.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.panier.dto.AjouterEssaiRequest;
import com.backend.modules.panier.dto.PanierDetailDto;
import com.backend.modules.panier.dto.PanierResumeDto;
import com.backend.modules.panier.service.PanierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints du panier client.
 *
 * <p>Toutes les routes nécessitent le rôle CLIENT. L'utilisateur est toujours
 * issu du principal ({@link SecurityUtils#principalCourant()}), jamais du corps
 * ni d'un paramètre.
 */
@RestController
@RequestMapping("/api/client/paniers")
@PreAuthorize("hasRole('CLIENT')")
@RequiredArgsConstructor
public class PanierController {

    private final PanierService panierService;

    @GetMapping
    public ApiResponse<List<PanierResumeDto>> lister() {
        Long utilisateurId = SecurityUtils.principalCourant().getId();
        return ApiResponse.success(panierService.listerPaniers(utilisateurId));
    }

    @GetMapping("/{laboratoireId}")
    public ApiResponse<PanierDetailDto> obtenir(@PathVariable Long laboratoireId) {
        Long utilisateurId = SecurityUtils.principalCourant().getId();
        return ApiResponse.success(panierService.obtenirPanier(utilisateurId, laboratoireId));
    }

    @PostMapping("/{laboratoireId}/essais")
    public ApiResponse<PanierDetailDto> ajouter(
            @PathVariable Long laboratoireId,
            @Valid @RequestBody AjouterEssaiRequest request) {
        Long utilisateurId = SecurityUtils.principalCourant().getId();
        return ApiResponse.success(panierService.ajouterEssai(utilisateurId, laboratoireId, request.getEssaiId()));
    }

    @DeleteMapping("/{laboratoireId}/essais/{essaiId}")
    public ApiResponse<PanierDetailDto> retirer(
            @PathVariable Long laboratoireId,
            @PathVariable Long essaiId) {
        Long utilisateurId = SecurityUtils.principalCourant().getId();
        return ApiResponse.success(panierService.retirerEssai(utilisateurId, laboratoireId, essaiId));
    }
}
