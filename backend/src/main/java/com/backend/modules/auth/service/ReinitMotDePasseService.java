package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.auth.entity.TokenReinitialisation;
import com.backend.modules.auth.repository.RefreshTokenRepository;
import com.backend.modules.auth.repository.TokenReinitialisationRepository;
import com.backend.modules.auth.security.MotDePasseValidator;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import static com.backend.modules.auth.service.ActivationService.sha256;

/**
 * Gestion du flux « mot de passe oublié » :
 * <ol>
 *   <li>Génération et envoi du lien de réinitialisation (après vérification silencieuse
 *       du compte).</li>
 *   <li>Réinitialisation effective à partir du jeton.</li>
 * </ol>
 *
 * <p><strong>Exclusions silencieuses</strong> (répondent 202 sans créer de jeton) :
 * <ul>
 *   <li>Email inconnu</li>
 *   <li>Compte {@code actif = false} (désactivé par un admin)</li>
 *   <li>Compte {@code compte_confirme = false} — personnels invités non encore activés
 *       ET clients non encore confirmés : la réinitialisation ne contourne jamais le
 *       flux d'invitation ni le flux d'activation.</li>
 * </ul>
 *
 * <p><strong>Séquence de réinitialisation</strong> :
 * <ol>
 *   <li>Charger le jeton (400 générique si absent).</li>
 *   <li>Vérifier expiration et utilise = false (400 générique si invalide).</li>
 *   <li>Valider la politique du nouveau mot de passe AVANT la consommation —
 *       si la politique échoue, la transaction est annulée et le jeton reste intact.</li>
 *   <li>Consommer le jeton atomiquement (UPDATE ... WHERE utilise=false AND date_expiration>now).</li>
 *   <li>Mettre à jour le hash, {@code must_change_password=false}, révoquer tous les refresh tokens.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReinitMotDePasseService {

    private final UtilisateurRepository utilisateurRepository;
    private final TokenReinitialisationRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReinitMotDePasseEmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.reinit-mot-de-passe.token-expiration-minutes:30}")
    private int ttlMinutes;

    // -------------------------------------------------------------------------
    // Demande de réinitialisation
    // -------------------------------------------------------------------------

    /**
     * Traite une demande de réinitialisation.
     * Toujours silencieux (202) — ne révèle jamais l'existence du compte.
     * L'e-mail est envoyé APRÈS le commit.
     */
    @Transactional
    public void demanderReinit(String emailRaw) {
        String email = emailRaw == null ? "" : emailRaw.toLowerCase().trim();

        Utilisateur utilisateur = utilisateurRepository.findByEmail(email).orElse(null);

        // Silencieux si absent, inactif ou non confirmé
        if (utilisateur == null
                || !utilisateur.isActif()
                || !utilisateur.isCompteConfirme()) {
            log.debug("Demande réinitialisation ignorée silencieusement pour email={}",
                    emailRaw != null ? "[masqué]" : "null");
            return;
        }

        // Révoquer les anciens jetons et créer le nouveau dans la même transaction
        int nb = tokenRepository.revoquerTousParUtilisateur(utilisateur.getId());
        if (nb > 0) {
            log.debug("Réinitialisation : {} jeton(s) précédent(s) révoqué(s) pour userId={}",
                    nb, utilisateur.getId());
        }

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String tokenBrut = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = sha256(tokenBrut);

        tokenRepository.save(TokenReinitialisation.builder()
                .tokenHash(tokenHash)
                .utilisateur(utilisateur)
                .dateExpiration(Instant.now().plusSeconds(ttlMinutes * 60L))
                .build());

        log.info("Jeton de réinitialisation créé pour userId={} (expire dans {} min)",
                utilisateur.getId(), ttlMinutes);

        final String emailFinal   = utilisateur.getEmail();
        final String nomComplet   = utilisateur.getNomComplet();

        // E-mail envoyé uniquement après le commit (jamais si rollback)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.envoyerLienReinit(emailFinal, nomComplet, tokenBrut);
                } catch (Exception ex) {
                    log.error("Erreur envoi e-mail réinitialisation [userId={}] : {}",
                            utilisateur.getId(), ex.getMessage());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Réinitialisation effective
    // -------------------------------------------------------------------------

    /**
     * Applique le nouveau mot de passe à partir d'un jeton valide.
     *
     * <p>La politique est validée AVANT la consommation du jeton : si elle échoue,
     * la transaction est annulée et le jeton reste utilisable. Le 2FA n'est ni
     * désactivé ni contourné.
     *
     * @throws BusinessRuleException (400) si le jeton est invalide, expiré ou déjà utilisé,
     *                               ou si la politique de mot de passe n'est pas respectée
     */
    @Transactional
    public void reinitialiser(String tokenBrut, String nouveauMotDePasse) {
        String hash = sha256(tokenBrut);

        // 1. Charger le jeton (400 générique si absent)
        TokenReinitialisation jeton = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessRuleException("REINIT_INVALIDE",
                        "Ce lien est invalide ou a déjà été utilisé."));

        // 2. Vérifier validité avant consommation
        if (jeton.isUtilise()) {
            throw new BusinessRuleException("REINIT_INVALIDE",
                    "Ce lien est invalide ou a déjà été utilisé.");
        }
        if (Instant.now().isAfter(jeton.getDateExpiration())) {
            throw new BusinessRuleException("REINIT_INVALIDE",
                    "Ce lien est invalide ou a déjà été utilisé.");
        }

        Utilisateur utilisateur = jeton.getUtilisateur();

        // 3. Valider la politique AVANT la consommation
        //    → si elle échoue, BusinessRuleException interrompt la transaction → rollback → jeton intact
        MotDePasseValidator.valider(nouveauMotDePasse, utilisateur.getMotDePasseHash(), passwordEncoder);

        // 4. Consommer le jeton atomiquement (double vérification côté base)
        int lignes = tokenRepository.consommerToken(hash, Instant.now());
        if (lignes == 0) {
            // Race condition : un autre appel concurrent a déjà consommé le jeton
            throw new BusinessRuleException("REINIT_INVALIDE",
                    "Ce lien est invalide ou a déjà été utilisé.");
        }

        // 5. Mettre à jour le mot de passe
        utilisateur.setMotDePasseHash(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setMustChangePassword(false);
        utilisateurRepository.save(utilisateur);
        log.info("Mot de passe réinitialisé pour userId={}", utilisateur.getId());

        // 6. Révoquer tous les refresh tokens (invalider toutes les sessions ouvertes)
        int nbRevoked = refreshTokenRepository.revoquerTousParUtilisateur(utilisateur.getId());
        log.debug("{} refresh token(s) révoqué(s) pour userId={}", nbRevoked, utilisateur.getId());

        // Pas d'émission de session — le frontend redirige vers /login
    }
}
