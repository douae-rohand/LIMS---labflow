package com.backend.modules.auth.service;

import com.backend.modules.auth.dto.InscriptionRequest;
import com.backend.modules.auth.entity.TokenActivation;
import com.backend.modules.auth.repository.TokenActivationRepository;
import com.backend.modules.client.entity.ClientProfil;
import com.backend.modules.client.entity.TypeClient;
import com.backend.modules.client.repository.ClientProfilRepository;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Optional;

/**
 * Gestion de l'inscription publique d'un client.
 *
 * <p>Stratégie de traitement selon l'état de l'email :
 * <table>
 *   <tr><td>Email inconnu</td>
 *       <td>Crée utilisateur + client_profil + jeton dans une transaction,
 *           envoie le lien après le commit.</td></tr>
 *   <tr><td>Email existant, compte <b>non confirmé</b></td>
 *       <td>Remplace les données et le mot de passe, révoque les anciens jetons,
 *           émet un nouveau jeton. Même transaction que ci-dessus.</td></tr>
 *   <tr><td>Email existant, compte <b>confirmé</b></td>
 *       <td>Ne modifie rien. Répond 202 silencieusement.</td></tr>
 * </table>
 *
 * <p>Dans tous les cas la réponse HTTP est <b>202 Accepted</b> pour ne pas
 * permettre l'énumération des adresses email.
 *
 * <p><strong>Ordre des opérations :</strong>
 * <ol>
 *   <li>Hacher le mot de passe (durée comparable quel que soit le chemin).</li>
 *   <li>Chercher l'email en base.</li>
 *   <li>Logique métier dans la transaction.</li>
 *   <li>Envoi du lien <em>après</em> le commit (afterCommit).</li>
 * </ol>
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

    // -------------------------------------------------------------------------
    // Point d'entrée
    // -------------------------------------------------------------------------

    /**
     * Traite une demande d'inscription.
     * Retourne silencieusement dans tous les cas (202).
     */
    public void inscrire(InscriptionRequest request) {
        // 1. Hacher AVANT de toucher à la base (timing constant)
        String hash = passwordEncoder.encode(request.getMotDePasse());

        // 2. Lire l'état actuel de l'email
        Optional<Utilisateur> existant = utilisateurRepository.findByEmail(
                request.getEmail().toLowerCase().trim());

        if (existant.isPresent()) {
            Utilisateur utilisateur = existant.get();
            if (utilisateur.isCompteConfirme()) {
                // Compte confirmé : ne rien faire, répondre 202 silencieusement
                log.debug("Inscription ignorée — email déjà confirmé : {}", request.getEmail());
                return;
            }
            // Compte non confirmé : mise à jour
            inscrireOuMettreAJour(request, hash, utilisateur);
        } else {
            inscrireOuMettreAJour(request, hash, null);
        }
    }

    // -------------------------------------------------------------------------
    // Transaction principale
    // -------------------------------------------------------------------------

    /**
     * Crée ou met à jour l'utilisateur, le profil client et le jeton
     * dans une seule transaction. L'envoi du lien se fait après le commit.
     *
     * @param utilisateurExistant null pour une création, non-null pour une mise à jour
     */
    @Transactional
    protected void inscrireOuMettreAJour(InscriptionRequest request, String hash,
                                          Utilisateur utilisateurExistant) {
        Role roleClient = roleRepository.findByCode("CLIENT")
                .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent en base"));

        Utilisateur utilisateur;

        if (utilisateurExistant == null) {
            // --- Création ---
            utilisateur = utilisateurRepository.save(Utilisateur.builder()
                    .nom(request.getNom().trim())
                    .prenom(request.getPrenom() != null ? request.getPrenom().trim() : null)
                    .email(request.getEmail().toLowerCase().trim())
                    .telephone(request.getTelephone())
                    .motDePasseHash(hash)
                    .role(roleClient)
                    .actif(false)          // inactif jusqu'à confirmation
                    .compteConfirme(false)
                    .build());

            // Créer le profil client
            clientProfilRepository.save(buildProfil(utilisateur, request));

        } else {
            // --- Mise à jour (compte non confirmé) ---
            utilisateurExistant.setNom(request.getNom().trim());
            utilisateurExistant.setPrenom(request.getPrenom() != null
                    ? request.getPrenom().trim() : null);
            utilisateurExistant.setTelephone(request.getTelephone());
            utilisateurExistant.setMotDePasseHash(hash);
            utilisateur = utilisateurRepository.save(utilisateurExistant);

            // Mettre à jour le profil (ou créer s'il manquait)
            ClientProfil profil = clientProfilRepository
                    .findByUtilisateur_Id(utilisateur.getId())
                    .orElseGet(() -> ClientProfil.builder()
                            .utilisateur(utilisateur)
                            .dateCreation(Instant.now())
                            .build());
            updateProfil(profil, request);
            clientProfilRepository.save(profil);
        }

        // Générer le jeton (révoque les anciens dans la même transaction)
        final String tokenBrut = activationService.genererJeton(utilisateur);
        final String email = utilisateur.getEmail();

        // Envoyer le lien APRÈS le commit (l'email ne doit pas partir si la transaction rollback)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    activationEmailService.envoyerLienActivation(email, tokenBrut);
                } catch (Exception ex) {
                    // L'envoi du lien ne doit jamais faire échouer l'inscription
                    log.error("Erreur envoi lien activation pour {} : {}", email, ex.getMessage());
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
                .versionConsentementCndp(request.getVersionConsentementCndp())
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
        profil.setVersionConsentementCndp(request.getVersionConsentementCndp());
    }
}
