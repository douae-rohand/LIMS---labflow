package com.backend.modules.auth.service;

import com.backend.config.JwtConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Service de gestion du cookie HttpOnly pour le Refresh Token.
 */
@Service
@RequiredArgsConstructor
public class RefreshCookieService {

    private final JwtConfig jwtConfig;

    @Value("${jwt.cookie-name:refreshToken}")
    private String cookieName;

    @Value("${jwt.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.cookie-same-site:Strict}")
    private String cookieSameSite;

    @Value("${jwt.cookie-path:/api/auth}")
    private String cookiePath;

    /**
     * Construit un {@link ResponseCookie} HttpOnly contenant le refresh token.
     *
     * @param rawRefreshToken jeton JWT brut de rafraîchissement
     * @return ResponseCookie configuré
     */
    public ResponseCookie creerCookieRefreshToken(String rawRefreshToken) {
        long maxAgeSeconds = jwtConfig.getRefreshExpiration() / 1000;
        return ResponseCookie.from(cookieName, rawRefreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(maxAgeSeconds)
                .sameSite(cookieSameSite)
                .build();
    }

    /**
     * Construit un {@link ResponseCookie} d'effacement (Max-Age=0).
     *
     * @return ResponseCookie réinitialisé
     */
    public ResponseCookie effacerCookieRefreshToken() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(cookiePath)
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
    }

    /**
     * Extrait la valeur du refresh token depuis les cookies de la requête HTTP.
     *
     * @param request requête HTTP
     * @return Optional contenant la valeur brute du refresh token si présent
     */
    public Optional<String> extraireRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
