package com.backend.modules.demande.dto;

import com.backend.modules.demande.entity.StatutDemande;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DemandeDto {
    private Long id;
    private String reference;
    private String objet;
    private StatutDemande statut;
    private Long clientId;
    private Long laboratoireId;
    private String laboratoireCode;
    private String laboratoireNom;
    private BigDecimal total;
    private Instant dateSoumission;
    private LocalDate dateSouhaitee;
    private String commentaire;
    private Instant dateCreation;
}
