package com.backend.modules.planification.service;

import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.planification.dto.SuiviResponsableDto;
import com.backend.modules.rapport.dto.RapportTravailDto;
import com.backend.modules.rapport.entity.StatutRapport;
import com.backend.modules.rapport.service.RapportService;
import com.backend.modules.validation.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final ValidationService validationService;
    private final RapportService rapportService;

    @Transactional(readOnly = true)
    public SuiviResponsableDto suivi() {
        long aDecider = StatutDemande.NOUVEAU.aliasesPersistés().stream()
                .mapToLong(demandeRepository::countByStatut)
                .sum();
        Instant maintenant = Instant.now();
        List<LigneEssai> lignes = ligneEssaiRepository.findLignesDesDemandes(PlanificationService.STATUTS_DEMANDES_ACTIVES);
        long aAffecter = lignes.stream()
                .filter(l -> l.getTechnicienId() == null && !LigneTravailAssembleur.terminee(l))
                .count();
        long enCours = lignes.stream()
                .filter(l -> l.getTechnicienId() != null && !LigneTravailAssembleur.terminee(l))
                .count();
        List<LigneEssai> enRetard = lignes.stream()
                .filter(l -> LigneTravailAssembleur.enRetard(l, maintenant))
                .toList();
        long aValider = validationService.resultatsAValider().size();
        long aPreparer = rapportService.travail().stream()
                .map(RapportTravailDto::statut)
                .filter(statut -> statut == null || statut == StatutRapport.GENERE || statut == StatutRapport.BROUILLON)
                .count();
        List<LigneTravailDto> retards = assembleur.assembler(enRetard).stream()
                .sorted(Comparator.comparingLong(LigneTravailDto::getJoursRetard).reversed())
                .limit(20)
                .toList();
        return new SuiviResponsableDto(
                new SuiviResponsableDto.Indicateurs(aDecider, aAffecter, enCours, enRetard.size(), aValider, aPreparer),
                planificationService.techniciens(),
                retards);
    }
}
