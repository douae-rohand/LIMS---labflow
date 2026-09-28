package com.backend.modules.satisfaction.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.satisfaction.dto.SatisfactionDto;
import com.backend.modules.satisfaction.entity.Satisfaction;
import com.backend.modules.satisfaction.repository.SatisfactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des enquêtes de satisfaction (M10).
 * TODO: envoi automatique du questionnaire après livraison du rapport.
 */
@Service @RequiredArgsConstructor
public class SatisfactionService {

    private final SatisfactionRepository satisfactionRepository;

    @Transactional
    public SatisfactionDto soumettre(SatisfactionDto dto) {
        if (satisfactionRepository.existsByDemandeIdAndClientId(dto.getDemandeId(), dto.getClientId()))
            throw new BusinessRuleException("SATISFACTION_DEJA_SOUMISE",
                    "Vous avez déjà évalué cette demande");
        Satisfaction s = Satisfaction.builder().demandeId(dto.getDemandeId()).clientId(dto.getClientId())
                .note(dto.getNote()).commentaire(dto.getCommentaire())
                .noteDelai(dto.getNoteDelai()).noteQualite(dto.getNoteQualite())
                .noteCommunication(dto.getNoteCommunication()).build();
        return toDto(satisfactionRepository.save(s));
    }

    @Transactional(readOnly = true)
    public Double moyenneGlobale() {
        return satisfactionRepository.calculerMoyenneGlobale();
    }

    private SatisfactionDto toDto(Satisfaction s) {
        return SatisfactionDto.builder().id(s.getId()).demandeId(s.getDemandeId()).clientId(s.getClientId())
                .note(s.getNote()).commentaire(s.getCommentaire()).noteDelai(s.getNoteDelai())
                .noteQualite(s.getNoteQualite()).noteCommunication(s.getNoteCommunication())
                .dateReponse(s.getDateReponse()).build();
    }
}
