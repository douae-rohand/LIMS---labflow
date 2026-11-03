package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.util.ApresCommit;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.integration.sendgrid.ResultatEnvoiEmail;
import com.backend.modules.auth.repository.RefreshTokenRepository;
import com.backend.modules.auth.service.ActivationCompteService;
import com.backend.modules.plateforme.dto.CompteAdministrateurDto;
import com.backend.modules.plateforme.dto.CreerAdministrateurLaboratoireRequest;
import com.backend.modules.plateforme.dto.ResultatCompteAdministrateurDto;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.utilisateur.service.MatriculeGeneratorService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdministrateurLaboratoireService {

    private final Map<Long, Instant> dernierEnvoi = new ConcurrentHashMap<>();

    private final UtilisateurRepository utilisateurRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MatriculeGeneratorService matriculeGeneratorService;
    private final ActivationCompteService activationCompteService;
    private final PasswordEncoder passwordEncoder;
    private final JournalAuditService journalAuditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CompteAdministrateurDto creer(CreerAdministrateurLaboratoireRequest request, String acteurEmail) {
        return creerInvite(
                RoleUtilisateur.ADMINISTRATEUR,
                request.getLaboratoireId(),
                request.getNom(),
                request.getPrenom(),
                request.getEmail(),
                request.getTelephone(),
                request.getFonction(),
                acteurEmail);
    }

    /**
     * Même mécanisme que la création d'un administrateur : mot de passe aléatoire jamais renvoyé,
     * compte inactif, lien d'activation à usage unique. L'échec d'envoi ne bloque pas la création.
     */
    public record InvitationPreparee(int numeroLigne, String email, String lien, boolean administrateur, Utilisateur compte) {}

    @Transactional
    public CompteAdministrateurDto creerInvite(RoleUtilisateur roleVoulu, Long laboratoireId,
                                               String nom, String prenom, String emailBrut,
                                               String telephone, String fonction, String acteurEmail) {
        InvitationPreparee preparee = preparerInvite(
                roleVoulu, laboratoireId, nom, prenom, emailBrut, telephone, fonction, acteurEmail, 0);
        ApresCommit.executer(() -> envoyerInvitationPreparee(preparee));
        return versDto(preparee.compte());
    }

    @Transactional
    public InvitationPreparee preparerInvite(RoleUtilisateur roleVoulu, Long laboratoireId,
                                             String nom, String prenom, String emailBrut,
                                             String telephone, String fonction, String acteurEmail,
                                             int numeroLigne) {
        if (roleVoulu != RoleUtilisateur.ADMINISTRATEUR
                && roleVoulu != RoleUtilisateur.RESPONSABLE
                && roleVoulu != RoleUtilisateur.TECHNICIEN
                && roleVoulu != RoleUtilisateur.ACCUEIL) {
            throw new BusinessRuleException("ROLE_NON_AUTORISE",
                    "Ce rôle ne peut pas être créé depuis la plateforme");
        }
        Laboratoire laboratoire = laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", laboratoireId));
        if (roleVoulu == RoleUtilisateur.ADMINISTRATEUR
                && utilisateurRepository.compterAdministrateursActifs(laboratoire.getId()) > 0) {
            throw new BusinessRuleException("ADMINISTRATEUR_ACTIF_EXISTANT",
                    "Ce laboratoire a déjà un administrateur actif");
        }
        String email = emailBrut.trim().toLowerCase(Locale.ROOT);
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("EMAIL_DEJA_UTILISE", "Cet e-mail est déjà associé à un compte");
        }
        Role role = roleRepository.findByCode(roleVoulu.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "code", roleVoulu.name()));

        Utilisateur compte = Utilisateur.builder()
                .matricule(matriculeGeneratorService.genererMatricule(roleVoulu))
                .nom(nom.trim())
                .prenom(texte(prenom))
                .email(email)
                .telephone(texte(telephone))
                .fonction(texte(fonction))
                .motDePasseHash(passwordEncoder.encode(UUID.randomUUID() + "A1a"))
                .mustChangePassword(true)
                .actif(false)
                .compteConfirme(false)
                .role(role)
                .laboratoire(laboratoire)
                .build();
        compte = utilisateurRepository.save(compte);
        String action = roleVoulu == RoleUtilisateur.ADMINISTRATEUR
                ? "CREER_ADMINISTRATEUR_LABORATOIRE"
                : "CREER_UTILISATEUR";
        journalAuditService.enregistrer(
                action, "Utilisateur", compte.getId(), acteurEmail, null, snapshot(compte), null, laboratoire.getId());
        if (compte.getLaboratoire() != null) {
            compte.getLaboratoire().getRaisonSociale();
        }
        String lien = activationCompteService.creerJeton(compte);
        boolean administrateur = roleVoulu == RoleUtilisateur.ADMINISTRATEUR;
        log.info("Compte créé : userId={}, role={}, labo={}", compte.getId(), roleVoulu, laboratoire.getId());
        return new InvitationPreparee(numeroLigne, compte.getEmail(), lien, administrateur, compte);
    }

    @Transactional
    public ResultatCompteAdministrateurDto reinitialiserMotDePasse(Long id, String acteurEmail) {
        Utilisateur admin = charger(id);
        if (!admin.isActif() || !admin.isCompteConfirme()) {
            throw new BusinessRuleException("COMPTE_NON_ACTIVE",
                    "Le compte n'est pas encore activé. Renvoyez l'invitation.");
        }
        limiterEnvoi(admin.getId());
        admin.setMotDePasseHash(passwordEncoder.encode(UUID.randomUUID() + "A1a"));
        admin.setMustChangePassword(true);
        utilisateurRepository.save(admin);
        refreshTokenRepository.revoquerTousParUtilisateur(admin.getId());
        ResultatEnvoiEmail envoi = envoyerLien(admin);
        journalAuditService.enregistrer(
                "REINITIALISER_MOT_DE_PASSE_ADMINISTRATEUR",
                "Utilisateur",
                admin.getId(),
                acteurEmail,
                null,
                "{\"mustChangePassword\":true,\"motDePasseInvalide\":true,\"emailAccepte\":" + envoi.accepte() + "}",
                null,
                idLaboratoire(admin));
        return versResultat(envoi);
    }

    @Async
    public void envoyerInvitationsApresImport(List<InvitationPreparee> preparees, String acteurEmail,
                                               int enregistres, int invalides) {
        int echecs = 0;
        for (InvitationPreparee preparee : preparees) {
            ResultatEnvoiEmail resultat = envoyerInvitationPreparee(preparee);
            if (!resultat.accepte()) {
                echecs++;
                log.warn("Invitation d'import non acceptée [ligne={}, code={}]", preparee.numeroLigne(), resultat.code());
            }
        }
        try {
            journalAuditService.enregistrer(
                    "IMPORTER_UTILISATEURS",
                    "ImportUtilisateur",
                    null,
                    acteurEmail,
                    null,
                    "{\"enregistres\":" + enregistres
                            + ",\"invalides\":" + invalides
                            + ",\"invitationsNonEnvoyees\":" + echecs + "}",
                    null,
                    null);
        } catch (RuntimeException ex) {
            log.error("Synthèse d'import non auditée : {}", ex.getMessage());
        }
    }

    @Transactional
    public ResultatCompteAdministrateurDto renvoyerInvitation(Long id, String acteurEmail) {
        Utilisateur admin = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        String code = admin.getRole() == null ? "" : admin.getRole().getCode();
        if (!RoleUtilisateur.ADMINISTRATEUR.name().equals(code)
                && !RoleUtilisateur.RESPONSABLE.name().equals(code)
                && !RoleUtilisateur.TECHNICIEN.name().equals(code)
                && !RoleUtilisateur.ACCUEIL.name().equals(code)) {
            throw new BusinessRuleException("ROLE_NON_INVITABLE",
                    "L'invitation ne peut pas être renvoyée pour ce rôle.");
        }
        if (admin.isActif() && admin.isCompteConfirme()) {
            throw new BusinessRuleException("COMPTE_DEJA_ACTIVE",
                    "Le compte est déjà activé. Utilisez la réinitialisation du mot de passe.");
        }
        limiterEnvoi(admin.getId());
        ResultatEnvoiEmail envoi = envoyerLien(admin);
        journalAuditService.enregistrer(
                "RENVOYER_INVITATION_ADMINISTRATEUR",
                "Utilisateur",
                admin.getId(),
                acteurEmail,
                null,
                "{\"emailAccepte\":" + envoi.accepte() + "}",
                null,
                idLaboratoire(admin));
        return versResultat(envoi);
    }

    private void limiterEnvoi(Long utilisateurId) {
        Instant maintenant = Instant.now();
        Instant precedent = dernierEnvoi.get(utilisateurId);
        if (precedent != null && precedent.plusSeconds(60).isAfter(maintenant)) {
            throw new BusinessRuleException("ENVOI_TROP_FREQUENT",
                    "Une seule réinitialisation ou un seul renvoi par minute est autorisé pour ce compte.");
        }
        dernierEnvoi.put(utilisateurId, maintenant);
    }

    public ResultatEnvoiEmail envoyerInvitationPreparee(InvitationPreparee preparee) {
        try {
            ResultatEnvoiEmail resultat = preparee.administrateur()
                    ? activationCompteService.envoyerInvitation(preparee.compte(), preparee.lien())
                    : activationCompteService.envoyerInvitationEmploye(preparee.compte(), preparee.lien());
            if (!resultat.accepte() && resultat.temporaire()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interrompu) {
                    Thread.currentThread().interrupt();
                }
                resultat = preparee.administrateur()
                        ? activationCompteService.envoyerInvitation(preparee.compte(), preparee.lien())
                        : activationCompteService.envoyerInvitationEmploye(preparee.compte(), preparee.lien());
            }
            return resultat;
        } catch (RuntimeException ex) {
            log.error("Invitation non envoyée pour userId={} : {}", preparee.compte().getId(), ex.getMessage());
            return ResultatEnvoiEmail.echec(true, 0, "ENVOI_ECHOUE", "L'e-mail n'a pas pu être envoyé");
        }
    }

    private ResultatEnvoiEmail envoyerLien(Utilisateur compte) {
        String lien = activationCompteService.creerJeton(compte);
        try {
            boolean administrateur = compte.getRole() != null
                    && RoleUtilisateur.ADMINISTRATEUR.name().equals(compte.getRole().getCode());
            return administrateur
                    ? activationCompteService.envoyerInvitation(compte, lien)
                    : activationCompteService.envoyerInvitationEmploye(compte, lien);
        } catch (RuntimeException ex) {
            log.error("Invitation non envoyée pour userId={} : {}", compte.getId(), ex.getMessage());
            return ResultatEnvoiEmail.echec(true, 0, "ENVOI_ECHOUE", "L'e-mail n'a pas pu être envoyé");
        }
    }

    private Utilisateur charger(Long id) {
        Utilisateur admin = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        if (admin.getRole() == null || !RoleUtilisateur.ADMINISTRATEUR.name().equals(admin.getRole().getCode())) {
            throw new BusinessRuleException("ROLE_NON_ADMINISTRATEUR",
                    "Ce compte n'est pas un administrateur de laboratoire");
        }
        if (admin.getLaboratoire() == null) {
            throw new BusinessRuleException("ADMINISTRATEUR_SANS_LABORATOIRE",
                    "Cet administrateur n'est rattaché à aucun laboratoire");
        }
        return admin;
    }

    private CompteAdministrateurDto versDto(Utilisateur admin) {
        Laboratoire laboratoire = admin.getLaboratoire();
        return CompteAdministrateurDto.builder()
                .id(admin.getId())
                .laboratoireId(laboratoire == null ? null : laboratoire.getId())
                .laboratoireNom(laboratoire == null ? null : laboratoire.getRaisonSociale())
                .laboratoireStatut(laboratoire == null ? null : laboratoire.getStatut())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .email(admin.getEmail())
                .telephone(admin.getTelephone())
                .fonction(admin.getFonction())
                .statut(statut(admin))
                .derniereConnexion(admin.getDerniereConnexion())
                .mustChangePassword(admin.isMustChangePassword())
                .build();
    }

    private static String statut(Utilisateur admin) {
        if (admin.isActif()) {
            return "ACTIF";
        }
        if (!admin.isCompteConfirme() || (admin.isMustChangePassword() && admin.getDerniereConnexion() == null)) {
            return "EN_ATTENTE";
        }
        return "DESACTIVE";
    }

    private String snapshot(Utilisateur admin) {
        Map<String, Object> valeurs = new LinkedHashMap<>();
        valeurs.put("nom", admin.getNom());
        valeurs.put("prenom", admin.getPrenom());
        valeurs.put("email", admin.getEmail());
        valeurs.put("telephone", admin.getTelephone());
        valeurs.put("fonction", admin.getFonction());
        valeurs.put("actif", admin.isActif());
        valeurs.put("compteConfirme", admin.isCompteConfirme());
        valeurs.put("mustChangePassword", admin.isMustChangePassword());
        try {
            return objectMapper.writeValueAsString(valeurs);
        } catch (JsonProcessingException ex) {
            log.error("Instantané administrateur non sérialisable : {}", ex.getMessage());
            return "{}";
        }
    }

    private static ResultatCompteAdministrateurDto versResultat(ResultatEnvoiEmail envoi) {
        return ResultatCompteAdministrateurDto.builder()
                .emailAccepte(envoi.accepte())
                .message(envoi.message())
                .build();
    }

    private static Long idLaboratoire(Utilisateur admin) {
        return admin.getLaboratoire() == null ? null : admin.getLaboratoire().getId();
    }

    private static String texte(String valeur) {
        if (!StringUtils.hasText(valeur)) {
            return null;
        }
        return valeur.trim();
    }
}
