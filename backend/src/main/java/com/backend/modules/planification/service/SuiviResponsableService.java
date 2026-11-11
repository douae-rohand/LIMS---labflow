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

/** Vue d'ensemble du responsable : file de travail, charge de l'équipe, retards. */
@Service
@RequiredArgsConstructor
public class SuiviResponsableService {

    private final DemandeRepository demandeRepository;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final LigneTravailAssembleur assembleur;
    private final PlanificationService planificationService;
    private final RapportService rapportService;

    @Transactional(readOnly = true)
    public SuiviResponsableDto suivi() {
        long aDecider = demandeRepository.findIdsParStatuts(StatutDemande.NOUVEAU.aliasesPersistés()).size();
        // Une seule lecture des analyses, de leurs validations et des noms : tout le reste en découle.
        List<LigneEssai> lignes = ligneEssaiRepository.findLignesDesDemandes(PlanificationService.STATUTS_DEMANDES_ACTIVES);
        List<LigneTravailDto> analyses = assembleur.assembler(lignes);
        long aAffecter = analyses.stream()
                .filter(a -> a.getTechnicienId() == null && !termineeOuNonRealisable(a))
                .count();
        long demandesAAffecter = analyses.stream()
                .filter(a -> a.getTechnicienId() == null && !termineeOuNonRealisable(a))
                .map(LigneTravailDto::getDemandeId)
                .distinct()
                .count();
        long enCours = analyses.stream()
                .filter(a -> a.getTechnicienId() != null && !termineeOuNonRealisable(a))
                .count();
        List<LigneTravailDto> enRetard = analyses.stream().filter(LigneTravailDto::isEnRetard).toList();
        long aValider = analyses.stream()
                .filter(a -> a.getStatut() == StatutEssai.TERMINE
                        && LigneTravailAssembleur.TECHNIQUE_VALIDEE.equals(a.getEtatValidation()))
                .count();
        long aPreparer = rapportService.compterAPreparer();
        List<LigneTravailDto> retards = enRetard.stream()
                .sorted(Comparator.comparingLong(LigneTravailDto::getJoursRetard).reversed())
                .limit(20)
                .toList();
        return new SuiviResponsableDto(
                new SuiviResponsableDto.Indicateurs(aDecider, aAffecter, enCours, enRetard.size(), aValider, aPreparer,
                        demandesAAffecter),
                planificationService.techniciens(lignes),
                retards);
    }

    private static boolean termineeOuNonRealisable(LigneTravailDto analyse) {
        return analyse.getStatut() == StatutEssai.TERMINE || analyse.getStatut() == StatutEssai.NON_REALISABLE;
    }
}
