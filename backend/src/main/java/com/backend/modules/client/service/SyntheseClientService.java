package com.backend.modules.client.service;

import com.backend.common.dto.ListeAgregeeDto;
import com.backend.modules.client.dto.SyntheseClientDto;
import com.backend.modules.facturation.dto.FactureClientResumeDto;
import com.backend.modules.facturation.entity.StatutFacture;
import com.backend.modules.facturation.service.FacturationClientService;
import com.backend.modules.rapport.dto.RapportClientResumeDto;
import com.backend.modules.rapport.service.RapportClientService;
import com.backend.modules.satisfaction.dto.EnqueteClientResumeDto;
import com.backend.modules.satisfaction.service.SatisfactionClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Synthèse du tableau de bord client. Réutilise les services de liste
 * (rapports, factures, enquêtes) : aucune requête n'est dupliquée ici.
 */
@Service
@RequiredArgsConstructor
public class SyntheseClientService {

    private final RapportClientService rapportClientService;
    private final FacturationClientService facturationClientService;
    private final SatisfactionClientService satisfactionClientService;

    public SyntheseClientDto calculerSynthese(Long utilisateurId) {
        ListeAgregeeDto<RapportClientResumeDto> rapports =
                rapportClientService.listerRapportsClient(utilisateurId);
        ListeAgregeeDto<FactureClientResumeDto> factures =
                facturationClientService.listerFacturesClient(utilisateurId);
        ListeAgregeeDto<EnqueteClientResumeDto> enquetes =
                satisfactionClientService.listerEnquetesClient(utilisateurId);

        List<FactureClientResumeDto> facturesImpayees = factures.getElements().stream()
                .filter(f -> f.getStatut() == StatutFacture.IMPAYEE)
                .toList();
        BigDecimal montantRestantDu = facturesImpayees.stream()
                .map(FactureClientResumeDto::getResteAPayer)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int enquetesEnAttente = (int) enquetes.getElements().stream()
                .filter(e -> !e.isRepondue())
                .count();

        // Les 3 services itèrent la même liste de client_laboratoire : ce sont donc
        // les mêmes laboratoires qui peuvent échouer. On retient le maximum des trois
        // compteurs comme estimation du nombre de laboratoires indisponibles.
        int laboratoiresIndisponibles = Math.max(rapports.getLaboratoiresIndisponibles(),
                Math.max(factures.getLaboratoiresIndisponibles(), enquetes.getLaboratoiresIndisponibles()));

        return SyntheseClientDto.builder()
                .rapportsDisponibles(rapports.getElements().size())
                .facturesImpayees(facturesImpayees.size())
                .montantRestantDu(montantRestantDu)
                .enquetesEnAttente(enquetesEnAttente)
                .laboratoiresIndisponibles(laboratoiresIndisponibles)
                .build();
    }
}
