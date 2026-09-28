package com.backend.modules.demande.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.demande.dto.DemandeDto;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service métier pour les demandes d'analyse (M01).
 * TODO: implémenter la logique complète (workflow, notifications, référence auto).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;

    @Transactional(readOnly = true)
    public DemandeDto trouverParId(Long id) {
        return toDto(charger(id));
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerParClient(Long clientId, Pageable pageable) {
        return demandeRepository.findByClientId(clientId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerParStatut(StatutDemande statut, Pageable pageable) {
        return demandeRepository.findByStatut(statut, pageable).map(this::toDto);
    }

    @Transactional
    public DemandeDto creer(DemandeDto dto) {
        Demande demande = Demande.builder()
                .reference("DEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .objet(dto.getObjet())
                .clientId(dto.getClientId())
                .dateSouhaitee(dto.getDateSouhaitee())
                .commentaire(dto.getCommentaire())
                .build();
        return toDto(demandeRepository.save(demande));
    }

    @Transactional
    public DemandeDto soumettre(Long id) {
        Demande demande = charger(id);
        demande.setStatut(StatutDemande.SOUMISE);
        demande.setDateSoumission(Instant.now());
        return toDto(demandeRepository.save(demande));
    }

    @Transactional
    public DemandeDto changerStatut(Long id, StatutDemande statut) {
        Demande demande = charger(id);
        demande.setStatut(statut);
        return toDto(demandeRepository.save(demande));
    }

    // TODO: GET /api/demandes/stats — agrégation par statut, délai moyen, etc.

    private Demande charger(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", id));
    }

    private DemandeDto toDto(Demande d) {
        return DemandeDto.builder()
                .id(d.getId()).reference(d.getReference()).objet(d.getObjet())
                .statut(d.getStatut()).clientId(d.getClientId())
                .dateSoumission(d.getDateSoumission()).dateSouhaitee(d.getDateSouhaitee())
                .commentaire(d.getCommentaire()).dateCreation(d.getDateCreation())
                .build();
    }
}
