package com.backend.modules.planification.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.ProprieteClient;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.demande.entity.StatutDemande;
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
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.utilisateur.service.TechnicienDomaineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

/**
 * Planification des analyses : affectation des lignes d'essai aux techniciens
 * du laboratoire et suivi de leur charge.
 */
@Service
@RequiredArgsConstructor
public class PlanificationService {

    /** Demandes dont les analyses peuvent être planifiées ou suivies. */
    public static final List<String> STATUTS_DEMANDES_ACTIVES = Stream.of(
                    StatutDemande.ACCEPTEE, StatutDemande.EN_COURS, StatutDemande.EN_VALIDATION)
            .flatMap(statut -> statut.aliasesPersistés().stream())
            .toList();

    private final LigneEssaiRepository ligneEssaiRepository;
    private final ProprieteClient proprieteClient;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantExecutor tenantExecutor;
    private final LigneTravailAssembleur assembleur;
    private final TechnicienDomaineService technicienDomaineService;
    private final EtatPaiementService etatPaiementService;
    private final AffectationAnalysesService affectationAnalysesService;

    public static final String PAIEMENT_AUCUNE_FACTURE = EtatPaiementService.AUCUNE_FACTURE;
    public static final String PAIEMENT_NON_PAYEE = EtatPaiementService.NON_PAYEE;
    public static final String PAIEMENT_PARTIELLE = EtatPaiementService.PARTIELLE;
    public static final String PAIEMENT_PAYEE = EtatPaiementService.PAYEE;

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
        List<LigneTravailDto> analyses = assembleur.assembler(
                        ligneEssaiRepository.findLignesDesDemandes(STATUTS_DEMANDES_ACTIVES)).stream()
                .filter(analyse -> switch (filtre == null ? "" : filtre) {
                    case "A_AFFECTER" -> (analyse.getTechnicienId() == null || analyse.isTechnicienDesactive())
                            && !terminee(analyse.getStatut());
                    case "AFFECTEES" -> analyse.getTechnicienId() != null && !terminee(analyse.getStatut());
                    case "EN_RETARD" -> analyse.isEnRetard();
                    default -> true;
                })
                .toList();
        Map<Long, String> paiements = etatPaiementService.etatsPaiement(
                analyses.stream().map(LigneTravailDto::getDemandeId).toList());
        analyses.forEach(a -> a.setEtatPaiement(paiements.getOrDefault(a.getDemandeId(), PAIEMENT_AUCUNE_FACTURE)));
        return analyses;
    }

    private static boolean terminee(com.backend.modules.essai.entity.StatutEssai statut) {
        return statut == com.backend.modules.essai.entity.StatutEssai.TERMINE
                || statut == com.backend.modules.essai.entity.StatutEssai.NON_REALISABLE;
    }

    /** État de paiement de la facture de chaque demande (une requête pour toutes). */
    public Map<Long, String> etatsPaiement(java.util.Collection<Long> demandeIds) {
        return etatPaiementService.etatsPaiement(demandeIds);
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
        return affectationAnalysesService.affecter(request);
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
        return technicienDomaineService.domainesDes(utilisateurs);
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

    static String libelle(LigneEssai ligne) {
        return ligne.getEssai() == null ? ligne.getCode() : ligne.getEssai().getDesignation();
    }

    static String nomComplet(Utilisateur u) {
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
        return instant == null ? null : LocalDate.ofInstant(instant, java.time.ZoneId.of("Africa/Casablanca"));
    }
}
