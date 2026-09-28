package com.backend.modules.planification.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.planification.dto.PlanificationDto;
import com.backend.modules.planification.entity.Planification;
import com.backend.modules.planification.entity.StatutPlanification;
import com.backend.modules.planification.repository.PlanificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de planification des analyses (M02).
 * TODO: gestion des conflits de ressources, calendrier, notifications technicien.
 */
@Service
@RequiredArgsConstructor
public class PlanificationService {

    private final PlanificationRepository planificationRepository;

    @Transactional(readOnly = true)
    public PlanificationDto trouverParId(Long id) {
        return toDto(planificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Planification", "id", id)));
    }

    @Transactional(readOnly = true)
    public List<PlanificationDto> listerParDemande(Long demandeId) {
        return planificationRepository.findByDemandeId(demandeId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Page<PlanificationDto> listerParTechnicien(Long technicienId, Pageable pageable) {
        return planificationRepository.findByTechnicienId(technicienId, pageable).map(this::toDto);
    }

    @Transactional
    public PlanificationDto creer(PlanificationDto dto) {
        Planification p = Planification.builder()
                .demandeId(dto.getDemandeId()).technicienId(dto.getTechnicienId())
                .dateDebutPrevue(dto.getDateDebutPrevue()).dateFinPrevue(dto.getDateFinPrevue())
                .notes(dto.getNotes()).build();
        return toDto(planificationRepository.save(p));
    }

    @Transactional
    public PlanificationDto changerStatut(Long id, StatutPlanification statut) {
        Planification p = planificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Planification", "id", id));
        p.setStatut(statut);
        return toDto(planificationRepository.save(p));
    }

    private PlanificationDto toDto(Planification p) {
        return PlanificationDto.builder()
                .id(p.getId()).demandeId(p.getDemandeId()).technicienId(p.getTechnicienId())
                .dateDebutPrevue(p.getDateDebutPrevue()).dateFinPrevue(p.getDateFinPrevue())
                .dateDebutEffective(p.getDateDebutEffective()).dateFinEffective(p.getDateFinEffective())
                .statut(p.getStatut()).notes(p.getNotes()).build();
    }
}
