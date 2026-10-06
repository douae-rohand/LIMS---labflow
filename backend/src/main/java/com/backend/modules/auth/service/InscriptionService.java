package com.backend.modules.auth.service;

import com.backend.modules.auth.dto.InscriptionRequest;
import com.backend.modules.client.entity.ClientProfil;
import com.backend.modules.client.entity.TypeClient;
import com.backend.modules.client.repository.ClientProfilRepository;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * Gestion de l'inscription publique d'un client.
 *
 * <p>Stratégie de traitement selon l'état de l'email :
 * <ul>
 *   <li>Email inconnu → crée utilisateur + client_profil + jeton, envoie le lien après commit.</li>
 *   <li>Email existant, compte non confirmé → remplace les données, révoque les anciens jetons,
 *       émet un nouveau lien.</li>
 *   <li>Email existant, compte confirmé → ne modifie rien, répond 202 silencieusement.</li>
 * </ul>
 *
 * <p>Dans tous les cas la réponse HTTP est 202 pour ne pas permettre l'énumération des emails.
 *
 * <p><strong>Note Spring AOP :</strong> la méthode d'entrée {@link #inscrire} est elle-même
 * {@code @Transactional} pour que {@link TransactionSynchronizationManager#registerSynchronization}
 * soit disponible. Un appel interne ({@code this.methode()}) ne traverserait pas le proxy AOP et
 * lèverait {@code IllegalStateException: Transaction synchronization is not active}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InscriptionService {

    private final UtilisateurRepository utilisateurRepository;
    private final ClientProfilRepository clientProfilRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivationService activationService;
    private final ActivationEmailService activationEmailService;

    @Value("${app.inscription.version-consentement-cndp:1.0}")
    private String versionConsentementCndp;

    // -------------------------------------------------------------------------
    // Point d'entrée — @Transactional ici pour que registerSynchronization soit actif
    // -------------------------------------------------------------------------

    /**
     * Traite une demande d'inscription dans une seule transaction.
     * L'envoi du lien d'activation se fait après le commit (afterCommit).
     * Retourne silencieusement dans tous les cas (202).
     */
    @Transactional
    public void inscrire(InscriptionRequest request) {
        // 1. Hacher AVANT de toucher à la base (timing constant quel que soit le chemin)
        String hash = passwordEncoder.encode(request.getMotDePasse());

        // 2. Lire l'état actuel de l'email
        Utilisateur utilisateurExistant = utilisateurRepository
                .findByEmail(request.getEmail().toLowerCase().trim())
                .orElse(null);

        // 3. Cas : compte confirmé → ne rien faire
        if (utilisateurExistant != null && utilisateurExistant.isCompteConfirme()) {
            log.debug("Inscription ignorée — email déjà confirmé : {}", request.getEmail());
            return;
        }

        // 4. Création ou mise à jour dans la transaction courante
        final Utilisateur utilisateur;

        if (utilisateurExistant == null) {
            // --- Création ---
            Role roleClient = roleRepository.findByCode("CLIENT")
                    .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent en base"));

            utilisateur = utilisateurRepository.save(Utilisateur.builder()
                    .nom(request.getNom().trim())
                    .prenom(request.getPrenom() != null ? request.getPrenom().trim() : null)
                    .email(request.getEmail().toLowerCase().trim())
                    .telephone(request.getTelephone())
                    .motDePasseHash(hash)
                    .role(roleClient)
                    .actif(false)
                    .compteConfirme(false)
                    .build());

            clientProfilRepository.save(buildProfil(utilisateur, request));

        } else {
            // --- Mise à jour (compte non confirmé) ---
            utilisateurExistant.setNom(request.getNom().trim());
            utilisateurExistant.setPrenom(
                    request.getPrenom() != null ? request.getPrenom().trim() : null);
            utilisateurExistant.setTelephone(request.getTelephone());
            utilisateurExistant.setMotDePasseHash(hash);
            utilisateur = utilisateurRepository.save(utilisateurExistant);

            ClientProfil profil = clientProfilRepository
                    .findByUtilisateur_Id(utilisateur.getId())
                    .orElseGet(() -> ClientProfil.builder()
                            .utilisateur(utilisateur)
                            .dateCreation(Instant.now())
                            .build());
            updateProfil(profil, request);
            clientProfilRepository.save(profil);
        }

        // 5. Générer le jeton d'activation (révoque les anciens dans la même transaction)
        final String tokenBrut = activationService.genererJeton(utilisateur);
        final String email     = utilisateur.getEmail();
        final String nomComplet = utilisateur.getNomComplet();

        // 6. Envoyer le lien APRÈS le commit
        //    (la synchronisation est active car inscrire() est @Transactional)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    activationEmailService.envoyerLienActivation(email, nomComplet, tokenBrut);
                } catch (Exception ex) {
                    log.error("Erreur envoi lien activation [userId={}] : {}",
                            utilisateur.getId(), ex.getMessage());
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ClientProfil buildProfil(Utilisateur utilisateur, InscriptionRequest request) {
        return ClientProfil.builder()
                .utilisateur(utilisateur)
                .typeClient(request.getTypeClient())
                .raisonSociale(request.getTypeClient() == TypeClient.ENTREPRISE
                        ? request.getRaisonSociale() : null)
                .ice(request.getIce())
                .adresse(request.getAdresse())
                .consentementCndp(request.isConsentementCndp())
                .dateConsentementCndp(request.isConsentementCndp() ? Instant.now() : null)
                .versionConsentementCndp(versionConsentementCndp)
                .dateCreation(Instant.now())
                .build();
    }

    private void updateProfil(ClientProfil profil, InscriptionRequest request) {
        profil.setTypeClient(request.getTypeClient());
        profil.setRaisonSociale(request.getTypeClient() == TypeClient.ENTREPRISE
                ? request.getRaisonSociale() : null);
        profil.setIce(request.getIce());
        profil.setAdresse(request.getAdresse());
        profil.setConsentementCndp(request.isConsentementCndp());
        if (request.isConsentementCndp()) {
            profil.setDateConsentementCndp(Instant.now());
        }
        profil.setVersionConsentementCndp(versionConsentementCndp);
    }
}
