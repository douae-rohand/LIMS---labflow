package com.backend.modules.demande.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.demande.dto.AnnulerDemandeRequest;
import com.backend.modules.demande.dto.CreerDemandeRequest;
import com.backend.modules.demande.dto.DemandeDetailClientDto;
import com.backend.modules.demande.service.DemandeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints client pour la gestion des demandes.
 *
 * <p>Toutes les routes nécessitent le rôle CLIENT. L'utilisateur est toujours
 * issu du principal, jamais du corps ni d'un paramètre. Aucun en-tête
 * {@code X-Tenant-ID} n'est requis : le laboratoire est identifié par le
 * paramètre de chemin.
 */
@RestController
@RequestMapping("/api/client/demandes")
@PreAuthorize("hasRole('CLIENT')")
@RequiredArgsConstructor
public class DemandeClientController {

    private final DemandeService demandeService;

    /**
     * Crée une demande depuis le panier central du client pour le laboratoire cible.
     */
    @PostMapping("/{laboratoireId}")
    public ApiResponse<DemandeDetailClientDto> creer(
            @PathVariable Long laboratoireId,
            @Valid @RequestBody CreerDemandeRequest request) {
        Long utilisateurId = SecurityUtils.principalCourant().getId();
        return ApiResponse.success(demandeService.creerDepuisPanier(
                utilisateurId, laboratoireId, request.getTitre(), request.getObjectif()));
    }

    /**
     * Annule une demande au statut NOUVEAU.
     */
    @PostMapping("/{laboratoireId}/{demandeId}/annuler")
    public ApiResponse<DemandeDetailClientDto> annuler(
            @PathVariable Long laboratoireId,
            @PathVariable Long demandeId,
            @Valid @RequestBody AnnulerDemandeRequest request) {
        Long utilisateurId = SecurityUtils.principalCourant().getId();
        return ApiResponse.success(demandeService.annuler(
                utilisateurId, laboratoireId, demandeId, request.getMotif()));
    }

    /**
     * Détail enrichi d'une demande du client.
     */
    @GetMapping("/{laboratoireId}/{demandeId}")
    public ApiResponse<DemandeDetailClientDto> detail(
            @PathVariable Long laboratoireId,
            @PathVariable Long demandeId) {
        Long utilisateurId = SecurityUtils.principalCourant().getId();
        return ApiResponse.success(demandeService.obtenirDetailClient(
                utilisateurId, laboratoireId, demandeId));
    }
}
