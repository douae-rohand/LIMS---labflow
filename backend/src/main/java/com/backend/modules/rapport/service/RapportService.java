package com.backend.modules.rapport.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
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

@Service
@RequiredArgsConstructor
public class RapportService {

    private final RapportRepository rapportRepository;
    private final DemandeRepository demandeRepository;

    @Transactional(readOnly = true)
    public RapportDto trouverParId(Long id) {
        return toDto(charger(id));
    }

    @Transactional(readOnly = true)
    public Page<RapportDto> listerParDemande(Long demandeId, Pageable pageable) {
        return rapportRepository.findByDemande_Id(demandeId, pageable).map(this::toDto);
    }

    @Transactional
    public RapportDto generer(Long demandeId, Long generateurId) {
        Demande demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId));
        Rapport rapport = Rapport.builder()
                .numero("RAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .demande(demande)
                .statut(StatutRapport.GENERE.name())
                .dateEmission(Instant.now())
                .signataireId(generateurId)
                .build();
        return toDto(rapportRepository.save(rapport));
    }

    @Transactional
    public RapportDto changerStatut(Long id, StatutRapport statut) {
        Rapport rapport = charger(id);
        rapport.setStatut(statut.name());
        if (statut == StatutRapport.ENVOYE) {
            rapport.setDiffuse(true);
        }
        if (statut == StatutRapport.SIGNE) {
            rapport.setDateSignature(Instant.now());
        }
        return toDto(rapportRepository.save(rapport));
    }

    private Rapport charger(Long id) {
        return rapportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport", "id", id));
    }

    private RapportDto toDto(Rapport rapport) {
        return RapportDto.builder()
                .id(rapport.getId())
                .reference(rapport.getNumero())
                .demandeId(rapport.getDemande() == null ? null : rapport.getDemande().getId())
                .statut(enumOuNull(StatutRapport.class, rapport.getStatut()))
                .fichierUrl(rapport.getClePdf())
                .signataireId(rapport.getSignataireId())
                .dateGeneration(rapport.getDateEmission())
                .dateEnvoi(Boolean.TRUE.equals(rapport.getDiffuse()) ? rapport.getDateSignature() : null)
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
