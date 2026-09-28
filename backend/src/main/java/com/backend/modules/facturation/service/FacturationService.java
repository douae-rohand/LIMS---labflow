package com.backend.modules.facturation.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.facturation.dto.FactureDto;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.StatutFacture;
import com.backend.modules.facturation.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service de facturation (M11).
 * TODO: génération PDF facture, intégration comptable, relances automatiques.
 */
@Service @RequiredArgsConstructor
public class FacturationService {

    private final FactureRepository factureRepository;

    @Transactional(readOnly = true)
    public FactureDto trouverParId(Long id) {
        return toDto(factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", id)));
    }

    @Transactional(readOnly = true)
    public Page<FactureDto> listerParClient(Long clientId, Pageable pageable) {
        return factureRepository.findByClientId(clientId, pageable).map(this::toDto);
    }

    @Transactional
    public FactureDto emettre(Long demandeId, Long clientId, BigDecimal montantHt) {
        BigDecimal tva = BigDecimal.valueOf(20);
        BigDecimal ttc = montantHt.multiply(BigDecimal.ONE.add(tva.divide(BigDecimal.valueOf(100))));
        Facture f = Facture.builder()
                .numero("FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .demandeId(demandeId).clientId(clientId).montantHt(montantHt)
                .tauxTva(tva).montantTtc(ttc).statut(StatutFacture.EMISE)
                .dateEmission(LocalDate.now()).dateEcheance(LocalDate.now().plusDays(30)).build();
        return toDto(factureRepository.save(f));
    }

    @Transactional
    public FactureDto enregistrerPaiement(Long id) {
        Facture f = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", id));
        f.setStatut(StatutFacture.PAYEE);
        f.setDatePaiement(Instant.now());
        return toDto(factureRepository.save(f));
    }

    private FactureDto toDto(Facture f) {
        return FactureDto.builder().id(f.getId()).numero(f.getNumero()).demandeId(f.getDemandeId())
                .clientId(f.getClientId()).statut(f.getStatut()).montantHt(f.getMontantHt())
                .tauxTva(f.getTauxTva()).montantTtc(f.getMontantTtc()).dateEmission(f.getDateEmission())
                .dateEcheance(f.getDateEcheance()).datePaiement(f.getDatePaiement()).fichierUrl(f.getFichierUrl()).build();
    }
}
