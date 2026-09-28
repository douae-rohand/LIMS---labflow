package com.backend.modules.ia.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyseIaRequest {
    /** Contexte ou données à analyser (texte brut, JSON, etc.). */
    private String contexte;
    /** Type de tâche IA demandée. */
    private TypeTacheIa typeTache;
    /** Paramètres supplémentaires (JSON). */
    private String parametres;
}
