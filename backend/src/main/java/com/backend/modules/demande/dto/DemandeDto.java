package com.backend.modules.demande.dto;

import com.backend.modules.demande.entity.StatutDemande;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DemandeDto {
    private Long id;
    private String reference;
    private String objet;
    private StatutDemande statut;
    private Long clientId;
    private Instant dateSoumission;
    private LocalDate dateSouhaitee;
    private String commentaire;
    private Instant dateCreation;
}
