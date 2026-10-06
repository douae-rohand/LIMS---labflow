package com.backend.modules.auth.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.integration.sendgrid.EmailService;
import com.backend.integration.sendgrid.ResultatEnvoiEmail;
import com.backend.modules.auth.entity.TokenActivation;
import com.backend.modules.auth.repository.TokenActivationRepository;
import com.backend.modules.auth.security.MotDePasseValidator;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Activation de compte (lot C) : jeton à usage unique + définition du mot de passe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationCompteService {

    private final TokenActivationRepository tokenActivationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.integration.token-activation-hours:48}")
    private int dureeHeures;

    public int getDureeHeures() {
        return dureeHeures;
    }

    @Transactional
    public String creerJeton(Utilisateur utilisateur) {
        tokenActivationRepository.findByUtilisateur_IdAndUtiliseFalse(utilisateur.getId())
                .forEach(token -> {
                    token.setUtilise(true);
                    tokenActivationRepository.save(token);
                });

        String brut = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        TokenActivation token = TokenActivation.builder()
                .tokenHash(RefreshTokenService.hash(brut))
                .createdAt(Instant.now())
                .dateExpiration(Instant.now().plus(dureeHeures, ChronoUnit.HOURS))
                .utilise(false)
                .utilisateur(utilisateur)
                .build();
        tokenActivationRepository.save(token);

        String lien = frontendUrl.replaceAll("/$", "") + "/activer-compte?token=" + brut;
        log.info("Jeton d'activation créé pour userId={} (expire dans {} h)", utilisateur.getId(), dureeHeures);
        return lien;
    }

    public ResultatEnvoiEmail envoyerInvitation(Utilisateur utilisateur, String lienActivation) {
        String nomComplet = StringUtils.hasText(utilisateur.getPrenom())
                ? utilisateur.getPrenom() + " " + utilisateur.getNom()
                : utilisateur.getNom();
        String nomLaboratoire = utilisateur.getLaboratoire() != null
                && StringUtils.hasText(utilisateur.getLaboratoire().getRaisonSociale())
                ? utilisateur.getLaboratoire().getRaisonSociale()
                : "votre laboratoire";
        return emailService.envoyerInvitationAdministrateur(
                utilisateur.getEmail(), nomComplet, nomLaboratoire, lienActivation, dureeHeures);
    }

    @Transactional
    public ResultatEnvoiEmail creerEtEnvoyer(Utilisateur utilisateur) {
        String lien = creerJeton(utilisateur);
        return envoyerInvitation(utilisateur, lien);
    }

    @Transactional(readOnly = true)
    public void verifier(String tokenBrut) {
        chargerTokenValide(tokenBrut);
    }

    @Transactional
    public void activer(String tokenBrut, String motDePasse) {
        TokenActivation token = chargerTokenValide(tokenBrut);
        Utilisateur utilisateur = token.getUtilisateur();

        MotDePasseValidator.valider(motDePasse, utilisateur.getMotDePasseHash(), passwordEncoder);

        utilisateur.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        utilisateur.setActif(true);
        utilisateur.setMustChangePassword(false);
        utilisateurRepository.save(utilisateur);

        token.setUtilise(true);
        tokenActivationRepository.save(token);
        log.info("Compte activé : userId={}", utilisateur.getId());
    }

    private TokenActivation chargerTokenValide(String tokenBrut) {
        if (tokenBrut == null || tokenBrut.isBlank()) {
            throw new BusinessRuleException("JETON_ACTIVATION_INVALIDE", "Jeton d'activation manquant");
        }
        TokenActivation token = tokenActivationRepository
                .findByTokenHashAndUtiliseFalse(RefreshTokenService.hash(tokenBrut))
                .orElseThrow(() -> new BusinessRuleException("JETON_ACTIVATION_INVALIDE",
                        "Ce lien d'activation est invalide ou déjà utilisé."));

        if (token.getDateExpiration().isBefore(Instant.now())) {
            throw new BusinessRuleException("JETON_ACTIVATION_EXPIRE",
                    "Ce lien d'activation a expiré. Contactez le Super Administrateur.");
        }
        return token;
    }
}
