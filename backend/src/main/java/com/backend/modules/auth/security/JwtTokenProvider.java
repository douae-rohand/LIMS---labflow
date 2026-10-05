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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "access");
        if (userDetails instanceof UtilisateurPrincipal principal) {
            extraClaims.put("uid", principal.getId());
            extraClaims.put("role", principal.getRole().name());
            if (principal.getNomSchema() != null) {
                extraClaims.put("tenantId", principal.getNomSchema());
            }
        }
        return buildToken(userDetails.getUsername(), extraClaims, jwtConfig.getExpiration());
    }

    public String generateRefreshToken(String username) {
        return buildToken(username, Map.of(
                "type", "refresh",
                "jti", java.util.UUID.randomUUID().toString()
        ), jwtConfig.getRefreshExpiration());
    }

    public String generateTwoFactorToken(String username) {
        return buildToken(username, Map.of("type", "2fa"), jwtConfig.getTwoFactorExpiration());
    }

    private String buildToken(String subject, Map<String, Object> extraClaims, long expirationMs) {
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

    public boolean validateToken(String token, String expectedType) {
        if (!validateToken(token)) {
            return false;
        }
        String type = getTypeFromToken(token);
        if (!expectedType.equals(type)) {
            log.warn("Type de jeton JWT incorrect : attendu '{}', recu '{}'", expectedType, type);
            return false;
        }
        return true;
    }

    public String getTypeFromToken(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String getTenantIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class));
    }

    public Long getUidFromToken(String token) {
        Number uid = extractClaim(token, claims -> claims.get("uid", Number.class));
        return uid == null ? null : uid.longValue();
    }

    public Date getExpirationFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseClaims(token));
    }

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
