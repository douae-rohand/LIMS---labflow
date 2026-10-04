package com.backend.modules.facturation.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.facturation.dto.FactureDto;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.Paiement;
import com.backend.modules.facturation.entity.StatutFacture;
import com.backend.modules.facturation.repository.FactureRepository;
import com.backend.modules.facturation.repository.PaiementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacturationService {

    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final DemandeRepository demandeRepository;

    @Transactional(readOnly = true)
    public FactureDto trouverParId(Long id) {
        return toDto(charger(id));
    }

    @Transactional(readOnly = true)
    public Page<FactureDto> listerParClient(Long clientId, Pageable pageable) {
        return factureRepository.findByDemande_Client_Id(clientId, pageable).map(this::toDto);
    }

    @Transactional
    public FactureDto emettre(Long demandeId, Long clientId, BigDecimal montantHt) {
        Demande demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId));
        Facture facture = Facture.builder()
                .numero("FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .demande(demande)
                .montantHt(montantHt)
                .statut(StatutFacture.EMISE.name())
                .dateFacture(LocalDate.now())
                .dateEcheance(LocalDate.now().plusDays(30))
                .build();
        return toDto(factureRepository.save(facture));
    }

    @Transactional
    public FactureDto enregistrerPaiement(Long id) {
        Facture facture = charger(id);
        facture.setStatut(StatutFacture.PAYEE.name());
        factureRepository.save(facture);
        paiementRepository.save(Paiement.builder()
                .code("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .datePaiement(Instant.now())
                .montant(facture.getMontantHt())
                .facture(facture)
                .build());
        return toDto(facture);
    }

    private Facture charger(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", id));
    }

    private FactureDto toDto(Facture facture) {
        Demande demande = facture.getDemande();
        Long clientId = demande != null && demande.getClient() != null ? demande.getClient().getId() : null;
        return FactureDto.builder()
                .id(facture.getId())
                .numero(facture.getNumero())
                .demandeId(demande == null ? null : demande.getId())
                .clientId(clientId)
                .statut(enumOuNull(StatutFacture.class, facture.getStatut()))
                .montantHt(facture.getMontantHt())
                .dateEmission(facture.getDateFacture())
                .dateEcheance(facture.getDateEcheance())
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
