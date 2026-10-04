package com.backend.modules.catalogue.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.catalogue.dto.AnalyseTypeDto;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.repository.EssaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogueService {

    private final EssaiRepository essaiRepository;

    @Transactional(readOnly = true)
    public Page<AnalyseTypeDto> lister(boolean actifSeulement, Pageable pageable) {
        Page<Essai> page = actifSeulement
                ? essaiRepository.findByActif(true, pageable)
                : essaiRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AnalyseTypeDto trouverParCode(String code) {
        return toDto(essaiRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "code", code)));
    }

    @Transactional
    public AnalyseTypeDto creer(AnalyseTypeDto dto) {
        if (essaiRepository.existsByCode(dto.getCode())) {
            throw new BusinessRuleException("CODE_ANALYSE_EXISTANT", "Code déjà utilisé : " + dto.getCode());
        }
        Essai essai = Essai.builder()
                .code(dto.getCode())
                .designation(dto.getDesignation())
                .description(dto.getDescription())
                .tarif(dto.getPrixUnitaire())
                .dureeEstimee(dto.getDelaiJours() == null ? 0 : dto.getDelaiJours())
                .unite(dto.getUniteMesure())
                .actif(true)
                .build();
        return toDto(essaiRepository.save(essai));
    }

    @Transactional
    public AnalyseTypeDto desactiver(Long id) {
        Essai essai = essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id));
        essai.setActif(false);
        return toDto(essaiRepository.save(essai));
    }

    private AnalyseTypeDto toDto(Essai essai) {
        return AnalyseTypeDto.builder()
                .id(essai.getId())
                .code(essai.getCode())
                .designation(essai.getDesignation())
                .description(essai.getDescription())
                .delaiJours(essai.getDureeEstimee())
                .prixUnitaire(essai.getTarif())
                .uniteMesure(essai.getUnite())
                .actif(Boolean.TRUE.equals(essai.getActif()))
                .build();
    }
}
