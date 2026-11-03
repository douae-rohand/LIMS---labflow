package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mémorise l'empreinte des jetons 2FA déjà utilisés pour une validation ou une activation réussie.
 * La trace disparaît après la durée de vie du jeton. Le jeton brut n'est pas conservé.
 */
@Service
public class JetonDeuxFacteursConsommation {

    private static final Duration DUREE = Duration.ofMinutes(10);

    private final Map<String, Instant> consommes = new ConcurrentHashMap<>();

    public void verifierDisponible(String token) {
        nettoyer();
        Instant jusqua = consommes.get(RefreshTokenService.hash(token));
        if (jusqua != null && Instant.now().isBefore(jusqua)) {
            throw new BusinessRuleException("2FA_TOKEN_INVALID", "Token 2FA invalide ou expiré");
        }
    }

    public void consommer(String token) {
        consommes.put(RefreshTokenService.hash(token), Instant.now().plus(DUREE));
    }

    private void nettoyer() {
        Instant maintenant = Instant.now();
        consommes.entrySet().removeIf(entree -> !maintenant.isBefore(entree.getValue()));
    }
}
