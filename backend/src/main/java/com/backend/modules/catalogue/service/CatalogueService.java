package com.backend.modules.catalogue.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.catalogue.dto.AnalyseTypeDto;
import com.backend.modules.catalogue.entity.AnalyseType;
import com.backend.modules.catalogue.repository.AnalyseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion du catalogue des analyses (M04).
 * TODO: catégories, accréditations par analyse, tarifs clients spéciaux.
 */
@Service
@RequiredArgsConstructor
public class CatalogueService {

    private final AnalyseTypeRepository analyseTypeRepository;

    @Transactional(readOnly = true)
    public Page<AnalyseTypeDto> lister(boolean actifSeulement, Pageable pageable) {
        return (actifSeulement
                ? analyseTypeRepository.findByActif(true, pageable)
                : analyseTypeRepository.findAll(pageable)).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AnalyseTypeDto trouverParCode(String code) {
        return toDto(analyseTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("AnalyseType", "code", code)));
    }

    @Transactional
    public AnalyseTypeDto creer(AnalyseTypeDto dto) {
        if (analyseTypeRepository.existsByCode(dto.getCode()))
            throw new BusinessRuleException("CODE_ANALYSE_EXISTANT", "Code déjà utilisé : " + dto.getCode());
        return toDto(analyseTypeRepository.save(AnalyseType.builder()
                .code(dto.getCode()).designation(dto.getDesignation()).description(dto.getDescription())
                .normeReference(dto.getNormeReference()).delaiJours(dto.getDelaiJours())
                .prixUnitaire(dto.getPrixUnitaire()).uniteMesure(dto.getUniteMesure()).build()));
    }

    @Transactional
    public AnalyseTypeDto desactiver(Long id) {
        AnalyseType at = analyseTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AnalyseType", "id", id));
        at.setActif(false);
        return toDto(analyseTypeRepository.save(at));
    }

    private AnalyseTypeDto toDto(AnalyseType a) {
        return AnalyseTypeDto.builder().id(a.getId()).code(a.getCode()).designation(a.getDesignation())
                .description(a.getDescription()).normeReference(a.getNormeReference())
                .delaiJours(a.getDelaiJours()).prixUnitaire(a.getPrixUnitaire())
                .uniteMesure(a.getUniteMesure()).actif(a.isActif()).build();
    }
}
