package com.backend.modules.satisfaction.service;

import com.backend.modules.satisfaction.dto.SerieMensuelleSatisfactionDto;
import com.backend.modules.satisfaction.dto.SatisfactionStatsDto;
import com.backend.modules.satisfaction.repository.EnqueteSatisfactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SatisfactionAdminService {

    private final EnqueteSatisfactionRepository enqueteRepository;

    @Transactional(readOnly = true)
    public SatisfactionStatsDto getStats(Instant debut, Instant fin) {
        if (debut == null) {
            debut = LocalDate.now().minusMonths(12).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        if (fin == null) {
            fin = LocalDate.now().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        }

        Long envoyees = enqueteRepository.countEnvoyeesBetween(debut, fin);
        Long repondues = enqueteRepository.countReponduesBetween(debut, fin);
        Double tauxReponse = envoyees > 0 ? (double) repondues / envoyees : 0.0;

        Double moyenneGlobale = enqueteRepository.moyenneGlobaleRepondues();
        Double moyenneDelai = enqueteRepository.moyenneDelai();
        Double moyenneClarte = enqueteRepository.moyenneClarte();
        Double moyenneRelation = enqueteRepository.moyenneRelation();

        Map<Integer, Long> repartitionNotes = new HashMap<>();
        List<Object[]> byNote = enqueteRepository.countByNoteGlobale();
        for (Object[] row : byNote) {
            Integer note = (Integer) row[0];
            Long count = (Long) row[1];
            repartitionNotes.put(note, count);
        }

        List<SerieMensuelleSatisfactionDto> serieMensuelle = new ArrayList<>();
        List<Object[]> byMonth = enqueteRepository.moyenneByMonth();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Object[] row : byMonth) {
            Integer annee = (Integer) row[0];
            Integer mois = (Integer) row[1];
            Double moyenne = (Double) row[2];
            String moisStr = YearMonth.of(annee, mois).format(formatter);
            serieMensuelle.add(SerieMensuelleSatisfactionDto.builder()
                    .mois(moisStr)
                    .moyenne(moyenne)
                    .build());
        }

        return SatisfactionStatsDto.builder()
                .envoyees(envoyees)
                .repondues(repondues)
                .tauxReponse(tauxReponse)
                .moyenneGlobale(moyenneGlobale)
                .moyenneDelai(moyenneDelai)
                .moyenneClarte(moyenneClarte)
                .moyenneRelation(moyenneRelation)
                .repartitionNotes(repartitionNotes)
                .serieMensuelle(serieMensuelle)
                .build();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Object[]> listerEnquetesRepondues(Instant debut, Instant fin, Integer note, Boolean avecCommentaire, org.springframework.data.domain.Pageable pageable) {
        return enqueteRepository.listerPourExport(pageable);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Object[]> toutesEnquetesRepondues(org.springframework.data.domain.Pageable pageable) {
        return enqueteRepository.listerPourExport(pageable);
    }
}
