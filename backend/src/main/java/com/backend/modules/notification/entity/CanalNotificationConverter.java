package com.backend.modules.notification.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CanalNotificationConverter implements AttributeConverter<CanalNotification, String> {

    @Override
    public String convertToDatabaseColumn(CanalNotification attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public CanalNotification convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return switch (dbData) {
            case "WEB_PUSH", "PUSH" -> CanalNotification.PUSH;
            case "N8N_WEBHOOK", "EMAIL" -> CanalNotification.EMAIL;
            case "IN_APP" -> CanalNotification.IN_APP;
            default -> {
                try {
                    yield CanalNotification.valueOf(dbData);
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}
