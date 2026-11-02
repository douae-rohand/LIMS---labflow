package com.backend.modules.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Synthèse du tableau de bord de l'espace client.
 *
 * <p>Les compteurs de demandes restent calculés côté frontend depuis la liste
 * existante ; seuls les indicateurs ci-dessous sont fournis par le serveur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyntheseClientDto {

    /** Nombre de rapports visibles du client. */
    private int rapportsDisponibles;

    /** Nombre de factures au statut recalculé {@code IMPAYEE}. */
    private int facturesImpayees;

    /** Somme des restes à payer des factures impayées. */
    private BigDecimal montantRestantDu;

    /** Nombre d'enquêtes de satisfaction non encore répondues. */
    private int enquetesEnAttente;

    /** Nombre de laboratoires du client dont la lecture a échoué. */
    private int laboratoiresIndisponibles;
}
