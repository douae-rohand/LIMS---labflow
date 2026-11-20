package com.backend.modules.planification.service;

import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.planification.dto.SuiviResponsableDto;
import com.backend.modules.rapport.service.RapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Vue d'ensemble du responsable : file de travail, charge de l'équipe, retards. */
@Service
@RequiredArgsConstructor
public class SuiviResponsableService {

    private final DemandeRepository demandeRepository;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final LigneTravailAssembleur assembleur;
    private final PlanificationService planificationService;
    private final RapportService rapportService;
    private final com.backend.modules.ia.anomalie.AnomalieService anomalieService;

    @Transactional(readOnly = true)
    public SuiviResponsableDto suivi() {
        long aDecider = demandeRepository.findIdsParStatuts(StatutDemande.NOUVEAU.aliasesPersistés()).size();
        // Une seule lecture des analyses, de leurs validations et des noms : tout le reste en découle.
        List<LigneEssai> lignes = ligneEssaiRepository.findLignesDesDemandes(PlanificationService.STATUTS_DEMANDES_ACTIVES);
        List<LigneTravailDto> analyses = assembleur.assembler(lignes);
        long aAffecter = analyses.stream()
                .filter(a -> a.getTechnicienId() == null && !termineeOuNonRealisable(a))
                .count();
        long aReaffecter = analyses.stream()
                .filter(a -> a.getTechnicienId() != null && a.isTechnicienDesactive()
                        && !termineeOuNonRealisable(a))
                .count();
        // Demandes dont au moins une analyse est prête à affecter : échantillon conforme et paiement reçu.
        Map<Long, String> paiements = planificationService.etatsPaiement(
                analyses.stream().map(LigneTravailDto::getDemandeId).distinct().toList());
        long demandesAAffecter = analyses.stream()
                .filter(a -> a.getTechnicienId() == null && !termineeOuNonRealisable(a))
                .filter(a -> "CONFORME".equals(a.getEtatEchantillon()))
                .filter(a -> {
                    String paiement = paiements.get(a.getDemandeId());
                    return PlanificationService.PAIEMENT_PARTIELLE.equals(paiement)
                            || PlanificationService.PAIEMENT_PAYEE.equals(paiement);
                })
                .map(LigneTravailDto::getDemandeId)
                .distinct()
                .count();
        long enCours = analyses.stream()
                .filter(a -> a.getTechnicienId() != null && !termineeOuNonRealisable(a))
                .count();
        List<LigneTravailDto> enRetard = analyses.stream().filter(LigneTravailDto::isEnRetard).toList();
        List<LigneTravailDto> resultatsAValider = analyses.stream()
                .filter(a -> a.getStatut() == StatutEssai.TERMINE
                        && LigneTravailAssembleur.TECHNIQUE_VALIDEE.equals(a.getEtatValidation()))
                .toList();
        long aValider = resultatsAValider.size();
        // Alertes anomalies (CDC M08) : scores calculés sur les résultats qui attendent la décision du responsable
        java.util.Set<Long> idsAValider = resultatsAValider.stream().map(LigneTravailDto::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<LigneTravailDto> alertes = anomalieService.completer(resultatsAValider,
                lignes.stream().filter(l -> idsAValider.contains(l.getId())).toList());
        long aPreparer = rapportService.compterAPreparer();
        List<LigneTravailDto> retards = enRetard.stream()
                .sorted(Comparator.comparingLong(LigneTravailDto::getJoursRetard).reversed())
                .limit(20)
                .toList();
        return new SuiviResponsableDto(
                new SuiviResponsableDto.Indicateurs(aDecider, aAffecter, enCours, enRetard.size(), aValider, aPreparer,
                        aReaffecter, demandesAAffecter, alertes.size()),
                planificationService.techniciens(lignes),
                retards,
                alertes.stream().limit(10).toList());
    }

    private static boolean termineeOuNonRealisable(LigneTravailDto analyse) {
        return analyse.getStatut() == StatutEssai.TERMINE || analyse.getStatut() == StatutEssai.NON_REALISABLE;
    }
}
