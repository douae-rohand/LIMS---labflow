package com.backend.modules.facturation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Facture émise pour une demande d'analyse (M11).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "facture")
public class Facture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", unique = true, nullable = false, length = 50)
    private String numero;

    @Column(name = "demande_id", nullable = false)
    private Long demandeId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutFacture statut = StatutFacture.BROUILLON;

    @Column(name = "montant_ht", nullable = false, precision = 14, scale = 2)
    private BigDecimal montantHt;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxTva = BigDecimal.valueOf(20);

    @Column(name = "montant_ttc", nullable = false, precision = 14, scale = 2)
    private BigDecimal montantTtc;

    @Column(name = "date_emission")
    private LocalDate dateEmission;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "date_paiement")
    private Instant datePaiement;

    @Column(name = "fichier_url", length = 500)
    private String fichierUrl;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
