package com.backend.modules.echantillon.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
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

@Service
@RequiredArgsConstructor
public class EchantillonService {

    private final EchantillonRepository echantillonRepository;
    private final DemandeRepository demandeRepository;

    @Transactional(readOnly = true)
    public EchantillonDto trouverParId(Long id) {
        return toDto(charger(id));
    }

    @Transactional(readOnly = true)
    public Page<EchantillonDto> listerParDemande(Long demandeId, Pageable pageable) {
        return echantillonRepository.findByDemande_Id(demandeId, pageable).map(this::toDto);
    }

    @Transactional
    public EchantillonDto enregistrer(EchantillonDto dto) {
        Demande demande = demandeRepository.findById(dto.getDemandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", dto.getDemandeId()));
        Echantillon echantillon = Echantillon.builder()
                .reference(dto.getCodeBarre() != null ? dto.getCodeBarre()
                        : "ECH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .nature(dto.getNature())
                .demande(demande)
                .receptionneurId(dto.getReceptionnaireId())
                .conditionsConservation(dto.getConditionsConservation())
                .motif(dto.getObservations())
                .build();
        return toDto(echantillonRepository.save(echantillon));
    }

    @Transactional
    public EchantillonDto receptionner(Long id, Long receptionnaireId) {
        Echantillon echantillon = charger(id);
        echantillon.setReceptionneurId(receptionnaireId);
        echantillon.setDateReception(Instant.now());
        echantillon.setConformite(true);
        return toDto(echantillonRepository.save(echantillon));
    }

    @Transactional
    public EchantillonDto changerStatut(Long id, StatutEchantillon statut) {
        Echantillon echantillon = charger(id);
        echantillon.setConformite(statut != StatutEchantillon.NON_CONFORME);
        return toDto(echantillonRepository.save(echantillon));
    }

    private Echantillon charger(Long id) {
        return echantillonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Echantillon", "id", id));
    }

    private EchantillonDto toDto(Echantillon e) {
        StatutEchantillon statut = null;
        if (Boolean.FALSE.equals(e.getConformite())) {
            statut = StatutEchantillon.NON_CONFORME;
        } else if (e.getDateReception() != null) {
            statut = StatutEchantillon.RECEPTIONNE;
        }
        return EchantillonDto.builder()
                .id(e.getId())
                .codeBarre(e.getReference())
                .nature(e.getNature())
                .demandeId(e.getDemande() == null ? null : e.getDemande().getId())
                .receptionnaireId(e.getReceptionneurId())
                .statut(statut)
                .dateReception(e.getDateReception())
                .conditionsConservation(e.getConditionsConservation())
                .observations(e.getMotif())
                .build();
    }
}
