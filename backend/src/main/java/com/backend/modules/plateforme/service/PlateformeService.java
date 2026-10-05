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
import com.backend.modules.plateforme.repository.*;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlateformeService {

    private static final String STATUT_ACTIF = "ACTIF";
    private static final String STATUT_INACTIF = "INACTIF";

    private final LaboratoireRepository laboratoireRepository;
    private final DemandeIntegrationRepository demandeIntegrationRepository;
    private final RoleRepository roleRepository;
    private final TenantProvisioner tenantProvisioner;
    private final TenantExecutor tenantExecutor;
    private final EssaiRepository essaiRepository;

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
        return tenantExecutor.inTenant(lab.getNomSchema(), () ->
                essaiRepository.findActifsAvecDomaine().stream()
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
     * statuts de demande issus de l'enum métier (même source que le module demande).
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
        return laboratoireRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional
    public void desactiverLaboratoire(Long id) {
        Laboratoire labo = laboratoireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", id));
        labo.setStatut(STATUT_INACTIF);
        laboratoireRepository.save(labo);
        log.info("Laboratoire désactivé : id={}, code={}", id, labo.getCode());
    }

    @Transactional
    public DemandeIntegrationDto soumettreDemande(DemandeIntegrationDto dto) {
        DemandeIntegration demande = DemandeIntegration.builder()
                .numero("INT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .dateDemande(Instant.now())
                .statut(StatutIntegration.EN_ATTENTE.name())
                .raisonSociale(dto.getNomLaboratoire())
                .contactNom(dto.getNomRepresentant() != null ? dto.getNomRepresentant() : dto.getNomLaboratoire())
                .contactEmail(dto.getEmailRepresentant())
                .contactTelephone(dto.getTelephoneRepresentant())
                .build();

        demande = demandeIntegrationRepository.save(demande);
        log.info("Demande d'intégration soumise : id={}, labo={}", demande.getId(), demande.getRaisonSociale());
        return toDto(demande);
    }

    @Transactional
    public DemandeIntegrationDto traiterDemande(Long id, StatutIntegration decision, String commentaire) {
        DemandeIntegration demande = demandeIntegrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DemandeIntegration", "id", id));

        demande.setStatut(decision.name());
        if (decision == StatutIntegration.REJETEE) {
            demande.setMotifRefus(commentaire);
        }

        if (decision == StatutIntegration.APPROUVEE) {
            Laboratoire laboratoire = demande.getLaboratoire();
            if (laboratoire == null) {
                String code = slugifier(demande.getRaisonSociale());
                if (laboratoireRepository.existsByCode(code)) {
                    code = code + "_" + demande.getId();
                }
                laboratoire = laboratoireRepository.save(Laboratoire.builder()
                        .code(code)
                        .raisonSociale(demande.getRaisonSociale())
                        .nomSchema("lims_" + code)
                        .email(demande.getContactEmail())
                        .telephone(demande.getContactTelephone())
                        .statut(STATUT_ACTIF)
                        .build());
                demande.setLaboratoire(laboratoire);
            }
            tenantProvisioner.provisionner(laboratoire);
        }

        return toDto(demandeIntegrationRepository.save(demande));
    }

    @Transactional(readOnly = true)
    public Page<DemandeIntegrationDto> listerDemandes(StatutIntegration statut, Pageable pageable) {
        Page<DemandeIntegration> page = (statut != null)
                ? demandeIntegrationRepository.findByStatut(statut.name(), pageable)
                : demandeIntegrationRepository.findAll(pageable);
        return page.map(this::toDto);
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

    private static String slugifier(String valeur) {
        String slug = valeur == null ? "lab" : valeur.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
        if (slug.isBlank()) {
            slug = "lab";
        }
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }

    private LaboratoireDto toDto(Laboratoire l) {
        return LaboratoireDto.builder()
                .id(l.getId())
                .code(l.getCode())
                .nom(l.getRaisonSociale())
                .schemaName(l.getNomSchema())
                .adresse(l.getAdresse())
                .telephone(l.getTelephone())
                .emailContact(l.getEmail())
                .actif(STATUT_ACTIF.equals(l.getStatut()))
                .dateCreation(l.getDateCreation())
                .build();
    }

    private DemandeIntegrationDto toDto(DemandeIntegration d) {
        return DemandeIntegrationDto.builder()
                .id(d.getId())
                .nomLaboratoire(d.getRaisonSociale())
                .emailRepresentant(d.getContactEmail())
                .nomRepresentant(d.getContactNom())
                .telephoneRepresentant(d.getContactTelephone())
                .message(d.getMotifRefus())
                .statut(enumOuNull(StatutIntegration.class, d.getStatut()))
                .commentaireAdmin(d.getMotifRefus())
                .dateSoumission(d.getDateDemande())
                .build();
    }

    private static <E extends Enum<E>> E enumOuNull(Class<E> type, String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
