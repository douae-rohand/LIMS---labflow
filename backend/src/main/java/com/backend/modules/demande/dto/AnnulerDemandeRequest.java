package com.backend.modules.demande.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Corps de {@code POST /api/client/demandes/{laboratoireId}/{demandeId}/annuler}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnulerDemandeRequest {

    @NotBlank(message = "Le motif d'annulation est obligatoire.")
    @Size(min = 5, max = 2000, message = "Le motif doit contenir entre 5 et 2000 caractères.")
    private String motif;
}
