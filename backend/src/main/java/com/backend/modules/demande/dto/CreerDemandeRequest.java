package com.backend.modules.demande.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Corps de {@code POST /api/client/demandes/{laboratoireId}}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreerDemandeRequest {

    @NotBlank(message = "Le titre de la demande est obligatoire.")
    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères.")
    private String titre;

    private String objectif;

    private List<PieceJointeRequest> piecesJointes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PieceJointeRequest {
        @NotBlank(message = "Le nom du fichier est obligatoire")
        @Size(max = 255, message = "Le nom du fichier ne peut pas dépasser 255 caractères")
        private String nomFichier;

        @NotBlank(message = "Le type MIME est obligatoire")
        @Size(max = 100, message = "Le type MIME ne peut pas dépasser 100 caractères")
        private String typeMime;

        private String contenu; // Base64 encoded
    }
}
