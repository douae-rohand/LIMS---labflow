package com.backend.modules.accueil.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.integration.sendgrid.EmailService;
import com.backend.modules.accueil.dto.ClientsDtos.ClientAccueilDto;
import com.backend.modules.accueil.dto.ClientsDtos.CreerClientRequest;
import com.backend.modules.accueil.repository.ClientsAccueilRepository;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.auth.service.ActivationCompteService;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.entity.ClientProfil;
import com.backend.modules.client.entity.TypeClient;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.client.repository.ClientProfilRepository;
import com.backend.modules.client.service.ClientService;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.utilisateur.service.MatriculeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Enregistrement des clients à l'accueil : création d'un compte client (le client choisit son mot
 * de passe via le lien d'activation reçu par e-mail), rattachement d'un client déjà inscrit sur la
 * plateforme, et renvoi de l'invitation. Les comptes vivent dans le schéma central ; la fiche
 * locale du laboratoire est créée par {@link ClientService#assurerFicheLocale}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientsAccueilService {

    private final ClientsAccueilRepository clientsAccueilRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final ClientProfilRepository clientProfilRepository;
    private final ClientLaboratoireRepository clientLaboratoireRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final ClientService clientService;
    private final ActivationCompteService activationCompteService;
    private final MatriculeGeneratorService matriculeGeneratorService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JournalAuditService journalAuditService;
    private final TenantExecutor tenantExecutor;

    @Value("${app.inscription.version-consentement-cndp:1.0}")
    private String versionConsentementCndp;

    /** Clients du laboratoire, les plus récemment rattachés en premier. */
    public List<ClientAccueilDto> lister() {
        Long laboratoireId = laboratoireCourant();
        return tenantExecutor.inCentral(() -> clientsAccueilRepository.clientsDuLaboratoire(laboratoireId).stream()
                .map(ligne -> versDto((ClientLaboratoire) ligne[0], (Utilisateur) ligne[1], (ClientProfil) ligne[2]))
                .toList());
    }

    public ClientAccueilDto creer(CreerClientRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        Long laboratoireId = laboratoireCourant();
        String schema = TenantContext.getCurrentTenant();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (request.type() == TypeClient.ENTREPRISE && !StringUtils.hasText(request.raisonSociale())) {
            throw new BusinessRuleException("RAISON_SOCIALE_OBLIGATOIRE", "Indiquez la raison sociale de l'entreprise.");
        }

        record Cree(Long id, String nomComplet, String lien) {
        }
        Cree cree = tenantExecutor.inCentral(() -> {
            Utilisateur existant = utilisateurRepository.findByEmail(email).orElse(null);
            if (existant != null) {
                boolean client = existant.getRole() != null && "CLIENT".equals(existant.getRole().getCode());
                throw new BusinessRuleException("EMAIL_DEJA_UTILISE", client
                        ? "Un client est déjà inscrit avec cet e-mail : utilisez « Rattacher un client existant »."
                        : "Cet e-mail est déjà utilisé par un autre compte.");
            }
            Role role = roleRepository.findByCode("CLIENT")
                    .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent en base"));
            Utilisateur utilisateur = utilisateurRepository.save(Utilisateur.builder()
                    .matricule(matriculeGeneratorService.genererMatricule("CLIENT"))
                    .nom(request.nom().trim())
                    .prenom(StringUtils.hasText(request.prenom()) ? request.prenom().trim() : null)
                    .email(email)
                    .telephone(StringUtils.hasText(request.telephone()) ? request.telephone().trim() : null)
                    // Mot de passe inutilisable : le client choisit le sien via le lien d'activation.
                    .motDePasseHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(role)
                    .actif(false)
                    .compteConfirme(false)
                    .build());
            Instant maintenant = Instant.now();
            clientProfilRepository.save(ClientProfil.builder()
                    .utilisateur(utilisateur)
                    .typeClient(request.type())
                    .raisonSociale(request.type() == TypeClient.ENTREPRISE ? request.raisonSociale().trim() : null)
                    .ice(StringUtils.hasText(request.ice()) ? request.ice().trim() : null)
                    .adresse(StringUtils.hasText(request.adresse()) ? request.adresse().trim() : null)
                    .consentementCndp(true)
                    .dateConsentementCndp(maintenant)
                    .versionConsentementCndp(versionConsentementCndp)
                    .dateCreation(maintenant)
                    .build());
            return new Cree(utilisateur.getId(), utilisateur.getNomComplet(), activationCompteService.creerJeton(utilisateur));
        });

        clientService.assurerFicheLocale(cree.id(), schema);
        journalAuditService.enregistrer("CREER_CLIENT_ACCUEIL", "Utilisateur", cree.id(), acteur.getEmail(), null,
                "{\"email\":\"" + email + "\",\"type\":\"" + request.type() + "\",\"consentementCndp\":true}",
                null, laboratoireId);
        inviter(email, cree.nomComplet(), cree.lien(), laboratoireId);
        return trouver(cree.id(), laboratoireId);
    }

    /** Rattache au laboratoire un client déjà inscrit sur la plateforme (recherche exacte par e-mail). */
    public ClientAccueilDto rattacher(String emailBrut) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        Long laboratoireId = laboratoireCourant();
        String email = emailBrut.trim().toLowerCase(Locale.ROOT);
        Long utilisateurId = tenantExecutor.inCentral(() -> {
            Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                    .filter(u -> u.getRole() != null && "CLIENT".equals(u.getRole().getCode()))
                    .orElseThrow(() -> new ResourceNotFoundException("Aucun client inscrit avec cet e-mail."));
            if (clientLaboratoireRepository.findByUtilisateur_IdAndLaboratoire_Id(utilisateur.getId(), laboratoireId).isPresent()) {
                throw new BusinessRuleException("CLIENT_DEJA_RATTACHE", "Ce client est déjà enregistré dans votre laboratoire.");
            }
            return utilisateur.getId();
        });
        clientService.assurerFicheLocale(utilisateurId, TenantContext.getCurrentTenant());
        journalAuditService.enregistrer("RATTACHER_CLIENT_ACCUEIL", "Utilisateur", utilisateurId, acteur.getEmail(),
                null, "{\"email\":\"" + email + "\"}", null, laboratoireId);
        return trouver(utilisateurId, laboratoireId);
    }

    /** Nouveau lien d'activation pour un client du laboratoire qui n'a pas encore activé son compte. */
    public ClientAccueilDto renvoyerInvitation(Long utilisateurId) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        Long laboratoireId = laboratoireCourant();
        record Invitation(String email, String nomComplet, String lien) {
        }
        Invitation invitation = tenantExecutor.inCentral(() -> {
            if (clientLaboratoireRepository.findByUtilisateur_IdAndLaboratoire_Id(utilisateurId, laboratoireId).isEmpty()) {
                throw new ResourceNotFoundException("Client", "id", utilisateurId);
            }
            Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Client", "id", utilisateurId));
            if (utilisateur.isCompteConfirme()) {
                throw new BusinessRuleException("COMPTE_DEJA_ACTIVE", "Ce client a déjà activé son compte.");
            }
            return new Invitation(utilisateur.getEmail(), utilisateur.getNomComplet(),
                    activationCompteService.creerJeton(utilisateur));
        });
        journalAuditService.enregistrer("RENVOYER_INVITATION_CLIENT", "Utilisateur", utilisateurId, acteur.getEmail(),
                null, null, null, laboratoireId);
        inviter(invitation.email(), invitation.nomComplet(), invitation.lien(), laboratoireId);
        return trouver(utilisateurId, laboratoireId);
    }

    // ------------------------------------------------------------------ outils

    private void inviter(String email, String nomComplet, String lien, Long laboratoireId) {
        try {
            String laboratoire = tenantExecutor.inCentral(() -> laboratoireRepository.findById(laboratoireId)
                    .map(l -> l.getRaisonSociale()).orElse("votre laboratoire"));
            var resultat = emailService.envoyerInvitationClient(email, nomComplet, laboratoire, lien,
                    activationCompteService.getDureeHeures());
            if (!resultat.accepte()) {
                log.warn("Invitation client non envoyée à {} : {}", email, resultat.code());
            }
        } catch (RuntimeException ex) {
            log.warn("Invitation client non envoyée à {} : {}", email, ex.getMessage());
        }
    }

    private ClientAccueilDto trouver(Long utilisateurId, Long laboratoireId) {
        return tenantExecutor.inCentral(() -> clientsAccueilRepository.clientsDuLaboratoire(laboratoireId).stream()
                .filter(ligne -> ((Utilisateur) ligne[1]).getId().equals(utilisateurId))
                .findFirst()
                .map(ligne -> versDto((ClientLaboratoire) ligne[0], (Utilisateur) ligne[1], (ClientProfil) ligne[2]))
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", utilisateurId)));
    }

    private static ClientAccueilDto versDto(ClientLaboratoire lien, Utilisateur u, ClientProfil p) {
        return new ClientAccueilDto(u.getId(), u.getNomComplet(), u.getEmail(), u.getTelephone(),
                p == null ? TypeClient.PARTICULIER : p.getTypeClient(),
                p == null ? null : p.getRaisonSociale(), p == null ? null : p.getIce(), p == null ? null : p.getAdresse(),
                u.isCompteConfirme(), lien.getDatePremierContact());
    }

    private static Long laboratoireCourant() {
        Long laboratoireId = SecurityUtils.principalCourant().getLaboratoireId();
        if (laboratoireId == null) {
            throw new BusinessRuleException("SANS_LABORATOIRE", "Compte non rattaché à un laboratoire.");
        }
        return laboratoireId;
    }
}
