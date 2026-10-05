package com.backend.config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration du moteur TOTP (RFC 6238) pour la 2FA.
 *
 * <p>Paramètres :
 * <ul>
 *   <li>Algorithme HMAC-SHA1 (défaut RFC 6238 / compatible Google Authenticator)</li>
 *   <li>Fenêtre de tolérance : ±1 période (30 s) pour compenser les décalages d'horloge</li>
 *   <li>Code numérique à 6 chiffres</li>
 * </ul>
 */
@Configuration
public class TotpConfig {

    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30))
                .setWindowSize(3)           // ±1 période (fenêtre de 3 = [-1, 0, +1])
                .setCodeDigits(6)
                .build();
        return new GoogleAuthenticator(config);
    }
}
