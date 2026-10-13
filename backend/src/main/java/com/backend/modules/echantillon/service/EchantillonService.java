package com.backend.modules.echantillon.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.echantillon.dto.EchantillonDto;
import com.backend.modules.echantillon.entity.Echantillon;
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
        if (dto.getNature() == null || dto.getNature().isBlank()) {
            throw new BusinessRuleException("NATURE_OBLIGATOIRE",
                    "La nature de l'échantillon reçu est obligatoire.");
        }
        boolean conforme = dto.getConformite() == null || Boolean.TRUE.equals(dto.getConformite());
        String motif = dto.getObservations();
        if (!conforme && (motif == null || motif.isBlank())) {
            throw new BusinessRuleException("MOTIF_OBLIGATOIRE",
                    "Un échantillon non conforme exige un motif.");
        }
        Demande demande = demandeRepository.findById(dto.getDemandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", dto.getDemandeId()));
        Echantillon echantillon = Echantillon.builder()
                .reference(dto.getCodeBarre() != null ? dto.getCodeBarre()
                        : "ECH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .nature(dto.getNature().trim())
                .dateReception(Instant.now())
                .conformite(conforme)
                .motif(motif)
                .etatConservationConstate(dto.getEtatConservationConstate())
                .demande(demande)
                .receptionneurId(SecurityUtils.principalCourant().getId())
                .build();
        return toDto(echantillonRepository.save(echantillon));
    }

    private Echantillon charger(Long id) {
        return echantillonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Echantillon", "id", id));
    }

    private EchantillonDto toDto(Echantillon e) {
        return EchantillonDto.builder()
                .id(e.getId())
                .codeBarre(e.getReference())
                .nature(e.getNature())
                .demandeId(e.getDemande() == null ? null : e.getDemande().getId())
                .receptionneurId(e.getReceptionneurId())
                .dateReception(e.getDateReception())
                .etatConservationConstate(e.getEtatConservationConstate())
                .conformite(e.getConformite())
                .observations(e.getMotif())
                .build();
    }
}
