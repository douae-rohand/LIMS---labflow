package com.backend.modules.conformite.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.conformite.dto.NonConformiteDto;
import com.backend.modules.conformite.entity.NonConformite;
import com.backend.modules.conformite.entity.StatutNonConformite;
import com.backend.modules.conformite.repository.NonConformiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

/**
 * Service de gestion de la conformité et des non-conformités (M13).
 * TODO: workflow d'approbation, indicateurs qualité, export ISO 17025.
 */
@Service @RequiredArgsConstructor
public class ConformiteService {

    private final NonConformiteRepository nonConformiteRepository;

    @Transactional(readOnly = true)
    public Page<NonConformiteDto> lister(StatutNonConformite statut, Pageable pageable) {
        return (statut != null
                ? nonConformiteRepository.findByStatut(statut, pageable)
                : nonConformiteRepository.findAll(pageable)).map(this::toDto);
    }

    @Transactional
    public NonConformiteDto declarer(NonConformiteDto dto) {
        NonConformite nc = NonConformite.builder()
                .reference("NC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .titre(dto.getTitre()).description(dto.getDescription()).gravite(dto.getGravite())
                .entiteSource(dto.getEntiteSource()).idEntiteSource(dto.getIdEntiteSource())
                .declarantId(dto.getDeclarantId()).build();
        return toDto(nonConformiteRepository.save(nc));
    }

    @Transactional
    public NonConformiteDto traiter(Long id, String actionCorrective, Long responsableId) {
        NonConformite nc = nonConformiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NonConformite", "id", id));
        nc.setActionCorrective(actionCorrective);
        nc.setResponsableTraitementId(responsableId);
        nc.setStatut(StatutNonConformite.EN_TRAITEMENT);
        return toDto(nonConformiteRepository.save(nc));
    }

    @Transactional
    public NonConformiteDto cloturer(Long id) {
        NonConformite nc = nonConformiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NonConformite", "id", id));
        nc.setStatut(StatutNonConformite.CLOTUREE);
        nc.setDateCloture(Instant.now());
        return toDto(nonConformiteRepository.save(nc));
    }

    private NonConformiteDto toDto(NonConformite nc) {
        return NonConformiteDto.builder().id(nc.getId()).reference(nc.getReference())
                .titre(nc.getTitre()).description(nc.getDescription()).gravite(nc.getGravite())
                .statut(nc.getStatut()).entiteSource(nc.getEntiteSource()).idEntiteSource(nc.getIdEntiteSource())
                .declarantId(nc.getDeclarantId()).responsableTraitementId(nc.getResponsableTraitementId())
                .actionCorrective(nc.getActionCorrective()).dateDetection(nc.getDateDetection())
                .dateCloture(nc.getDateCloture()).build();
    }
}
