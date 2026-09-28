package com.backend.modules.rapport.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.rapport.dto.RapportDto;
import com.backend.modules.rapport.entity.Rapport;
import com.backend.modules.rapport.entity.StatutRapport;
import com.backend.modules.rapport.repository.RapportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

/**
 * Service de génération et gestion des rapports (M07).
 * TODO: génération PDF (JasperReports / Thymeleaf + Flying Saucer), stockage MinIO, envoi email.
 */
@Service @RequiredArgsConstructor
public class RapportService {

    private final RapportRepository rapportRepository;

    @Transactional(readOnly = true)
    public RapportDto trouverParId(Long id) {
        return toDto(rapportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport", "id", id)));
    }

    @Transactional(readOnly = true)
    public Page<RapportDto> listerParDemande(Long demandeId, Pageable pageable) {
        return rapportRepository.findByDemandeId(demandeId, pageable).map(this::toDto);
    }

    @Transactional
    public RapportDto generer(Long demandeId, Long generateurId) {
        Rapport r = Rapport.builder()
                .reference("RAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .demandeId(demandeId).generateurId(generateurId)
                .statut(StatutRapport.GENERE).dateGeneration(Instant.now()).build();
        // TODO: appel au moteur de génération PDF + upload MinIO
        return toDto(rapportRepository.save(r));
    }

    @Transactional
    public RapportDto changerStatut(Long id, StatutRapport statut) {
        Rapport r = rapportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport", "id", id));
        r.setStatut(statut);
        if (statut == StatutRapport.ENVOYE) r.setDateEnvoi(Instant.now());
        return toDto(rapportRepository.save(r));
    }

    private RapportDto toDto(Rapport r) {
        return RapportDto.builder().id(r.getId()).reference(r.getReference()).demandeId(r.getDemandeId())
                .statut(r.getStatut()).fichierUrl(r.getFichierUrl()).generateurId(r.getGenerateurId())
                .signataireId(r.getSignataireId()).dateGeneration(r.getDateGeneration()).dateEnvoi(r.getDateEnvoi()).build();
    }
}
