package com.backend.modules.plateforme.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SessionActiveDto {

    private final Long id;
    private final String userAgent;
    private final Instant creeLe;
    private final Instant expireLe;
    private final boolean courante;
}
