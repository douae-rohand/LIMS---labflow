package com.backend.modules.validation.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.validation.dto.ValidationDto;
import com.backend.modules.validation.entity.StatutValidation;
import com.backend.modules.validation.entity.Validation;
import com.backend.modules.validation.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ValidationRepository validationRepository;
    private final LigneEssaiRepository ligneEssaiRepository;

    @Transactional(readOnly = true)
    public List<ValidationDto> listerParEssai(Long essaiId) {
        return validationRepository.findByLigneEssai_Id(essaiId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ValidationDto valider(Long id, Long validateurId, String commentaire, StatutValidation decision) {
        Validation validation = validationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Validation", "id", id));
        validation.setValidateurId(validateurId);
        validation.setDecision(decision.name());
        validation.setMotif(commentaire);
        validation.setDateValidation(Instant.now());
        return toDto(validationRepository.save(validation));
    }

    @Transactional
    public ValidationDto creer(ValidationDto dto) {
        LigneEssai ligne = ligneEssaiRepository.findById(dto.getEssaiId())
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", dto.getEssaiId()));
        Validation validation = Validation.builder()
                .code("VAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .niveau(1)
                .decision(dto.getStatut() == null ? null : dto.getStatut().name())
                .motif(dto.getCommentaire())
                .dateValidation(Instant.now())
                .ligneEssai(ligne)
                .validateurId(dto.getValidateurId())
                .build();
        return toDto(validationRepository.save(validation));
    }

    private ValidationDto toDto(Validation validation) {
        return ValidationDto.builder()
                .id(validation.getId())
                .essaiId(validation.getLigneEssai() == null ? null : validation.getLigneEssai().getId())
                .validateurId(validation.getValidateurId())
                .statut(enumOuNull(StatutValidation.class, validation.getDecision()))
                .commentaire(validation.getMotif())
                .dateValidation(validation.getDateValidation())
                .build();
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
