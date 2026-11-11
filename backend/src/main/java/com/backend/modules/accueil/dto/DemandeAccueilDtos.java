package com.backend.modules.accueil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Demande d'analyse enregistrée par l'accueil pour un client présent sur site. */
public final class DemandeAccueilDtos {

    private DemandeAccueilDtos() {
    }

    public record CreerDemandeAccueilRequest(
            @NotNull(message = "Choisissez le client") Long clientUtilisateurId,
            Long patientId,
            @NotBlank(message = "L'intitulé est obligatoire") @Size(max = 255) String titre,
            @Size(max = 2000) String objectif,
            @NotEmpty(message = "Choisissez au moins une analyse") List<@NotNull Long> essaiIds) {
    }

    public record DemandeCreeeDto(Long id, String numero, int nombreAnalyses, BigDecimal totalTtc) {
    }

    /** Essai du catalogue proposé à l'accueil : prix TTC, sans paramètres techniques. */
    public record EssaiCatalogueDto(Long id, String code, String designation, String domaineCode,
                                    String domaineLibelle, BigDecimal prix, Integer dureeEstimeeMinutes) {
    }
}
