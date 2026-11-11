package com.backend.modules.accueil.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contrats de la caisse de l'accueil (M11 : encaissement et reçus). */
public final class CaisseDtos {

    private CaisseDtos() {
    }

    public record PaiementCaisseDto(
            Long id,
            String code,
            Instant date,
            BigDecimal montant,
            String mode,
            String reference,
            String encaisseur,
            /** Écriture d'annulation (montant négatif) d'un encaissement antérieur. */
            boolean contrePassation,
            /** Encaissement déjà annulé par une contre-passation. */
            boolean annule,
            /** Annulable : encaissement du jour, positif, pas encore annulé. */
            boolean annulable) {
    }

    public record AnnulationRequest(
            @jakarta.validation.constraints.NotBlank(message = "Indiquez le motif de l'annulation")
            @Size(min = 5, max = 200, message = "Le motif doit contenir entre 5 et 200 caractères") String motif) {
    }

    /** Une ligne du journal de caisse. */
    public record LigneJournal(Long paiementId, String code, Instant date, BigDecimal montant, String mode,
                               String facture, String client, String encaisseur, boolean contrePassation,
                               String reference) {
    }

    /** Total par mode de paiement (contre-passations déduites). */
    public record TotalMode(String mode, long operations, BigDecimal montant) {
    }

    public record JournalCaisse(java.time.LocalDate jour, List<LigneJournal> lignes, List<TotalMode> totaux,
                                BigDecimal total, long annulations) {
    }

    public record FactureCaisseDto(
            Long id,
            String numero,
            LocalDate dateFacture,
            LocalDate dateEcheance,
            BigDecimal montant,
            BigDecimal paye,
            BigDecimal reste,
            String statut,
            boolean enRetard,
            Long demandeId,
            String demandeReference,
            String demandeTitre,
            String client,
            String clientIce,
            Instant dernierPaiement,
            List<PaiementCaisseDto> paiements) {
    }

    public record EncaissementRequest(
            @NotNull(message = "Indiquez le montant encaissé")
            @DecimalMin(value = "0.01", message = "Le montant doit être positif")
            @Digits(integer = 10, fraction = 2, message = "Montant invalide (2 décimales au plus)") BigDecimal montant,
            @NotBlank(message = "Choisissez le mode de paiement")
            @Pattern(regexp = "ESPECES|CHEQUE|CARTE|VIREMENT", message = "Mode de paiement invalide") String mode,
            @Size(max = 255, message = "La référence ne doit pas dépasser 255 caractères") String reference) {
    }

    public record EncaissementDto(FactureCaisseDto facture, PaiementCaisseDto paiement) {
    }
}
