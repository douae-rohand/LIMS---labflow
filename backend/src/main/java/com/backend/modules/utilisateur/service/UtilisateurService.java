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
import org.springframework.data.domain.Sort;
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
    private final MatriculeGeneratorService matriculeGeneratorService;

    /** Rôles que peut créer un ADMINISTRATEUR (les siens exclus). */
    private static final Set<RoleUtilisateur> ROLES_CREABLES_PAR_ADMIN = Set.of(
            RoleUtilisateur.RESPONSABLE,
            RoleUtilisateur.TECHNICIEN,
            RoleUtilisateur.ACCUEIL
    );

    @Transactional(readOnly = true)
    public UtilisateurDto trouverParId(Long id) {
        return tenantExecutor.inCentral(() -> utilisateurMapper.toDto(chargerPourAppelant(id, true)));
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

    /**
     * Liste les employés du laboratoire de l'administrateur connecté avec filtres, recherche et pagination.
     */
    @Transactional(readOnly = true)
    public PageResponse<com.backend.modules.utilisateur.dto.EmployeDto> listerEmployes(
            String q,
            String statutStr,
            String roleStr,
            int page,
            int size,
            String tri,
            String direction) {

        UtilisateurPrincipal principal = principalConnecte();
        if (principal.getLaboratoireId() == null) {
            throw new BusinessRuleException("ADMIN_SANS_LABORATOIRE",
                    "Votre compte administrateur n'est pas rattaché à un laboratoire.");
        }

        // 1. Validation et normalisation du rôle (doit être RESPONSABLE, TECHNICIEN ou ACCUEIL)
        String roleFiltre = null;
        if (StringUtils.hasText(roleStr)) {
            String roleNorm = roleStr.trim().toUpperCase();
            if (!Set.of("RESPONSABLE", "TECHNICIEN", "ACCUEIL").contains(roleNorm)) {
                throw new BusinessRuleException("ROLE_INVALIDE",
                        "Rôle invalide. Valeurs autorisées : RESPONSABLE, TECHNICIEN, ACCUEIL.");
            }
            roleFiltre = roleNorm;
        }

        // 2. Validation et normalisation du statut
        String statutFiltre = null;
        if (StringUtils.hasText(statutStr)) {
            String statutNorm = statutStr.trim().toUpperCase();
            try {
                com.backend.modules.utilisateur.entity.StatutEmploye.valueOf(statutNorm);
                statutFiltre = statutNorm;
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException("STATUT_INVALIDE",
                        "Statut invalide. Valeurs autorisées : ACTIF, EN_ATTENTE, DESACTIVE.");
            }
        }

        // 3. Traitement et échappement de la recherche partielle
        String qFiltre = null;
        if (StringUtils.hasText(q)) {
            String qTrim = q.trim();
            if (qTrim.length() > 100) {
                throw new BusinessRuleException("RECHERCHE_TROP_LONGUE",
                        "Le terme de recherche ne doit pas dépasser 100 caractères.");
            }
            if (!qTrim.isEmpty()) {
                // Échappement des caractères spéciaux SQL avec '!'
                qFiltre = qTrim.replace("!", "!!")
                               .replace("%", "!%")
                               .replace("_", "!_");
            }
        }

        // 4. Plafonnement de la taille de page
        int taillePlafonnee = Math.min(Math.max(1, size), 50);
        int pageAjustee = Math.max(0, page);

        // 5. Validation de la liste blanche de tri et de la direction
        Set<String> champsTriAutorises = Set.of(
                "nom", "prenom", "matricule", "dateCreation", "derniereConnexion"
        );
        String champTri = (StringUtils.hasText(tri)) ? tri.trim() : "nom";
        if (!champsTriAutorises.contains(champTri)) {
            throw new BusinessRuleException("TRI_INVALIDE",
                    "Champ de tri non autorisé : " + tri + ". Champs autorisés : nom, prenom, matricule, dateCreation, derniereConnexion.");
        }

        Sort.Direction sortDirection;
        String dirNorm = StringUtils.hasText(direction) ? direction.trim().toLowerCase() : "asc";
        if ("asc".equals(dirNorm)) {
            sortDirection = Sort.Direction.ASC;
        } else if ("desc".equals(dirNorm)) {
            sortDirection = Sort.Direction.DESC;
        } else {
            throw new BusinessRuleException("DIRECTION_TRI_INVALIDE",
                    "Direction de tri invalide : " + direction + ". Valeurs autorisées : asc, desc.");
        }

        // Tri avec bris d'égalité par 'id' pour assurer la stabilité de pagination
        Sort sort = Sort.by(sortDirection, champTri).and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageAjustee, taillePlafonnee, sort);

        final String finalRole = roleFiltre;
        final String finalStatut = statutFiltre;
        final String finalQ = qFiltre;

        return tenantExecutor.inCentral(() -> {
            Page<Utilisateur> pageResult = utilisateurRepository.rechercherEmployesLaboratoire(
                    principal.getLaboratoireId(),
                    finalRole,
                    finalStatut,
                    finalQ,
                    pageable
            );

            // Mapping vers EmployeDto à l'intérieur du contexte central (résolution LAZY du Role sécurisée)
            return PageResponse.from(pageResult.map(this::versEmployeDto));
        });
    }

    private com.backend.modules.utilisateur.dto.EmployeDto versEmployeDto(Utilisateur u) {
        com.backend.modules.utilisateur.entity.StatutEmploye statut =
                calculerStatutEmploye(u.isActif(), u.isCompteConfirme());

        String roleCode = u.getRole() != null ? u.getRole().getCode() : null;

        return com.backend.modules.utilisateur.dto.EmployeDto.builder()
                .id(u.getId())
                .matricule(u.getMatricule())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .nomComplet(u.getNomComplet())
                .email(u.getEmail())
                .telephone(u.getTelephone())
                .role(roleCode)
                .statut(statut)
                .derniereConnexion(u.getDerniereConnexion())
                .dateCreation(u.getDateCreation())
                .build();
    }

    private static com.backend.modules.utilisateur.entity.StatutEmploye calculerStatutEmploye(
            boolean actif, boolean compteConfirme) {
        if (!compteConfirme) {
            // Note : couvre aussi (actif=true, compteConfirme=false) tant que le lot 4 n'interdit pas d'activer un compte non confirmé
            return com.backend.modules.utilisateur.entity.StatutEmploye.EN_ATTENTE;
        }
        return actif ? com.backend.modules.utilisateur.entity.StatutEmploye.ACTIF
                     : com.backend.modules.utilisateur.entity.StatutEmploye.DESACTIVE;
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

        String matricule = matriculeGeneratorService.genererMatricule(role.getCode());

        Utilisateur utilisateur = Utilisateur.builder()
                .matricule(matricule)
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
            Utilisateur utilisateur = chargerPourAppelant(id, false);
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
        Utilisateur utilisateur = chargerPourAppelant(id, true);

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
            Utilisateur utilisateur = chargerPourAppelant(id, false);
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
            Utilisateur utilisateur = chargerPourAppelant(id, false);
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

    private Utilisateur chargerPourAppelant(Long id, boolean autoriserProprietaire) {
        UtilisateurPrincipal principal = principalConnecte();
        Utilisateur utilisateur = chargerParId(id);

        // 1. Raccourci propriétaire traité avant tout contrôle par rôle (uniquement pour GET et PATCH /{id})
        if (autoriserProprietaire && principal.getId() != null && principal.getId().equals(utilisateur.getId())) {
            return utilisateur;
        }

        // 2. Si l'appelant est SUPER_ADMINISTRATEUR : accès global (aucun contrôle supplémentaire)
        if (principal.getRole() == RoleUtilisateur.SUPER_ADMINISTRATEUR) {
            return utilisateur;
        }

        // 3. Si l'appelant est ADMINISTRATEUR :
        if (principal.getRole() == RoleUtilisateur.ADMINISTRATEUR) {
            String roleCode = utilisateur.getRole() != null ? utilisateur.getRole().getCode() : null;

            // Interdiction de cibler un CLIENT, un SUPER_ADMINISTRATEUR ou un autre ADMINISTRATEUR (404 générique opaque)
            if ("CLIENT".equalsIgnoreCase(roleCode)
                    || "SUPER_ADMINISTRATEUR".equalsIgnoreCase(roleCode)
                    || "ADMINISTRATEUR".equalsIgnoreCase(roleCode)) {
                throw new ResourceNotFoundException("Utilisateur", "id", id);
            }

            // Exigence de laboratoire identique (404 générique opaque)
            Long laboCibleId = utilisateur.getLaboratoire() != null ? utilisateur.getLaboratoire().getId() : null;
            if (principal.getLaboratoireId() == null || !principal.getLaboratoireId().equals(laboCibleId)) {
                throw new ResourceNotFoundException("Utilisateur", "id", id);
            }

            return utilisateur;
        }

        // Tout autre rôle non autorisé
        throw new ResourceNotFoundException("Utilisateur", "id", id);
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
