package com.backend.modules.echantillon.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.echantillon.dto.EchantillonDto;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.echantillon.entity.StatutEchantillon;
import com.backend.modules.echantillon.repository.EchantillonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service de gestion des échantillons (M03).
 * TODO: génération code-barre, intégration MinIO pour photos, non-conformités.
 */
@Service
@RequiredArgsConstructor
public class EchantillonService {

    private final EchantillonRepository echantillonRepository;

    @Transactional(readOnly = true)
    public EchantillonDto trouverParId(Long id) {
        return toDto(charger(id));
    }

    @Transactional(readOnly = true)
    public Page<EchantillonDto> listerParDemande(Long demandeId, Pageable pageable) {
        return echantillonRepository.findByDemandeId(demandeId, pageable).map(this::toDto);
    }

    @Transactional
    public EchantillonDto enregistrer(EchantillonDto dto) {
        Echantillon e = Echantillon.builder()
                .codeBarre(dto.getCodeBarre() != null ? dto.getCodeBarre()
                        : "ECH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .designation(dto.getDesignation()).nature(dto.getNature())
                .demandeId(dto.getDemandeId()).receptionnaireId(dto.getReceptionnaireId())
                .quantite(dto.getQuantite()).unite(dto.getUnite())
                .datePeremption(dto.getDatePeremption())
                .conditionsConservation(dto.getConditionsConservation())
                .observations(dto.getObservations()).build();
        return toDto(echantillonRepository.save(e));
    }

    @Transactional
    public EchantillonDto receptionner(Long id, Long receptionnaireId) {
        Echantillon e = charger(id);
        e.setStatut(StatutEchantillon.RECEPTIONNE);
        e.setReceptionnaireId(receptionnaireId);
        e.setDateReception(Instant.now());
        return toDto(echantillonRepository.save(e));
    }

    @Transactional
    public EchantillonDto changerStatut(Long id, StatutEchantillon statut) {
        Echantillon e = charger(id);
        e.setStatut(statut);
        return toDto(echantillonRepository.save(e));
    }

    private Echantillon charger(Long id) {
        return echantillonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Echantillon", "id", id));
    }

    private EchantillonDto toDto(Echantillon e) {
        return EchantillonDto.builder()
                .id(e.getId()).codeBarre(e.getCodeBarre()).designation(e.getDesignation())
                .nature(e.getNature()).demandeId(e.getDemandeId()).receptionnaireId(e.getReceptionnaireId())
                .statut(e.getStatut()).dateReception(e.getDateReception())
                .datePeremption(e.getDatePeremption()).quantite(e.getQuantite()).unite(e.getUnite())
                .conditionsConservation(e.getConditionsConservation()).observations(e.getObservations())
                .build();
    }
}
