package com.backend.modules.essai.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.echantillon.repository.EchantillonRepository;
import com.backend.modules.essai.dto.EssaiDto;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EssaiService {

    private final LigneEssaiRepository ligneEssaiRepository;
    private final EssaiRepository essaiRepository;
    private final EchantillonRepository echantillonRepository;

    @Transactional(readOnly = true)
    public EssaiDto trouverParId(Long id) {
        return toDto(charger(id));
    }

    @Transactional(readOnly = true)
    public List<EssaiDto> listerParEchantillon(Long echantillonId) {
        return ligneEssaiRepository.findByEchantillon_Id(echantillonId).stream().map(this::toDto).toList();
    }

    @Transactional
    public EssaiDto creer(EssaiDto dto) {
        Echantillon echantillon = echantillonRepository.findById(dto.getEchantillonId())
                .orElseThrow(() -> new ResourceNotFoundException("Echantillon", "id", dto.getEchantillonId()));
        Essai essai = essaiRepository.findByCode(dto.getAnalyseTypeCode())
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "code", dto.getAnalyseTypeCode()));

        LigneEssai ligne = LigneEssai.builder()
                .code("LIG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .statut(StatutEssai.EN_ATTENTE.name())
                .montant(BigDecimal.ZERO)
                .demande(echantillon.getDemande())
                .essai(essai)
                .echantillon(echantillon)
                .technicienId(dto.getTechnicienId())
                .build();
        return toDto(ligneEssaiRepository.save(ligne));
    }

    @Transactional
    public EssaiDto saisirResultat(Long id, EssaiDto dto) {
        LigneEssai ligne = charger(id);
        ligne.setValeur(dto.getResultatValeur());
        ligne.setStatut(StatutEssai.TERMINE.name());
        ligne.setDateFin(Instant.now());
        ligne.setMotif(dto.getObservations());
        if (dto.getValeurMin() != null && dto.getValeurMax() != null && dto.getResultatValeur() != null) {
            try {
                double val = Double.parseDouble(dto.getResultatValeur());
                ligne.setConformite(val >= dto.getValeurMin() && val <= dto.getValeurMax());
            } catch (NumberFormatException ignored) {
                ligne.setConformite(null);
            }
        }
        return toDto(ligneEssaiRepository.save(ligne));
    }

    private LigneEssai charger(Long id) {
        return ligneEssaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
    }

    private EssaiDto toDto(LigneEssai ligne) {
        return EssaiDto.builder()
                .id(ligne.getId())
                .echantillonId(ligne.getEchantillon() == null ? null : ligne.getEchantillon().getId())
                .analyseTypeCode(ligne.getEssai() == null ? null : ligne.getEssai().getCode())
                .technicienId(ligne.getTechnicienId())
                .statut(enumOuNull(StatutEssai.class, ligne.getStatut()))
                .resultatValeur(ligne.getValeur())
                .conforme(ligne.getConformite())
                .observations(ligne.getMotif())
                .dateDebut(ligne.getDateDebut())
                .dateFin(ligne.getDateFin())
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
