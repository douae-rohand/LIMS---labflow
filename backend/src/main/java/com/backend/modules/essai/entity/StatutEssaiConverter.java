package com.backend.modules.essai.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persiste {@link StatutEssai} en VARCHAR et relit les noms déjà écrits en base.
 */
@Converter
public class StatutEssaiConverter implements AttributeConverter<StatutEssai, String> {

    @Override
    public String convertToDatabaseColumn(StatutEssai attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public StatutEssai convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return switch (dbData) {
            case "EN_ATTENTE", "PLANIFIE", "PLANIFIEE", "REPORTEE" -> StatutEssai.ATTENTE_ECHANTILLON;
            case "A_REFAIRE", "EN_COURS" -> StatutEssai.EN_COURS;
            case "TERMINE", "TERMINEE" -> StatutEssai.TERMINE;
            case "ANNULE", "ANNULEE" -> StatutEssai.NON_REALISABLE;
            default -> {
                try {
                    yield StatutEssai.valueOf(dbData);
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}
