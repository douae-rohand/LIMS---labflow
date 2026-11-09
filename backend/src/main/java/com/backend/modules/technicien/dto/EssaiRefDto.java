package com.backend.modules.technicien.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Informations de l'essai catalogue visibles par le technicien.
 * ❌ tarif non exposé.
 */
@Value
@Builder
public class EssaiRefDto {
    String code;
    String designation;
    String methode;
    /** Unité de mesure du résultat (ex. "g/L"). */
    String unite;
    /** Durée estimée en minutes. */
    Integer dureeEstimee;
    DomaineRefDto domaine;
}
