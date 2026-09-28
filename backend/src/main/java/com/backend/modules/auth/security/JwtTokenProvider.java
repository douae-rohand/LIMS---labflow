package com.backend.modules.auth.security;

import com.backend.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Fournisseur JWT : génération, validation et extraction des claims.
 *
 * <p>Utilise HMAC-SHA512 avec la clé définie dans {@link JwtConfig}.
 * Le claim {@code tenantId} est inclus dans chaque token d'accès
 * pour permettre le routage multi-tenant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    // -------------------------------------------------------------------------
    // Génération
    // -------------------------------------------------------------------------

    /**
     * Génère un token d'accès JWT pour l'utilisateur authentifié.
     *
     * @param authentication le contexte d'authentification Spring Security
     * @param tenantId       l'identifiant du tenant (schéma MySQL)
     */
    public String generateAccessToken(Authentication authentication, String tenantId) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildToken(userDetails.getUsername(), tenantId, jwtConfig.getExpiration());
    }

    /**
     * Génère un refresh token (sans claim tenantId).
     */
    public String generateRefreshToken(String username) {
        return buildToken(username, null, jwtConfig.getRefreshExpiration());
    }

    private String buildToken(String subject, String tenantId, long expirationMs) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (tenantId != null) {
            extraClaims.put("tenantId", tenantId);
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            log.warn("Signature JWT invalide : {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expiré : {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Token JWT non supporté : {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Claims JWT vides : {}", ex.getMessage());
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Extraction des claims
    // -------------------------------------------------------------------------

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String getTenantIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class));
    }

    public Date getExpirationFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
