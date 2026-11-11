package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.integration.sendgrid.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Double authentification par e-mail : à chaque connexion d'un rôle soumis à la 2FA, un code à
 * 6 chiffres valable 15 minutes est envoyé à l'adresse du compte.
 *
 * <p>Le code n'est jamais conservé en clair : seule son empreinte HMAC-SHA256 (clé aléatoire tirée
 * au démarrage) est gardée en mémoire, avec son échéance. Un code sert une seule fois ; un nouvel
 * envoi remplace le précédent. Les codes en attente sont perdus au redémarrage du serveur : il
 * suffit alors de se reconnecter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeConnexionService {

    public static final int DUREE_MINUTES = 15;
    private static final Duration DUREE = Duration.ofMinutes(DUREE_MINUTES);
    /** Délai minimal entre deux envois pour un même compte. */
    private static final Duration DELAI_RENVOI = Duration.ofSeconds(60);
    private static final SecureRandom ALEA = new SecureRandom();
    private static final byte[] CLE = cleAleatoire();

    private final EmailService emailService;

    private record CodeEnAttente(byte[] empreinte, Instant expiration, Instant envoi) {
    }

    private final Map<String, CodeEnAttente> codes = new ConcurrentHashMap<>();

    /** Génère un nouveau code, remplace le précédent et l'envoie par e-mail. */
    public void envoyer(String email) {
        String cle = cle(email);
        String code = String.format(Locale.ROOT, "%06d", ALEA.nextInt(1_000_000));
        Instant maintenant = Instant.now();
        codes.put(cle, new CodeEnAttente(empreinte(cle, code), maintenant.plus(DUREE), maintenant));
        emailService.envoyerCodeOtp(email, code, DUREE_MINUTES);
        log.debug("Code de connexion envoyé pour un compte (expire dans {} min)", DUREE_MINUTES);
    }

    /** Nouvel envoi demandé par l'utilisateur, au plus une fois par minute. */
    public void renvoyer(String email) {
        CodeEnAttente actuel = codes.get(cle(email));
        if (actuel != null && actuel.envoi().plus(DELAI_RENVOI).isAfter(Instant.now())) {
            long secondes = Duration.between(Instant.now(), actuel.envoi().plus(DELAI_RENVOI)).toSeconds() + 1;
            throw new BusinessRuleException("2FA_RENVOI_TROP_TOT",
                    "Patientez " + secondes + " secondes avant de demander un nouveau code.");
        }
        envoyer(email);
    }

    /** Vrai si le code correspond au dernier envoyé et n'a pas expiré ; il est alors consommé. */
    public boolean verifier(String email, String code) {
        String cle = cle(email);
        CodeEnAttente attendu = codes.get(cle);
        if (attendu == null || code == null) {
            return false;
        }
        if (attendu.expiration().isBefore(Instant.now())) {
            codes.remove(cle, attendu);
            return false;
        }
        boolean correct = MessageDigest.isEqual(attendu.empreinte(), empreinte(cle, code.trim()));
        if (correct) {
            codes.remove(cle, attendu);
        }
        return correct;
    }

    /** Purge des codes expirés, pour ne pas garder en mémoire les connexions abandonnées. */
    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    public void purger() {
        Instant maintenant = Instant.now();
        codes.entrySet().removeIf(e -> e.getValue().expiration().isBefore(maintenant));
    }

    private static String cle(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] empreinte(String cle, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CLE, "HmacSHA256"));
            return mac.doFinal((cle + ":" + code).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", ex);
        }
    }

    private static byte[] cleAleatoire() {
        byte[] cle = new byte[32];
        ALEA.nextBytes(cle);
        return cle;
    }
}
