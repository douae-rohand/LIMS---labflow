package com.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Propriétés JWT lues depuis application.yaml / variables d'environnement.
 */
@Getter
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    /** Durée de validité du token d'accès en millisecondes. */
    @Value("${jwt.expiration}")
    private long expiration;

    /** Durée de validité du refresh token en millisecondes. */
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /** Durée de validité du jeton 2FA temporaire en millisecondes (5 min par défaut). */
    @Value("${jwt.two-factor-expiration:300000}")
    private long twoFactorExpiration;
}
