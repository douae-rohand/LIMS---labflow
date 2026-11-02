package com.backend.modules.facturation.dto;

import com.backend.modules.facturation.entity.StatutFacture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Détail d'une facture côté client : en-tête, lignes et paiements.
 *
 * <p>{@code montantHt} porte explicitement le montant HT (aucune colonne TVA en
 * base). {@code statut} est recalculé serveur : {@code PAYEE} ou {@code IMPAYEE}.
 * {@code ice} provient de la fiche client du laboratoire (mentions juridiques
 * marocaines) ; il est {@code null} si non renseigné.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureClientDetailDto {

    private Long factureId;
    private Long laboratoireId;
    private String laboratoireNom;
    private String numero;
    private String type;
    private Long demandeId;
    private String demandeReference;
    private String demandeTitre;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private BigDecimal montantHt;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;

    /** Statut recalculé serveur : {@code PAYEE} ou {@code IMPAYEE} uniquement. */
    private StatutFacture statut;

    /** ICE du client au sein du laboratoire, si renseigné. */
    private String ice;

    private List<FactureLigneClientDto> lignes;
    private List<PaiementClientDto> paiements;
}
