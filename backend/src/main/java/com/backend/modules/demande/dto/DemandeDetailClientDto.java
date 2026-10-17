package com.backend.modules.demande.dto;

import com.backend.modules.demande.entity.StatutDemande;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Détail enrichi d'une demande pour le client.
 * N'expose jamais de limites de référence, seuils, identifiants d'utilisateurs
 * ni de commentaires internes du laboratoire.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeDetailClientDto {

    private Long id;
    private String reference;
    private String titre;
    private String objectif;
    private StatutDemande statut;
    private Instant dateSoumission;
    private String motif;
    private String laboratoireNom;
    private List<LigneEssaiClientDto> lignes;
    private BigDecimal total;
    private List<EchantillonClientDto> echantillons;
    private List<HistoriqueEntreeDto> historique;
}
