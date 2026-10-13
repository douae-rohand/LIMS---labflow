package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.common.tenant.TenantExecutor;
import com.backend.common.tenant.TenantProvisioner;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.plateforme.dto.*;
import com.backend.modules.plateforme.entity.*;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlateformeService {

    private static final String STATUT_ACTIF = StatutLabo.ACTIF.name();
    private static final String STATUT_DESACTIVE = StatutLabo.DESACTIVE.name();
    private static final Set<String> TRIS_AUTORISES = Set.of(
            "raisonSociale", "code", "ville", "email", "telephone", "dateCreation", "statut");

    private final LaboratoireRepository laboratoireRepository;
    private final DemandeIntegrationRepository demandeIntegrationRepository;
    private final RoleRepository roleRepository;
    private final TenantProvisioner tenantProvisioner;
    private final TenantExecutor tenantExecutor;
    private final EssaiRepository essaiRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public LaboratoireDto creerLaboratoire(CreerLaboratoireRequest request) {
        if (laboratoireRepository.existsByCode(request.getCode())) {
            throw new BusinessRuleException("CODE_LABORATOIRE_EXISTANT",
                    "Le code laboratoire '" + request.getCode() + "' est déjà utilisé");
        }

        String nomSchema = "lims_" + request.getCode();

        Laboratoire labo = Laboratoire.builder()
                .code(request.getCode())
                .raisonSociale(request.getNom())
                .nomSchema(nomSchema)
                .adresse(request.getAdresse())
                .telephone(request.getTelephone())
                .email(request.getEmailContact())
                .statut(STATUT_ACTIF)
                .build();

        labo = laboratoireRepository.save(labo);
        tenantProvisioner.provisionner(labo);
        log.info("Laboratoire créé : code={}, schema={}", labo.getCode(), labo.getNomSchema());

        return toDto(labo);
    }

    @Transactional(readOnly = true)
    public List<LaboratoirePublicDto> listerLaboratoiresPublics() {
        return laboratoireRepository.findByStatutOrderByRaisonSocialeAsc(STATUT_ACTIF).stream()
                .map(this::toPublicDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LaboratoirePublicDto trouverPublicParId(Long id) {
        return toPublicDto(exigerLaboratoireActif(id));
    }

    public List<AnalysePubliqueDto> listerAnalysesPubliques(Long laboratoireId) {
        Laboratoire lab = tenantExecutor.inCentral(() -> exigerLaboratoireActif(laboratoireId));
        return tenantExecutor.inTenant(lab.getNomSchema(), () -> essaiRepository.findActifsAvecDomaine().stream()
                .map(this::toAnalysePubliqueDto)
                .toList());
    }

    private Laboratoire exigerLaboratoireActif(Long id) {
        Laboratoire lab = laboratoireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", id));
        if (!STATUT_ACTIF.equals(lab.getStatut())) {
            throw new ResourceNotFoundException("Laboratoire", "id", id);
        }
        return lab;
    }

    /**
     * Agrégat public de la landing : rôles et laboratoires lus en base centrale,
     * statuts de demande issus de l'enum métier (même source que le module
     * demande).
     */
    @Transactional(readOnly = true)
    public LandingPublicDto obtenirLandingPublic() {
        List<RolePublicDto> roles = roleRepository.findAllByOrderByIdAsc().stream()
                .map(this::toRolePublicDto)
                .toList();
        List<LaboratoirePublicDto> laboratoires = listerLaboratoiresPublics();
        List<String> statutsDemande = Arrays.stream(StatutDemande.values())
                .map(Enum::name)
                .toList();

        return LandingPublicDto.builder()
                .statistiques(LandingStatistiquesDto.builder()
                        .nombreRoles(roles.size())
                        .nombreLaboratoiresActifs(laboratoires.size())
                        .nombreStatutsDemande(statutsDemande.size())
                        .build())
                .roles(roles)
                .laboratoires(laboratoires)
                .statutsDemande(statutsDemande)
                .build();
    }

    @Transactional(readOnly = true)
    public LaboratoirePublicDto trouverPublicParCode(String code) {
        Laboratoire laboratoire = laboratoireRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "code", code));
        if (!STATUT_ACTIF.equals(laboratoire.getStatut())) {
            throw new UnauthorizedTenantException("Le laboratoire '" + code + "' n'est pas sélectionnable");
        }
        return toPublicDto(laboratoire);
    }

    @Transactional(readOnly = true)
    public LaboratoireDto trouverParCode(String code) {
        return toDto(laboratoireRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "code", code)));
    }

    @Transactional(readOnly = true)
    public Page<LaboratoireDto> listerLaboratoires(Pageable pageable) {
        return rechercherLaboratoires(null, null, null,
                pageable.getPageNumber(), pageable.getPageSize(), "dateCreation", "desc");
    }

    @Transactional(readOnly = true)
    public Page<LaboratoireDto> rechercherLaboratoires(String recherche, String statut, String ville,
            int page, int size, String tri, String ordre) {
        String statutNormalise = normaliserStatut(statut);
        Page<Laboratoire> resultat = laboratoireRepository.rechercher(
                echapperLike(texte(recherche)),
                statutNormalise,
                texte(ville),
                pageDemandee(page, size, tri, ordre));
        Map<Long, Utilisateur> administrateurs = chargerAdministrateurs(resultat.getContent());
        return resultat.map(laboratoire -> toDto(laboratoire, administrateurs.get(laboratoire.getId())));
    }

    @Transactional(readOnly = true)
    public List<LaboratoireDto> listerPourExport(String recherche, String statut, String ville, boolean tous) {
        List<Laboratoire> laboratoires = tous
                ? laboratoireRepository.findAll(Sort.by(Sort.Direction.ASC, "raisonSociale"))
                : laboratoireRepository.rechercherListe(
                        echapperLike(texte(recherche)),
                        normaliserStatut(statut),
                        texte(ville),
                        Sort.by(Sort.Direction.ASC, "raisonSociale"));
        Map<Long, Utilisateur> administrateurs = chargerAdministrateurs(laboratoires);
        return laboratoires.stream()
                .map(laboratoire -> toDto(laboratoire, administrateurs.get(laboratoire.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listerVilles() {
        return laboratoireRepository.findVillesDistinctes();
    }

    @Transactional(readOnly = true)
    public StatistiquesPlateformeDto statistiques() {
        long enAttente = demandeIntegrationRepository.countByStatut(StatutIntegration.EN_ATTENTE.name());
        return StatistiquesPlateformeDto.builder()
                .totalLaboratoires(laboratoireRepository.count())
                .laboratoiresActifs(laboratoireRepository.countByStatut(STATUT_ACTIF))
                .laboratoiresDesactives(laboratoireRepository.countByStatut(STATUT_DESACTIVE))
                .demandesEnAttente(enAttente)
                .totalDemandes(demandeIntegrationRepository.count())
                .build();
    }

    @Transactional
    public LaboratoireDto modifierLaboratoire(Long id, ModifierLaboratoireRequest request) {
        Laboratoire labo = chargerLaboratoire(id);
        String email = texte(request.getEmail());
        if (email != null && laboratoireRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessRuleException("EMAIL_LABORATOIRE_DEJA_UTILISE",
                    "Cette adresse e-mail est déjà utilisée par un autre laboratoire");
        }
        String ice = texte(request.getIce());
        if (ice != null && laboratoireRepository.existsByIceAndIdNot(ice, id)) {
            throw new BusinessRuleException("ICE_DEJA_UTILISE",
                    "Cet identifiant ICE est déjà associé à un autre laboratoire");
        }
        if (request.getLatitude() != null ^ request.getLongitude() != null) {
            throw new BusinessRuleException("COORDONNEES_INCOMPLETES",
                    "La latitude et la longitude doivent être renseignées ensemble");
        }

        labo.setRaisonSociale(request.getNom().trim());
        labo.setIce(ice);
        labo.setTypeLaboratoire(texte(request.getTypeLaboratoire()));
        labo.setAdresse(texte(request.getAdresse()));
        labo.setVille(texte(request.getVille()));
        labo.setRegion(texte(request.getRegion()));
        labo.setPays(texte(request.getPays()));
        labo.setCodePostal(texte(request.getCodePostal()));
        labo.setTelephone(texte(request.getTelephone()));
        labo.setEmail(email);
        labo.setSiteWeb(texte(request.getSiteWeb()));
        labo.setLatitude(request.getLatitude());
        labo.setLongitude(request.getLongitude());
        labo = laboratoireRepository.save(labo);
        log.info("Laboratoire modifié : id={}, code={}", labo.getId(), labo.getCode());
        return toDto(labo);
    }

    @Transactional
    public LaboratoireDto modifierAdministrateur(Long laboratoireId, ModifierAdministrateurLaboratoireRequest request) {
        Laboratoire labo = chargerLaboratoire(laboratoireId);
        Utilisateur admin = utilisateurRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", request.getId()));
        if (admin.getLaboratoire() == null || !laboratoireId.equals(admin.getLaboratoire().getId())) {
            throw new BusinessRuleException("ADMINISTRATEUR_HORS_LABORATOIRE",
                    "Cet administrateur n'est pas rattaché à ce laboratoire");
        }
        if (admin.getRole() == null
                || !RoleUtilisateur.ADMINISTRATEUR.name().equals(admin.getRole().getCode())) {
            throw new BusinessRuleException("ROLE_NON_MODIFIABLE",
                    "Seul le compte administrateur du laboratoire peut être modifié ici");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!email.equalsIgnoreCase(admin.getEmail()) && utilisateurRepository.existsByEmail(email)) {
            throw new BusinessRuleException("EMAIL_ADMIN_DEJA_UTILISE",
                    "Cette adresse e-mail est déjà associée à un compte");
        }

        admin.setNom(request.getNom().trim());
        admin.setPrenom(request.getPrenom().trim());
        admin.setEmail(email);
        admin.setTelephone(texte(request.getTelephone()));
        admin.setFonction(texte(request.getFonction()));
        admin.setCin(texte(request.getCin()));
        utilisateurRepository.save(admin);
        log.info("Administrateur de laboratoire modifié : userId={}, labo={}", admin.getId(), labo.getCode());
        return toDto(labo, admin);
    }

    /**
     * Suspend l'accès au laboratoire sans supprimer son schéma ni ses données.
     * Les comptes restent en base : l'intercepteur de tenant et la connexion
     * refusent l'accès tant que le statut n'est pas ACTIF.
     */
    @Transactional
    public LaboratoireDto desactiverLaboratoire(Long id) {
        return changerStatut(id, STATUT_DESACTIVE);
    }

    @Transactional
    public LaboratoireDto activerLaboratoire(Long id) {
        return changerStatut(id, STATUT_ACTIF);
    }

    private LaboratoireDto changerStatut(Long id, String statut) {
        Laboratoire labo = chargerLaboratoire(id);
        labo.setStatut(statut);
        labo = laboratoireRepository.save(labo);
        log.info("Statut laboratoire mis à jour : id={}, code={}, statut={}, schema={}",
                id, labo.getCode(), statut, labo.getNomSchema());
        return toDto(labo);
    }

    private Laboratoire chargerLaboratoire(Long id) {
        return laboratoireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", id));
    }

    private Pageable pageDemandee(int page, int size, String tri, String ordre) {
        int pageSafe = Math.max(page, 0);
        int sizeSafe = Math.min(Math.max(size, 1), 100);
        String champ = TRIS_AUTORISES.contains(tri) ? tri : "dateCreation";
        Sort.Direction direction = "asc".equalsIgnoreCase(ordre) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageSafe, sizeSafe, Sort.by(direction, champ));
    }

    private String normaliserStatut(String statut) {
        String valeur = texte(statut);
        if (valeur == null) {
            return null;
        }
        String normalise = valeur.toUpperCase(Locale.ROOT);
        try {
            StatutLabo.valueOf(normalise);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("STATUT_LABORATOIRE_INVALIDE",
                    "Le statut du laboratoire est invalide");
        }
        return normalise;
    }

    private Map<Long, Utilisateur> chargerAdministrateurs(List<Laboratoire> laboratoires) {
        List<Long> ids = laboratoires.stream().map(Laboratoire::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Utilisateur> parLaboratoire = new HashMap<>();
        for (Utilisateur admin : utilisateurRepository.findAdministrateursDesLaboratoires(ids)) {
            if (admin.getLaboratoire() != null) {
                parLaboratoire.putIfAbsent(admin.getLaboratoire().getId(), admin);
            }
        }
        return parLaboratoire;
    }

    private static String texte(String valeur) {
        if (!StringUtils.hasText(valeur)) {
            return null;
        }
        return valeur.trim();
    }

    private static String echapperLike(String valeur) {
        if (valeur == null) {
            return null;
        }
        return valeur.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private RolePublicDto toRolePublicDto(Role role) {
        return RolePublicDto.builder()
                .id(role.getId())
                .code(role.getCode())
                .libelle(role.getLibelle())
                .build();
    }

    private LaboratoirePublicDto toPublicDto(Laboratoire l) {
        return LaboratoirePublicDto.builder()
                .id(l.getId())
                .code(l.getCode())
                .raisonSociale(l.getRaisonSociale())
                .ville(l.getVille())
                .adresse(l.getAdresse())
                .telephone(l.getTelephone())
                .email(l.getEmail())
                .ice(l.getIce())
                .statut(l.getStatut())
                .latitude(l.getLatitude())
                .longitude(l.getLongitude())
                .build();
    }

    private AnalysePubliqueDto toAnalysePubliqueDto(Essai essai) {
        return AnalysePubliqueDto.builder()
                .id(essai.getId())
                .code(essai.getCode())
                .designation(essai.getDesignation())
                .description(essai.getDescription())
                .methode(essai.getMethode())
                .domaineCode(essai.getDomaine() != null ? essai.getDomaine().getCode() : null)
                .domaineLibelle(essai.getDomaine() != null ? essai.getDomaine().getLibelle() : null)
                .tarif(essai.getTarif())
                .dureeEstimee(essai.getDureeEstimee())
                .unite(essai.getUnite())
                .actif(essai.getActif())
                .build();
    }

    private LaboratoireDto toDto(Laboratoire laboratoire) {
        return toDto(laboratoire, administrateurUnique(laboratoire.getId()));
    }

    private Utilisateur administrateurUnique(Long laboratoireId) {
        if (laboratoireId == null) {
            return null;
        }
        return utilisateurRepository.findAdministrateursDesLaboratoires(List.of(laboratoireId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private LaboratoireDto toDto(Laboratoire l, Utilisateur admin) {
        return LaboratoireDto.builder()
                .id(l.getId())
                .code(l.getCode())
                .nom(l.getRaisonSociale())
                .schemaName(l.getNomSchema())
                .adresse(l.getAdresse())
                .ville(l.getVille())
                .region(l.getRegion())
                .pays(l.getPays())
                .codePostal(l.getCodePostal())
                .ice(l.getIce())
                .typeLaboratoire(l.getTypeLaboratoire())
                .typesLaboratoire(extraireTypes(l.getTypeLaboratoire()))
                .telephone(l.getTelephone())
                .emailContact(l.getEmail())
                .siteWeb(l.getSiteWeb())
                .latitude(l.getLatitude())
                .longitude(l.getLongitude())
                .statut(l.getStatut())
                .actif(STATUT_ACTIF.equals(l.getStatut()))
                .dateCreation(l.getDateCreation())
                .administrateur(admin == null ? null : toAdminDto(admin))
                .build();
    }

    private AdministrateurLaboratoireDto toAdminDto(Utilisateur admin) {
        return AdministrateurLaboratoireDto.builder()
                .id(admin.getId())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .nomComplet(admin.getNomComplet())
                .email(admin.getEmail())
                .telephone(admin.getTelephone())
                .fonction(admin.getFonction())
                .cin(admin.getCin())
                .actif(admin.isActif())
                .build();
    }

    private static List<String> extraireTypes(String brut) {
        if (!StringUtils.hasText(brut)) {
            return List.of();
        }
        return Arrays.stream(brut.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .toList();
    }
}
