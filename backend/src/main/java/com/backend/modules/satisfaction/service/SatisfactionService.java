package com.backend.modules.satisfaction.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.auth.security.UtilisateurPrincipal;
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
        Demande demande = demandeRepository.findById(dto.getDemandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", dto.getDemandeId()));
        verifierProprieteClient(demande);
        if (enqueteRepository.existsByDemande_Id(dto.getDemandeId())) {
            throw new BusinessRuleException("SATISFACTION_DEJA_SOUMISE",
                    "Vous avez déjà évalué cette demande");
        }
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
        Long clientLocalId = demande.getClient() == null ? null : demande.getClient().getId();
        return toDto(enqueteRepository.save(enquete), clientLocalId);
    }

    @Transactional(readOnly = true)
    public Double moyenneGlobale() {
        return enqueteRepository.calculerMoyenneGlobale();
    }

    private void verifierProprieteClient(Demande demande) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        if (!principal.isClient()) {
            return;
        }
        Long proprietaire = demande.getClient() == null ? null : demande.getClient().getUtilisateurId();
        if (proprietaire == null || !proprietaire.equals(principal.getId())) {
            throw new UnauthorizedTenantException("Cette demande ne vous appartient pas");
        }
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
