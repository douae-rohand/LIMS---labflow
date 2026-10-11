package com.backend.modules.essai.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persiste {@link StatutEssai} en VARCHAR et accepte les alias déjà présents en base.
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
            case "PLANIFIE" -> StatutEssai.PLANIFIEE;
            case "TERMINEE" -> StatutEssai.TERMINE;
            case "ANNULEE" -> StatutEssai.ANNULE;
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
