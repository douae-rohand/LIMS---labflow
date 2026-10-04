package com.backend.modules.satisfaction.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.satisfaction.dto.SatisfactionDto;
import com.backend.modules.satisfaction.entity.EnqueteSatisfaction;
import com.backend.modules.satisfaction.repository.EnqueteSatisfactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SatisfactionService {

    private final EnqueteSatisfactionRepository enqueteRepository;
    private final DemandeRepository demandeRepository;

    @Transactional
    public SatisfactionDto soumettre(SatisfactionDto dto) {
        if (enqueteRepository.existsByDemande_Id(dto.getDemandeId())) {
            throw new BusinessRuleException("SATISFACTION_DEJA_SOUMISE",
                    "Vous avez déjà évalué cette demande");
        }
        Demande demande = demandeRepository.findById(dto.getDemandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", dto.getDemandeId()));
        EnqueteSatisfaction enquete = EnqueteSatisfaction.builder()
                .code("SAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .dateReponse(Instant.now())
                .noteGlobale(dto.getNote())
                .noteDelai(dto.getNoteDelai())
                .noteClarte(dto.getNoteQualite())
                .noteRelation(dto.getNoteCommunication())
                .commentaire(dto.getCommentaire())
                .demande(demande)
                .build();
        return toDto(enqueteRepository.save(enquete), dto.getClientId());
    }

    @Transactional(readOnly = true)
    public Double moyenneGlobale() {
        return enqueteRepository.calculerMoyenneGlobale();
    }

    private SatisfactionDto toDto(EnqueteSatisfaction enquete, Long clientId) {
        return SatisfactionDto.builder()
                .id(enquete.getId())
                .demandeId(enquete.getDemande() == null ? null : enquete.getDemande().getId())
                .clientId(clientId)
                .note(enquete.getNoteGlobale())
                .commentaire(enquete.getCommentaire())
                .noteDelai(enquete.getNoteDelai())
                .noteQualite(enquete.getNoteClarte())
                .noteCommunication(enquete.getNoteRelation())
                .dateReponse(enquete.getDateReponse())
                .build();
    }
}
