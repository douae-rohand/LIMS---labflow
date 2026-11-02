package com.backend.modules.rapport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ligne d'un rapport côté client.
 *
 * <p>La table {@code rapport_ligne} ne porte que la liaison
 * {@code (rapport_id, ligne_essai_id)} : la désignation et l'unité sont lues
 * sur {@code essai}, la valeur et la conformité sur {@code ligne_essai}.
 * Les limites de référence et seuils OOS/OOT ne sont jamais exposés.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportLigneClientDto {

    private Long ligneEssaiId;
    private String designation;
    private String valeur;
    private String unite;
    private Boolean conformite;
}
