package com.backend.modules.planification.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.planification.dto.PlanificationDto;
import com.backend.modules.planification.entity.StatutPlanification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanificationService {

    private final LigneEssaiRepository ligneEssaiRepository;
    private final DemandeRepository demandeRepository;
    private final EssaiRepository essaiRepository;

    @Transactional(readOnly = true)
    public PlanificationDto trouverParId(Long id) {
        return toDto(charger(id));
    }

    @Transactional(readOnly = true)
    public List<PlanificationDto> listerParDemande(Long demandeId) {
        return ligneEssaiRepository.findByDemande_Id(demandeId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<PlanificationDto> listerParTechnicien(Long technicienId, Pageable pageable) {
        return ligneEssaiRepository.findByTechnicienId(technicienId, pageable).map(this::toDto);
    }

    @Transactional
    public PlanificationDto creer(PlanificationDto dto) {
        Demande demande = demandeRepository.findById(dto.getDemandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", dto.getDemandeId()));
        Essai essai = essaiRepository.findFirstByActifTrue()
                .orElseThrow(() -> new BusinessRuleException("ESSAI_CATALOGUE_ABSENT",
                        "Aucun essai actif dans le catalogue"));
        LigneEssai ligne = LigneEssai.builder()
                .code("PLN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .statut(dto.getStatut() == null ? StatutPlanification.PLANIFIEE.name() : dto.getStatut().name())
                .montant(BigDecimal.ZERO)
                .demande(demande)
                .essai(essai)
                .technicienId(dto.getTechnicienId())
                .dateDebut(toInstant(dto.getDateDebutPrevue()))
                .dateFin(toInstant(dto.getDateFinPrevue()))
                .motif(dto.getNotes())
                .build();
        return toDto(ligneEssaiRepository.save(ligne));
    }

    @Transactional
    public PlanificationDto changerStatut(Long id, StatutPlanification statut) {
        LigneEssai ligne = charger(id);
        ligne.setStatut(statut.name());
        if (statut == StatutPlanification.EN_COURS && ligne.getDateDebut() == null) {
            ligne.setDateDebut(Instant.now());
        }
        if (statut == StatutPlanification.TERMINEE) {
            ligne.setDateFin(Instant.now());
        }
        return toDto(ligneEssaiRepository.save(ligne));
    }

    private LigneEssai charger(Long id) {
        return ligneEssaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
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
                .statut(enumOuNull(StatutPlanification.class, ligne.getStatut()))
                .notes(ligne.getMotif())
                .build();
    }

    private static Instant toInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, ZoneOffset.UTC);
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
