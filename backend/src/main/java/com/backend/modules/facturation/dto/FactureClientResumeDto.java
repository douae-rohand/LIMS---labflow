package com.backend.modules.facturation.dto;

import com.backend.modules.facturation.entity.StatutFacture;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Résumé d'une facture pour la liste agrégée {@code GET /api/client/factures}.
 *
 * <p>{@code statut} est <b>recalculé côté serveur</b> à partir des paiements :
 * {@code PAYEE} si la somme des paiements {@code >= montant_ht}, sinon
 * {@code IMPAYEE}. Le statut stocké en base n'est jamais renvoyé tel quel.
 *
 * <p>{@code montantTotal} correspond au montant HT ({@code facture.montant_ht}) :
 * aucune colonne TVA n'existe, il ne s'agit donc pas d'un TTC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactureClientResumeDto {

    private Long factureId;
    private Long laboratoireId;
    private String laboratoireNom;
    private String numero;
    private String demandeReference;
    private String demandeTitre;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;

    /** Montant HT (aucune TVA en base). */
    private BigDecimal montantTotal;

    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;

    /** Statut recalculé serveur : {@code PAYEE} ou {@code IMPAYEE} uniquement. */
    private StatutFacture statut;
}
