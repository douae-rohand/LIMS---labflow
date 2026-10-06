package com.backend.modules.utilisateur.service;

import com.backend.common.audit.Auditable;
import com.backend.common.dto.PageResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.auth.service.ActivationCompteService;
import com.backend.modules.client.service.ClientService;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.dto.CreerUtilisateurRequest;
import com.backend.modules.utilisateur.dto.ModifierUtilisateurRequest;
import com.backend.modules.utilisateur.dto.UtilisateurDto;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.mapper.UtilisateurMapper;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final ClientService clientService;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;
    private final ActivationCompteService activationCompteService;
    private final TenantExecutor tenantExecutor;

    /** Rôles que peut créer un ADMINISTRATEUR (les siens exclus). */
    private static final Set<RoleUtilisateur> ROLES_CREABLES_PAR_ADMIN = Set.of(
            RoleUtilisateur.RESPONSABLE,
            RoleUtilisateur.TECHNICIEN,
            RoleUtilisateur.ACCUEIL
    );

    @Transactional(readOnly = true)
    public UtilisateurDto trouverParId(Long id) {
        return tenantExecutor.inCentral(() -> utilisateurMapper.toDto(chargerParId(id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<UtilisateurDto> lister(String search, Pageable pageable) {
        return tenantExecutor.inCentral(() -> {
            Page<Utilisateur> page = StringUtils.hasText(search)
                    ? utilisateurRepository.rechercher(search, pageable)
                    : utilisateurRepository.findAll(pageable);
            return PageResponse.from(page.map(utilisateurMapper::toDto));
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<UtilisateurDto> listerParRole(RoleUtilisateur role, Pageable pageable) {
        return tenantExecutor.inCentral(() ->
            PageResponse.from(
                utilisateurRepository.findByRole(role, pageable)
                        .map(utilisateurMapper::toDto)));
    }

    @Transactional
    @Auditable(action = "CREATION_UTILISATEUR", entite = "Utilisateur")
    public UtilisateurDto creer(CreerUtilisateurRequest request) {
        return tenantExecutor.inCentral(() -> creerInterne(request));
    }

    private UtilisateurDto creerInterne(CreerUtilisateurRequest request) {
        UtilisateurPrincipal principal = principalConnecte();

        // Vérification du droit de créer le rôle demandé
        validerRoleAutorise(principal, request.getRole());

        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("EMAIL_DEJA_UTILISE",
                    "L'email '" + request.getEmail() + "' est déjà utilisé");
        }

        Role role = roleRepository.findByCode(request.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role", "code", request.getRole().name()));

        Laboratoire laboratoire = resoudreLaboratoire(principal, request);

        boolean fluxInvitation = !StringUtils.hasText(request.getMotDePasse());

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                // Flux invitation : hash vide = impossible à authentifier directement
                .motDePasseHash(fluxInvitation ? "" : passwordEncoder.encode(request.getMotDePasse()))
                .telephone(request.getTelephone())
                .role(role)
                .laboratoire(laboratoire)
                .actif(!fluxInvitation)
                .compteConfirme(!fluxInvitation)
                .build();

        utilisateur = utilisateurRepository.save(utilisateur);

        if (request.getRole() == RoleUtilisateur.CLIENT) {
            clientService.creerProfil(utilisateur, request.getRaisonSociale(),
                    request.getIce(), request.getAdresse(), request.getConsentementCndp());
        }

        if (fluxInvitation) {
            activationCompteService.creerEtEnvoyer(utilisateur);
            log.info("Invitation envoyée : id={}, email={}, role={}", utilisateur.getId(),
                    utilisateur.getEmail(), request.getRole());
        } else {
            log.info("Utilisateur créé (direct) : id={}, email={}, role={}",
                    utilisateur.getId(), utilisateur.getEmail(), request.getRole());
        }

        return utilisateurMapper.toDto(utilisateur);
    }

    /**
     * Renvoie l'email d'invitation à un utilisateur dont le compte n'est pas encore confirmé.
     * Invalide l'ancien jeton et en génère un nouveau.
     */
    @Transactional
    @Auditable(action = "RENVOI_INVITATION", entite = "Utilisateur")
    public void renvoyerInvitation(Long id) {
        tenantExecutor.inCentral(() -> {
            Utilisateur utilisateur = chargerParId(id);
            if (utilisateur.isCompteConfirme()) {
                throw new BusinessRuleException("COMPTE_DEJA_ACTIVE",
                        "Ce compte est déjà activé. Impossible de renvoyer une invitation.");
            }
            activationCompteService.creerEtEnvoyer(utilisateur);
            log.info("Invitation renvoyée : userId={}, email={}", utilisateur.getId(), utilisateur.getEmail());
            return null;
        });
    }

    @Transactional
    @Auditable(action = "MODIFICATION_UTILISATEUR", entite = "Utilisateur")
    public UtilisateurDto modifier(Long id, ModifierUtilisateurRequest request) {
        return tenantExecutor.inCentral(() -> modifierInterne(id, request));
    }

    private UtilisateurDto modifierInterne(Long id, ModifierUtilisateurRequest request) {
        Utilisateur utilisateur = chargerParId(id);

        if (StringUtils.hasText(request.getNom())) utilisateur.setNom(request.getNom());
        if (StringUtils.hasText(request.getPrenom())) utilisateur.setPrenom(request.getPrenom());
        if (StringUtils.hasText(request.getTelephone())) utilisateur.setTelephone(request.getTelephone());

        if (StringUtils.hasText(request.getEmail()) &&
                !request.getEmail().equalsIgnoreCase(utilisateur.getEmail())) {
            if (utilisateurRepository.existsByEmail(request.getEmail())) {
                throw new BusinessRuleException("EMAIL_DEJA_UTILISE",
                        "L'email '" + request.getEmail() + "' est déjà utilisé");
            }
            utilisateur.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getNouveauMotDePasse())) {
            utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNouveauMotDePasse()));
        }

        if (request.getActif() != null) utilisateur.setActif(request.getActif());
        if (request.getDeuxFacteursActif() != null)
            utilisateur.setDoubleAuthentification(request.getDeuxFacteursActif());

        return utilisateurMapper.toDto(utilisateurRepository.save(utilisateur));
    }

    @Transactional
    @Auditable(action = "DESACTIVATION_UTILISATEUR", entite = "Utilisateur")
    public void desactiver(Long id) {
        tenantExecutor.inCentral(() -> {
            Utilisateur utilisateur = chargerParId(id);
            utilisateur.setActif(false);
            utilisateurRepository.save(utilisateur);
            log.info("Utilisateur désactivé : id={}", id);
            return null;
        });
    }

    @Transactional
    @Auditable(action = "ACTIVATION_UTILISATEUR", entite = "Utilisateur")
    public void activer(Long id) {
        tenantExecutor.inCentral(() -> {
            Utilisateur utilisateur = chargerParId(id);
            utilisateur.setActif(true);
            utilisateurRepository.save(utilisateur);
            log.info("Utilisateur activé : id={}", id);
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Privé
    // -------------------------------------------------------------------------

    private Utilisateur chargerParId(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
    }

    /**
     * Détermine le laboratoire à assigner au nouvel utilisateur.
     * <ul>
     *   <li>Pour un ADMINISTRATEUR : utilise automatiquement son propre laboratoire.</li>
     *   <li>Pour un SUPER_ADMINISTRATEUR : utilise le {@code laboratoireId} fourni dans la requête.</li>
     *   <li>CLIENT / SUPER_ADMINISTRATEUR sans labo : pas de laboratoire.</li>
     * </ul>
     */
    private Laboratoire resoudreLaboratoire(UtilisateurPrincipal principal, CreerUtilisateurRequest request) {
        if (!estPersonnelLaboratoire(request.getRole())) {
            return null;
        }

        // L'ADMINISTRATEUR est rattaché à son propre labo — on l'utilise automatiquement
        if (principal.getRole() == RoleUtilisateur.ADMINISTRATEUR) {
            if (principal.getLaboratoireId() == null) {
                throw new BusinessRuleException("ADMIN_SANS_LABORATOIRE",
                        "Votre compte administrateur n'est pas rattaché à un laboratoire.");
            }
            return laboratoireRepository.findById(principal.getLaboratoireId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Laboratoire", "id", principal.getLaboratoireId()));
        }

        // SUPER_ADMINISTRATEUR : laboratoireId doit être fourni explicitement
        if (request.getLaboratoireId() == null) {
            throw new BusinessRuleException("LABORATOIRE_OBLIGATOIRE",
                    "Un employé de laboratoire doit être rattaché à un laboratoire.");
        }
        return laboratoireRepository.findById(request.getLaboratoireId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Laboratoire", "id", request.getLaboratoireId()));
    }

    /**
     * Vérifie que l'admin connecté a le droit de créer un utilisateur avec le rôle demandé.
     */
    private void validerRoleAutorise(UtilisateurPrincipal principal, RoleUtilisateur roleVoulu) {
        if (principal.getRole() == RoleUtilisateur.SUPER_ADMINISTRATEUR) {
            // Super admin peut tout créer
            return;
        }
        if (principal.getRole() == RoleUtilisateur.ADMINISTRATEUR) {
            if (!ROLES_CREABLES_PAR_ADMIN.contains(roleVoulu)) {
                throw new BusinessRuleException("ROLE_INTERDIT",
                        "Un administrateur ne peut créer que des comptes : Responsable, Technicien, Agent d'accueil.");
            }
            return;
        }
        throw new BusinessRuleException("ACCES_REFUSE", "Vous n'êtes pas autorisé à créer des utilisateurs.");
    }

    private static UtilisateurPrincipal principalConnecte() {
        Object auth = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (auth instanceof UtilisateurPrincipal p) {
            return p;
        }
        throw new BusinessRuleException("ACCES_REFUSE", "Utilisateur non authentifié.");
    }

    private static boolean estPersonnelLaboratoire(RoleUtilisateur role) {
        return role == RoleUtilisateur.ACCUEIL
                || role == RoleUtilisateur.TECHNICIEN
                || role == RoleUtilisateur.RESPONSABLE
                || role == RoleUtilisateur.ADMINISTRATEUR;
    }
}
