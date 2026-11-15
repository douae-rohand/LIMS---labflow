package com.backend.modules.accueil.service;

import com.backend.modules.accueil.dto.ReceptionDtos.DemandeReceptionDto;
import com.backend.modules.accueil.dto.ReceptionDtos.EchantillonRecuDto;
import com.backend.modules.accueil.dto.ReceptionDtos.EtatReception;
import com.backend.modules.accueil.dto.ReceptionDtos.TableauAccueilDto;
import com.backend.modules.accueil.repository.ReceptionRepository;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.planification.service.LigneTravailAssembleur;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Tableau de bord de l'accueil : réceptions à faire, reçues aujourd'hui, factures à encaisser. */
@Service
@RequiredArgsConstructor
public class TableauAccueilService {

    private static final ZoneId FUSEAU = ZoneId.of("Africa/Casablanca");
    private static final int APERCU = 6;

    private final ReceptionService receptionService;
    private final ReceptionRepository receptionRepository;
    private final LigneTravailAssembleur assembleur;

    @Transactional(readOnly = true)
    public TableauAccueilDto tableau() {
        List<DemandeReceptionDto> demandes = receptionService.lister();
        List<DemandeReceptionDto> enAttente = demandes.stream()
                .filter(d -> d.etat() != EtatReception.COMPLETE).toList();

        Instant debutJour = LocalDate.now(FUSEAU).atStartOfDay(FUSEAU).toInstant();
        List<Echantillon> recus = receptionRepository.recusDepuis(debutJour);
        Map<Long, String> noms = assembleur.nomsUtilisateurs(recus.stream()
                .map(Echantillon::getReceptionneurId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<EchantillonRecuDto> recusDto = recus.stream()
                .map(e -> receptionService.versDto(e, noms, List.of())).toList();

        Object[] factures = receptionRepository.facturesAEncaisser().stream().findFirst().orElse(new Object[]{0L, BigDecimal.ZERO});
        long nombreFactures = factures[0] == null ? 0 : ((Number) factures[0]).longValue();
        BigDecimal reste = factures[1] instanceof BigDecimal montant ? montant
                : factures[1] == null ? BigDecimal.ZERO : new BigDecimal(factures[1].toString());

        return new TableauAccueilDto(
                enAttente.stream().filter(d -> d.etat() == EtatReception.A_RECEVOIR).count(),
                enAttente.stream().filter(d -> d.etat() == EtatReception.PARTIELLE).count(),
                recusDto.size(),
                recusDto.stream().filter(e -> !e.conforme()).count(),
                nombreFactures,
                reste,
                enAttente.stream().limit(APERCU).toList(),
                recusDto.stream().limit(APERCU).toList());
    }
}
