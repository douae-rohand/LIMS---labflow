package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.auth.entity.TokenActivation;
import com.backend.modules.auth.repository.TokenActivationRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Gestion des jetons d'activation de compte client (CLI-01 et CLI-02).
 *
 * <p>Sécurité :
 * <ul>
 *   <li>Le jeton brut est 32 octets de {@link SecureRandom} encodés en Base64-URL
 *       (43 caractères, 256 bits d'entropie).</li>
 *   <li>Seul le SHA-256 du jeton est stocké en base — une compromission de la base
 *       ne permet pas de rejouer un lien d'activation.</li>
 *   <li>L'invalidation se fait par un {@code UPDATE ... WHERE utilise = false AND date_expiration > now}
 *       dont on contrôle le nombre de lignes affectées.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationService {

    private final TokenActivationRepository tokenActivationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ActivationEmailService activationEmailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.inscription.token-expiration-heures:24}")
    private int tokenExpirationHeures;

    // -------------------------------------------------------------------------
    // Génération
    // -------------------------------------------------------------------------

    /**
     * Génère un jeton d'activation pour l'utilisateur donné, révoque les anciens
     * jetons non utilisés et persiste le nouveau hash.
     *
     * @return la valeur brute du jeton (à envoyer par email, jamais stockée)
     */
    @Transactional
    public String genererJeton(Utilisateur utilisateur) {
        // Révoquer les anciens jetons non utilisés (cas de ré-inscription / renvoi)
        int nb = tokenActivationRepository.revoquerTousParUtilisateur(utilisateur.getId());
        if (nb > 0) {
            log.debug("Activation : {} jeton(s) précédent(s) révoqué(s) pour userId={}",
                    nb, utilisateur.getId());
        }

        // Générer 32 octets aléatoires
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String tokenBrut = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = sha256(tokenBrut);

        tokenActivationRepository.save(TokenActivation.builder()
                .tokenHash(tokenHash)
                .utilisateur(utilisateur)
                .dateExpiration(Instant.now().plusSeconds(tokenExpirationHeures * 3600L))
                .build());

        return tokenBrut;
    }

    // -------------------------------------------------------------------------
    // Renvoi du lien d'activation (CLI-02)
    // -------------------------------------------------------------------------

    /**
     * Traite le renvoi du lien d'activation du compte.
     *
     * <p>Si et seulement si un utilisateur existe avec {@code compteConfirme == false} :
     * <ul>
     *   <li>Révocation des anciens jetons + création du nouveau jeton dans UNE seule transaction.</li>
     *   <li>Envoi de l'e-mail déclenché uniquement APRÈS le commit de la transaction.</li>
     * </ul>
     * Dans tous les cas, aucune exception n'est levée pour préserver le silence (anti-énumération).
     */
    @Transactional
    public void renvoyerLienActivation(String emailRaw) {
        if (emailRaw == null || emailRaw.isBlank()) {
            return;
        }

        String emailNormalized = emailRaw.toLowerCase().trim();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(emailNormalized).orElse(null);

        if (utilisateur == null || utilisateur.isCompteConfirme()) {
            log.debug("Renvoi d'activation ignoré — utilisateur absent ou déjà confirmé.");
            return;
        }

        // Régénérer le jeton (invalide les anciens et crée le nouveau dans la transaction courante)
        final String tokenBrut = genererJeton(utilisateur);
        final String email = utilisateur.getEmail();
        final String nomComplet = utilisateur.getNomComplet();

        // Programmer l'envoi de l'email uniquement après le commit réussi de la transaction
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    activationEmailService.envoyerLienActivation(email, nomComplet, tokenBrut);
                } catch (Exception ex) {
                    log.error("Erreur envoi renvoi lien activation [userId={}] : {}",
                            utilisateur.getId(), ex.getMessage());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Confirmation
    // -------------------------------------------------------------------------

    /**
     * Confirme le compte associé au jeton donné.
     *
     * <p>L'opération est atomique : un seul UPDATE conditionnel garantit qu'un jeton
     * ne peut être utilisé qu'une seule fois, même en cas d'appels concurrents.
     *
     * @throws BusinessRuleException 400 générique si le jeton est invalide, expiré ou déjà utilisé
     */
    @Transactional
    public void confirmerCompte(String tokenBrut) {
        String hash = sha256(tokenBrut);

        // 1. Marquer le jeton utilisé de façon atomique
        int lignes = tokenActivationRepository.consommerToken(hash, Instant.now());
        if (lignes == 0) {
            // 400 générique — on ne distingue pas "inconnu" / "expiré" / "déjà utilisé"
            // pour ne pas aider une énumération de jetons
            throw new BusinessRuleException("ACTIVATION_INVALIDE",
                    "Lien d'activation invalide ou expiré.");
        }

        // 2. Activer le compte
        TokenActivation jeton = tokenActivationRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException("ACTIVATION_INVALIDE",
                        "Lien d'activation invalide ou expiré."));

        Utilisateur utilisateur = jeton.getUtilisateur();
        utilisateur.setActif(true);
        utilisateur.setCompteConfirme(true);
        utilisateurRepository.save(utilisateur);

        log.info("Compte activé : userId={}, email={}", utilisateur.getId(), utilisateur.getEmail());
    }

    // -------------------------------------------------------------------------
    // Utilitaire
    // -------------------------------------------------------------------------

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}
