package com.backend.modules.demande.service;

import com.backend.modules.demande.dto.DemandesStatsDto;
import com.backend.modules.demande.dto.SerieMensuelleDemandeDto;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DemandesStatsService {

    private final DemandeRepository demandeRepository;

    @Transactional(readOnly = true)
    public DemandesStatsDto getStats(LocalDate debut, LocalDate fin) {
        if (debut == null) {
            debut = LocalDate.now().minusMonths(12);
        }
        if (fin == null) {
            fin = LocalDate.now();
        }

        Instant debutInstant = debut.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant finInstant = fin.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        Long total = demandeRepository.count();
        Long nombreSurPeriode = demandeRepository.countByDateSoumissionBetween(debutInstant, finInstant);

        Map<String, Long> parStatut = new HashMap<>();
        List<Object[]> counts = demandeRepository.countByStatut();
        for (Object[] row : counts) {
            String statut = (String) row[0];
            Long count = (Long) row[1];
            StatutDemande statutEnum = StatutDemande.depuis(statut);
            if (statutEnum != null) {
                parStatut.put(statutEnum.name(), count);
            }
        }

        Long terminees = parStatut.getOrDefault("TERMINEE", 0L);
        Long annulees = parStatut.getOrDefault("ANNULEE", 0L);
        Long refusees = parStatut.getOrDefault("REFUSEE", 0L);
        Long denominateur = total - annulees - refusees;
        Double tauxCloture = denominateur > 0 ? (double) terminees / denominateur : 0.0;

        List<SerieMensuelleDemandeDto> serieMensuelle = new ArrayList<>();
        List<Object[]> byMonth = demandeRepository.countByMonth();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Object[] row : byMonth) {
            Integer annee = (Integer) row[0];
            Integer mois = (Integer) row[1];
            Long count = (Long) row[2];
            String moisStr = YearMonth.of(annee, mois).format(formatter);
            serieMensuelle.add(SerieMensuelleDemandeDto.builder()
                    .mois(moisStr)
                    .nombre(count)
                    .build());
        }

        return DemandesStatsDto.builder()
                .total(total)
                .parStatut(parStatut)
                .nombreSurPeriode(nombreSurPeriode)
                .tauxCloture(tauxCloture)
                .serieMensuelle(serieMensuelle)
                .build();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Object[]> toutesDemandes(org.springframework.data.domain.Pageable pageable) {
        return demandeRepository.listerPourExport(pageable);
    }
}
