package com.backend.modules.validation.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.validation.dto.ValidationDto;
import com.backend.modules.validation.entity.Validation;
import com.backend.modules.validation.entity.StatutValidation;
import com.backend.modules.validation.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

/**
 * Service de validation des résultats (M06).
 * TODO: workflow de double validation, signature électronique.
 */
@Service @RequiredArgsConstructor
public class ValidationService {

    private final ValidationRepository validationRepository;

    @Transactional(readOnly = true)
    public List<ValidationDto> listerParEssai(Long essaiId) {
        return validationRepository.findByEssaiId(essaiId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ValidationDto valider(Long id, Long validateurId, String commentaire, StatutValidation decision) {
        Validation v = validationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Validation", "id", id));
        v.setValidateurId(validateurId);
        v.setStatut(decision);
        v.setCommentaire(commentaire);
        v.setDateValidation(Instant.now());
        return toDto(validationRepository.save(v));
    }

    @Transactional
    public ValidationDto creer(ValidationDto dto) {
        Validation v = Validation.builder().essaiId(dto.getEssaiId()).validateurId(dto.getValidateurId()).build();
        return toDto(validationRepository.save(v));
    }

    private ValidationDto toDto(Validation v) {
        return ValidationDto.builder().id(v.getId()).essaiId(v.getEssaiId()).validateurId(v.getValidateurId())
                .statut(v.getStatut()).commentaire(v.getCommentaire()).dateValidation(v.getDateValidation()).build();
    }
}
