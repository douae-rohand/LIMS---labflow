package com.backend.modules.plateforme.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.plateforme.service.TvaService;
import com.backend.modules.plateforme.service.TvaService.TvaLaboratoire;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** TVA du laboratoire : visible et modifiable uniquement par l'Administrateur du laboratoire. */
@RestController
@RequestMapping("/api/laboratoire/tva")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRATEUR')")
@Tag(name = "TVA laboratoire", description = "Taux de TVA appliqué aux prix TTC du laboratoire")
public class TvaLaboratoireController {

    private final TvaService tvaService;

    /** {@code tauxTva} à {@code null} : le laboratoire suit le taux de la plateforme. */
    public record ModifierTvaLaboratoire(
            @DecimalMin(value = "0.00", message = "Le taux de TVA ne peut pas être négatif")
            @DecimalMax(value = "100.00", message = "Le taux de TVA ne peut pas dépasser 100 %")
            BigDecimal tauxTva) {
    }

    @GetMapping
    @Operation(summary = "Taux de TVA du laboratoire (propre, plateforme, effectif)")
    public ResponseEntity<ApiResponse<TvaLaboratoire>> lire(@AuthenticationPrincipal UtilisateurPrincipal moi) {
        return ResponseEntity.ok(ApiResponse.success(tvaService.pourLaboratoire(laboratoire(moi))));
    }

    @PutMapping
    @Operation(summary = "Définir le taux propre au laboratoire, ou revenir au taux de la plateforme")
    public ResponseEntity<ApiResponse<TvaLaboratoire>> modifier(
            @AuthenticationPrincipal UtilisateurPrincipal moi,
            @Valid @RequestBody ModifierTvaLaboratoire demande) {
        return ResponseEntity.ok(ApiResponse.success("Taux de TVA enregistré",
                tvaService.modifierTauxLaboratoire(laboratoire(moi), demande.tauxTva(), moi.getEmail())));
    }

    private static Long laboratoire(UtilisateurPrincipal moi) {
        if (moi == null || moi.getLaboratoireId() == null) {
            throw new BusinessRuleException("ADMIN_SANS_LABORATOIRE", "Administrateur sans laboratoire rattaché");
        }
        return moi.getLaboratoireId();
    }
}
