package com.backend.modules.accueil.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contrats de l'espace accueil : réception des échantillons (M03) et tableau de bord. */
public final class ReceptionDtos {

    private ReceptionDtos() {
    }

    /** État de réception d'une demande. */
    public enum EtatReception { A_RECEVOIR, PARTIELLE, COMPLETE }

    public record DemandeReceptionDto(
            Long demandeId,
            String reference,
            String titre,
            String client,
            String patient,
            String statut,
            Instant dateDecision,
            Instant derniereReception,
            int analyses,
            int enAttente,
            int recues,
            int nonConformes,
            EtatReception etat) {
    }

    public record ExigenceDto(String nature, String etatConservation, BigDecimal quantiteMinimale, String unite) {
    }

    public record LigneReceptionDto(
            Long id,
            String code,
            String essaiCode,
            String essai,
            String domaine,
            String technicien,
            String statut,
            boolean aRecevoir,
            String echantillonReference,
            Boolean echantillonConforme,
            List<ExigenceDto> exigences) {
    }

    /** Nature attendue parmi les analyses encore à recevoir, avec les analyses concernées. */
    public record NatureAttendueDto(String nature, String etatConservation, String quantite, List<Long> ligneIds) {
    }

    public record EchantillonRecuDto(
            Long id,
            String reference,
            String nature,
            Instant dateReception,
            boolean conforme,
            String motif,
            String etatConservationConstate,
            String receptionneur,
            Long demandeId,
            String demandeReference,
            List<String> analyses) {
    }

    public record DetailReceptionDto(
            DemandeReceptionDto demande,
            List<LigneReceptionDto> lignes,
            List<NatureAttendueDto> naturesAttendues,
            List<EchantillonRecuDto> echantillons) {
    }

    public record EchantillonRecuRequest(
            @NotBlank(message = "Indiquez la nature de l'échantillon")
            @Size(max = 100, message = "La nature ne doit pas dépasser 100 caractères") String nature,
            boolean conforme,
            @Size(max = 1000, message = "Le motif ne doit pas dépasser 1000 caractères") String motif,
            @Size(max = 500, message = "L'état constaté ne doit pas dépasser 500 caractères") String etatConservationConstate,
            @NotEmpty(message = "Choisissez au moins une analyse pour chaque échantillon") List<Long> ligneIds) {
    }

    public record ReceptionRequest(
            @NotEmpty(message = "Ajoutez au moins un échantillon") List<@Valid EchantillonRecuRequest> echantillons) {
    }

    public record TableauAccueilDto(
            long demandesARecevoir,
            long receptionsPartielles,
            long echantillonsRecusAujourdhui,
            long nonConformesAujourdhui,
            long facturesAEncaisser,
            BigDecimal resteAEncaisser,
            List<DemandeReceptionDto> prochainesReceptions,
            List<EchantillonRecuDto> dernieresReceptions) {
    }
}
