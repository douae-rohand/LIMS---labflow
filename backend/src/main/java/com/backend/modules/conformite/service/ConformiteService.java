package com.backend.modules.conformite.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.conformite.dto.NonConformiteDto;
import com.backend.modules.conformite.entity.StatutNonConformite;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.echantillon.repository.EchantillonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConformiteService {

    private final EchantillonRepository echantillonRepository;

    @Transactional(readOnly = true)
    public Page<NonConformiteDto> lister(StatutNonConformite statut, Pageable pageable) {
        boolean ouverte = statut == null || statut == StatutNonConformite.OUVERTE || statut == StatutNonConformite.EN_TRAITEMENT;
        Page<Echantillon> page = ouverte
                ? echantillonRepository.findByConformite(false, pageable)
                : echantillonRepository.findByConformite(true, pageable);
        return page.map(echantillon -> toDto(echantillon, statut));
    }

    @Transactional
    public NonConformiteDto declarer(NonConformiteDto dto) {
        if (dto.getIdEntiteSource() == null) {
            throw new ResourceNotFoundException("Echantillon", "id", null);
        }
        Echantillon echantillon = echantillonRepository.findById(dto.getIdEntiteSource())
                .orElseThrow(() -> new ResourceNotFoundException("Echantillon", "id", dto.getIdEntiteSource()));
        echantillon.setConformite(false);
        echantillon.setMotif(dto.getDescription() != null ? dto.getDescription() : dto.getTitre());
        return toDto(echantillonRepository.save(echantillon), StatutNonConformite.OUVERTE);
    }

    @Transactional
    public NonConformiteDto traiter(Long id, String actionCorrective, Long responsableId) {
        Echantillon echantillon = charger(id);
        echantillon.setMotif(actionCorrective);
        echantillon.setConformite(false);
        return toDto(echantillonRepository.save(echantillon), StatutNonConformite.EN_TRAITEMENT);
    }

    @Transactional
    public NonConformiteDto cloturer(Long id) {
        Echantillon echantillon = charger(id);
        echantillon.setConformite(true);
        return toDto(echantillonRepository.save(echantillon), StatutNonConformite.CLOTUREE);
    }

    private Echantillon charger(Long id) {
        return echantillonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Echantillon", "id", id));
    }

    private NonConformiteDto toDto(Echantillon echantillon, StatutNonConformite statut) {
        StatutNonConformite effectif = statut;
        if (effectif == null) {
            effectif = Boolean.FALSE.equals(echantillon.getConformite())
                    ? StatutNonConformite.OUVERTE
                    : StatutNonConformite.CLOTUREE;
        }
        return NonConformiteDto.builder()
                .id(echantillon.getId())
                .reference(echantillon.getReference())
                .titre(echantillon.getNature())
                .description(echantillon.getMotif())
                .statut(effectif)
                .build();
    }
}
