package com.backend.modules.essai.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.essai.dto.EssaiDto;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.EssaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

/**
 * Service de saisie et suivi des essais (M05).
 * TODO: gestion des instruments, traçabilité des réactifs, incertitudes de mesure.
 */
@Service @RequiredArgsConstructor
public class EssaiService {

    private final EssaiRepository essaiRepository;

    @Transactional(readOnly = true)
    public EssaiDto trouverParId(Long id) {
        return toDto(essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id)));
    }

    @Transactional(readOnly = true)
    public List<EssaiDto> listerParEchantillon(Long echantillonId) {
        return essaiRepository.findByEchantillonId(echantillonId).stream().map(this::toDto).toList();
    }

    @Transactional
    public EssaiDto creer(EssaiDto dto) {
        Essai e = Essai.builder().echantillonId(dto.getEchantillonId())
                .analyseTypeCode(dto.getAnalyseTypeCode()).technicienId(dto.getTechnicienId()).build();
        return toDto(essaiRepository.save(e));
    }

    @Transactional
    public EssaiDto saisirResultat(Long id, EssaiDto dto) {
        Essai e = essaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Essai", "id", id));
        e.setResultatValeur(dto.getResultatValeur());
        e.setResultatUnite(dto.getResultatUnite());
        e.setValeurMin(dto.getValeurMin());
        e.setValeurMax(dto.getValeurMax());
        e.setObservations(dto.getObservations());
        e.setStatut(StatutEssai.TERMINE);
        e.setDateFin(Instant.now());
        // Calcul conformité automatique si bornes définies
        if (dto.getValeurMin() != null && dto.getValeurMax() != null && dto.getResultatValeur() != null) {
            try {
                double val = Double.parseDouble(dto.getResultatValeur());
                e.setConforme(val >= dto.getValeurMin() && val <= dto.getValeurMax());
            } catch (NumberFormatException ignored) { /* résultat non numérique */ }
        }
        return toDto(essaiRepository.save(e));
    }

    private EssaiDto toDto(Essai e) {
        return EssaiDto.builder().id(e.getId()).echantillonId(e.getEchantillonId())
                .analyseTypeCode(e.getAnalyseTypeCode()).technicienId(e.getTechnicienId())
                .statut(e.getStatut()).resultatValeur(e.getResultatValeur()).resultatUnite(e.getResultatUnite())
                .valeurMin(e.getValeurMin()).valeurMax(e.getValeurMax()).conforme(e.getConforme())
                .observations(e.getObservations()).dateDebut(e.getDateDebut()).dateFin(e.getDateFin()).build();
    }
}
