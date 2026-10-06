package com.backend.modules.plateforme.dto;

public record StatutSendGridDto(
        boolean enabled,
        boolean configure,
        String fromEmail,
        String fromName,
        String frontendUrl
) {
}
