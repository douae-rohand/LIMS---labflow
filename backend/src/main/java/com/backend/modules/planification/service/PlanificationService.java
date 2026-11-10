package com.backend.modules.planification.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.ProprieteClient;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.planification.dto.AffecterAnalysesRequest;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.planification.dto.PlanificationDto;
import com.backend.modules.planification.dto.TechnicienChargeDto;
import com.backend.modules.planification.entity.StatutPlanification;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.entity.UtilisateurDomaine;
import com.backend.modules.utilisateur.repository.UtilisateurDomaineRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Planification des analyses : affectation des lignes d'essai aux techniciens
 * du laboratoire et suivi de leur charge.
 */
@Service
@RequiredArgsConstructor
public class PlanificationService {

    /** Demandes dont les analyses peuvent être planifiées ou suivies. */
    static final List<String> STATUTS_DEMANDES_ACTIVES = Stream.of(
                    StatutDemande.ACCEPTEE, StatutDemande.EN_COURS, StatutDemande.EN_VALIDATION)
            .flatMap(statut -> statut.aliasesPersistés().stream())
            .toList();

    private final LigneEssaiRepository ligneEssaiRepository;
    private final DemandeRepository demandeRepository;
    private final ProprieteClient proprieteClient;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurDomaineRepository utilisateurDomaineRepository;
    private final TenantExecutor tenantExecutor;
    private final LigneTravailAssembleur assembleur;
    private final JournalAuditService journalAuditService;

    @Transactional(readOnly = true)
    public PlanificationDto trouverParId(Long id) {
        if (!proprieteClient.clientConnecte()) {
            return toDto(charger(id));
        }
        LigneEssai ligne = ligneEssaiRepository.findById(id).orElseThrow(proprieteClient::introuvable);
        proprieteClient.exiger(ligne.getDemande());
        return toDto(ligne);
    }

    @Transactional(readOnly = true)
    public List<PlanificationDto> listerParDemande(Long demandeId) {
        proprieteClient.exigerDemande(demandeId);
        return ligneEssaiRepository.findByDemande_Id(demandeId).stream().map(this::toDto).toList();
    }

    /**
     * Analyses des demandes acceptées ou en cours.
     *
     * @param filtre A_AFFECTER, AFFECTEES, EN_RETARD ou null (toutes)
     */
    @Transactional(readOnly = true)
    public List<LigneTravailDto> listerAnalyses(String filtre) {
        Instant maintenant = Instant.now();
        List<LigneEssai> lignes = ligneEssaiRepository.findLignesDesDemandes(STATUTS_DEMANDES_ACTIVES).stream()
                .filter(ligne -> switch (filtre == null ? "" : filtre) {
                    case "A_AFFECTER" -> ligne.getTechnicienId() == null && !LigneTravailAssembleur.terminee(ligne);
                    case "AFFECTEES" -> ligne.getTechnicienId() != null && !LigneTravailAssembleur.terminee(ligne);
                    case "EN_RETARD" -> LigneTravailAssembleur.enRetard(ligne, maintenant);
                    default -> true;
                })
                .toList();
        return assembleur.assembler(lignes);
    }

    /** Techniciens actifs du laboratoire avec leur charge actuelle. */
    @Transactional(readOnly = true)
    public List<TechnicienChargeDto> techniciens() {
        return techniciens(ligneEssaiRepository.findLignesDesDemandes(STATUTS_DEMANDES_ACTIVES));
    }

    /** Charge des techniciens calculée sur des analyses déjà chargées (évite une seconde requête). */
    @Transactional(readOnly = true)
    public List<TechnicienChargeDto> techniciens(List<LigneEssai> lignes) {
        Long laboratoireId = laboratoireCourant();
        List<Utilisateur> techniciens = tenantExecutor.inCentral(() ->
                utilisateurRepository.findActifsParRoleEtLaboratoire(RoleUtilisateur.TECHNICIEN.name(), laboratoireId));
        Map<Long, List<String>> domaines = domainesDes(techniciens.stream().map(Utilisateur::getId).toList());
        Instant maintenant = Instant.now();
        return techniciens.stream()
                .map(technicien -> {
                    List<LigneEssai> siennes = lignes.stream()
                            .filter(l -> technicien.getId().equals(l.getTechnicienId()))
                            .filter(l -> !LigneTravailAssembleur.terminee(l))
                            .toList();
                    return new TechnicienChargeDto(
                            technicien.getId(),
                            nomComplet(technicien),
                            technicien.getEmail(),
                            domaines.getOrDefault(technicien.getId(), List.of()),
                            siennes.stream().filter(l -> l.getStatut() != StatutEssai.EN_COURS).count(),
                            siennes.stream().filter(l -> l.getStatut() == StatutEssai.EN_COURS).count(),
                            siennes.stream().filter(l -> LigneTravailAssembleur.enRetard(l, maintenant)).count());
                })
                .sorted(Comparator.comparingLong(TechnicienChargeDto::total))
                .toList();
    }

    /**
     * Affecte des analyses à un technicien actif du laboratoire. Si le technicien a déclaré
     * ses domaines, l'essai doit relever de l'un d'eux. La demande passe « en cours ».
     */
    @Transactional
    public List<LigneTravailDto> affecter(AffecterAnalysesRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        Long laboratoireId = laboratoireCourant();
        // Le rôle et le laboratoire sont chargés paresseusement : on vérifie dans la session centrale.
        Utilisateur technicien = tenantExecutor.inCentral(() -> utilisateurRepository
                .findById(request.technicienId())
                .filter(u -> u.getRole() != null
                        && RoleUtilisateur.TECHNICIEN.name().equals(u.getRole().getCode())
                        && u.isActif()
                        && u.getLaboratoire() != null
                        && laboratoireId.equals(u.getLaboratoire().getId()))
                .orElse(null));
        if (technicien == null) {
            throw new BusinessRuleException("TECHNICIEN_INVALIDE",
                    "Choisissez un technicien actif de votre laboratoire.");
        }
        List<String> domaines = domainesDes(List.of(technicien.getId())).getOrDefault(technicien.getId(), List.of());

        List<LigneEssai> lignes = ligneEssaiRepository.findAvecDetails(new HashSet<>(request.ligneIds()));
        if (lignes.size() != new HashSet<>(request.ligneIds()).size()) {
            throw new ResourceNotFoundException("Analyse introuvable");
        }
        Instant maintenant = Instant.now();
        Instant datePrevue = request.datePrevue() == null ? null
                : request.datePrevue().atStartOfDay(ZoneOffset.UTC).toInstant();
        if (request.datePrevue() != null && request.datePrevue().isBefore(LocalDate.now(ZoneOffset.UTC))) {
            throw new BusinessRuleException("DATE_PREVUE_PASSEE", "La date prévue ne peut pas être dans le passé.");
        }
        Set<Demande> demandes = new HashSet<>();
        for (LigneEssai ligne : lignes) {
            Demande demande = ligne.getDemande();
            if (!STATUTS_DEMANDES_ACTIVES.contains(demande.getStatut())) {
                throw new BusinessRuleException("DEMANDE_NON_PLANIFIABLE",
                        "La demande " + demande.getNumero() + " n'est pas acceptée ou en cours.");
            }
            if (LigneTravailAssembleur.terminee(ligne)) {
                throw new BusinessRuleException("ANALYSE_TERMINEE",
                        "L'analyse " + libelle(ligne) + " est déjà terminée.");
            }
            String domaine = ligne.getEssai() == null || ligne.getEssai().getDomaine() == null
                    ? null : ligne.getEssai().getDomaine().getCode();
            if (!domaines.isEmpty() && domaine != null && !domaines.contains(domaine)) {
                throw new BusinessRuleException("DOMAINE_NON_COUVERT",
                        nomComplet(technicien) + " ne couvre pas le domaine de l'analyse " + libelle(ligne) + ".");
            }
            Long avant = ligne.getTechnicienId();
            ligne.setTechnicienId(technicien.getId());
            ligne.setDateAttribution(maintenant);
            if (datePrevue != null) {
                ligne.setDateDebut(datePrevue);
            }
            ligneEssaiRepository.save(ligne);
            demandes.add(demande);
            journalAuditService.enregistrer("AFFECTER_ANALYSE", "LigneEssai", ligne.getId(), acteur.getEmail(),
                    avant == null ? null : "{\"technicienId\":" + avant + "}",
                    "{\"technicienId\":" + technicien.getId() + "}", null, laboratoireId);
        }
        for (Demande demande : demandes) {
            if (StatutDemande.depuis(demande.getStatut()) == StatutDemande.ACCEPTEE) {
                demande.setStatut(StatutDemande.EN_COURS.name());
                demandeRepository.save(demande);
            }
        }
        return assembleur.assembler(ligneEssaiRepository.findAvecDetails(new HashSet<>(request.ligneIds())));
    }

    @Transactional
    public PlanificationDto changerStatut(Long id, StatutPlanification statut) {
        LigneEssai ligne = charger(id);
        ligne.setStatut(CorrespondanceStatutLigne.versLigne(statut));
        if (statut == StatutPlanification.EN_COURS && ligne.getDateDebut() == null) {
            ligne.setDateDebut(Instant.now());
        }
        if (statut == StatutPlanification.TERMINEE) {
            RoleUtilisateur role = SecurityUtils.principalCourant().getRole();
            if (role != RoleUtilisateur.RESPONSABLE && role != RoleUtilisateur.ADMINISTRATEUR) {
                throw new BusinessRuleException("DECISION_RESERVEE_AU_ROLE",
                        "Le passage d'une planification à terminée est réservé au responsable ou à l'administrateur.");
            }
            ligne.setDateFin(Instant.now());
        }
        return toDto(ligneEssaiRepository.save(ligne));
    }

    Map<Long, List<String>> domainesDes(Collection<Long> utilisateurs) {
        if (utilisateurs.isEmpty()) {
            return Map.of();
        }
        return tenantExecutor.inCentral(() -> utilisateurDomaineRepository.findByUtilisateurIdIn(utilisateurs).stream()
                .collect(Collectors.groupingBy(UtilisateurDomaine::getUtilisateurId,
                        Collectors.mapping(UtilisateurDomaine::getCodeDomaine, Collectors.toList()))));
    }

    private static Long laboratoireCourant() {
        Long laboratoireId = SecurityUtils.principalCourant().getLaboratoireId();
        if (laboratoireId == null) {
            throw new BusinessRuleException("SANS_LABORATOIRE", "Compte non rattaché à un laboratoire.");
        }
        return laboratoireId;
    }

    private LigneEssai charger(Long id) {
        return ligneEssaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
    }

    private static String libelle(LigneEssai ligne) {
        return ligne.getEssai() == null ? ligne.getCode() : ligne.getEssai().getDesignation();
    }

    private static String nomComplet(Utilisateur u) {
        String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
        return (prenom + " " + (u.getNom() == null ? "" : u.getNom().trim())).trim();
    }

    private PlanificationDto toDto(LigneEssai ligne) {
        return PlanificationDto.builder()
                .id(ligne.getId())
                .demandeId(ligne.getDemande() == null ? null : ligne.getDemande().getId())
                .technicienId(ligne.getTechnicienId())
                .dateDebutPrevue(toLocalDate(ligne.getDateDebut()))
                .dateFinPrevue(toLocalDate(ligne.getDateFin()))
                .dateDebutEffective(ligne.getDateDebut())
                .dateFinEffective(ligne.getDateFin())
                .statut(CorrespondanceStatutLigne.versPlanification(ligne.getStatut()))
                .notes(ligne.getMotif())
                .build();
    }

    private static LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }
}
