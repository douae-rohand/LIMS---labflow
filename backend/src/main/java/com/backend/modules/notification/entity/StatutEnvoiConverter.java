package com.backend.modules.notification.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StatutEnvoiConverter implements AttributeConverter<StatutEnvoi, String> {

    @Override
    public String convertToDatabaseColumn(StatutEnvoi attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public StatutEnvoi convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return switch (dbData) {
            case "CREEE", "EN_ATTENTE" -> StatutEnvoi.EN_ATTENTE;
            case "ENVOYEE", "ENVOYE", "LIVRE", "PARTIELLEMENT_ENVOYEE" -> StatutEnvoi.ENVOYE;
            case "ECHEC" -> StatutEnvoi.ECHEC;
            default -> {
                try {
                    yield StatutEnvoi.valueOf(dbData);
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}
