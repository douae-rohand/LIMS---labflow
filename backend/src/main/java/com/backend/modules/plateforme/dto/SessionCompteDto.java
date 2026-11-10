package com.backend.modules.plateforme.dto;

import java.time.Instant;

/** Session ouverte (jeton de rafraîchissement valide) d'un compte. Aucun jeton n'est exposé. */
public record SessionCompteDto(Long id, String appareil, Instant ouverture, Instant expiration) {
}
