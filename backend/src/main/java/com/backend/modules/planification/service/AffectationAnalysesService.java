package com.backend.modules.planification.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
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
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.utilisateur.service.TechnicienDomaineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AffectationAnalysesService {

    private final LigneEssaiRepository ligneEssaiRepository;
    private final DemandeRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantExecutor tenantExecutor;
    private final LigneTravailAssembleur assembleur;
    private final JournalAuditService journalAuditService;
    private final TechnicienDomaineService technicienDomaineService;
    private final EtatPaiementService etatPaiementService;

    @Transactional
    public List<LigneTravailDto> affecter(AffecterAnalysesRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        Long laboratoireId = acteur.getLaboratoireId();
        if (laboratoireId == null) {
            throw new BusinessRuleException("SANS_LABORATOIRE", "Compte non rattaché à un laboratoire.");
        }
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
        List<String> domaines = technicienDomaineService.domainesDes(List.of(technicien.getId()))
                .getOrDefault(technicien.getId(), List.of());
        if (domaines.isEmpty()) {
            throw new BusinessRuleException("TECHNICIEN_SANS_DOMAINE",
                    PlanificationService.nomComplet(technicien)
                            + " n'a aucun domaine d'analyse : demandez à l'administrateur de lui en attribuer.");
        }
        String motif = request.motif() == null || request.motif().isBlank() ? null : request.motif().trim();
        Set<Long> ligneIds = new HashSet<>(request.ligneIds());
        List<LigneEssai> lignes = ligneEssaiRepository.findAvecDetails(ligneIds);
        if (lignes.size() != ligneIds.size()) {
            throw new ResourceNotFoundException("Analyse introuvable");
        }
        Instant maintenant = Instant.now();
        Instant datePrevue = request.datePrevue() == null ? null
                : request.datePrevue().atStartOfDay(java.time.ZoneId.of("Africa/Casablanca")).toInstant();
        if (request.datePrevue() != null
                && request.datePrevue().isBefore(LocalDate.now(java.time.ZoneId.of("Africa/Casablanca")))) {
            throw new BusinessRuleException("DATE_PREVUE_PASSEE", "La date prévue ne peut pas être dans le passé.");
        }
        Set<Demande> demandes = new HashSet<>();
        Map<Long, String> paiements = etatPaiementService.etatsPaiement(
                lignes.stream().map(ligne -> ligne.getDemande().getId()).toList());
        for (LigneEssai ligne : lignes) {
            Demande demande = ligne.getDemande();
            if (!PlanificationService.STATUTS_DEMANDES_ACTIVES.contains(demande.getStatut())) {
                throw new BusinessRuleException("DEMANDE_NON_PLANIFIABLE",
                        "La demande " + demande.getNumero() + " n'est pas acceptée ou en cours.");
            }
            if (LigneTravailAssembleur.terminee(ligne)) {
                throw new BusinessRuleException("ANALYSE_TERMINEE",
                        "L'analyse " + PlanificationService.libelle(ligne) + " est déjà terminée.");
            }
            String domaine = ligne.getEssai() == null || ligne.getEssai().getDomaine() == null
                    ? null : ligne.getEssai().getDomaine().getCode();
            if (domaine == null || !domaines.contains(domaine)) {
                throw new BusinessRuleException("DOMAINE_NON_COUVERT",
                        PlanificationService.nomComplet(technicien) + " ne couvre pas le domaine de l'analyse "
                                + PlanificationService.libelle(ligne) + ".");
            }
            if (ligne.getEchantillon() == null || !Boolean.TRUE.equals(ligne.getEchantillon().getConformite())) {
                throw new BusinessRuleException("ECHANTILLON_ABSENT",
                        "L'analyse " + PlanificationService.libelle(ligne)
                                + " n'a pas d'échantillon conforme reçu : affectation impossible.");
            }
            String paiement = paiements.getOrDefault(demande.getId(), EtatPaiementService.AUCUNE_FACTURE);
            if (!EtatPaiementService.PARTIELLE.equals(paiement)
                    && !EtatPaiementService.PAYEE.equals(paiement)) {
                throw new BusinessRuleException("PAIEMENT_ABSENT",
                        "La demande " + demande.getNumero()
                                + " n'a reçu aucun paiement : affectation impossible (un paiement partiel suffit).");
            }
            Long avant = ligne.getTechnicienId();
            boolean reaffectation = avant != null && !avant.equals(technicien.getId());
            if (reaffectation && motif == null) {
                throw new BusinessRuleException("MOTIF_REAFFECTATION_OBLIGATOIRE",
                        "L'analyse " + PlanificationService.libelle(ligne)
                                + " est déjà affectée : indiquez le motif de la réaffectation.");
            }
            ligne.setTechnicienId(technicien.getId());
            ligne.setDateAttribution(maintenant);
            if (datePrevue != null) {
                ligne.setDateDebut(datePrevue);
            }
            ligneEssaiRepository.save(ligne);
            demandes.add(demande);
            journalAuditService.enregistrer(reaffectation ? "REAFFECTER_ANALYSE" : "AFFECTER_ANALYSE",
                    "LigneEssai", ligne.getId(), acteur.getEmail(),
                    avant == null ? null : "{\"technicienId\":" + avant + "}",
                    "{\"technicienId\":" + technicien.getId() + "}", reaffectation ? motif : null, laboratoireId);
        }
        for (Demande demande : demandes) {
            if (StatutDemande.depuis(demande.getStatut()) == StatutDemande.ACCEPTEE) {
                demande.setStatut(StatutDemande.EN_COURS.name());
                demandeRepository.save(demande);
            }
        }
        return assembleur.assembler(ligneEssaiRepository.findAvecDetails(ligneIds));
    }
}
