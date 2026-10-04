package com.backend.modules.facturation.service;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacturationService {

    private final FactureRepository factureRepository;
    private final PaiementRepository paiementRepository;
    private final DemandeRepository demandeRepository;
    private final ClientLaboratoireRepository clientLaboratoireRepository;
    private final TenantExecutor tenantExecutor;

    @Transactional(readOnly = true)
    public FactureDto trouverParId(Long id) {
        Facture facture = charger(id);
        verifierProprieteClient(facture);
        return toDto(facture, TenantContext.getCurrentTenant());
    }

    @Transactional(readOnly = true)
    public Page<FactureDto> listerParClient(Long clientLocalId, Pageable pageable) {
        return factureRepository.findByDemande_Client_Id(clientLocalId, pageable)
                .map(f -> toDto(f, TenantContext.getCurrentTenant()));
    }

    @Transactional(readOnly = true)
    public Page<FactureDto> listerMesFactures(Pageable pageable) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        return factureRepository.findByDemande_Client_UtilisateurId(principal.getId(), pageable)
                .map(f -> toDto(f, TenantContext.getCurrentTenant()));
    }

    public List<FactureDto> listerToutesMesFactures() {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        List<ClientLaboratoire> liaisons = tenantExecutor.inCentral(
                () -> clientLaboratoireRepository.findByUtilisateur_Id(principal.getId()));
        List<FactureDto> resultats = new ArrayList<>();
        for (ClientLaboratoire liaison : liaisons) {
            String nomSchema = liaison.getLaboratoire().getNomSchema();
            String code = liaison.getLaboratoire().getCode();
            resultats.addAll(tenantExecutor.inTenant(nomSchema, () ->
                    factureRepository.findByDemande_Client_UtilisateurId(principal.getId()).stream()
                            .map(f -> toDto(f, code))
                            .toList()));
        }
        resultats.sort(Comparator.comparing(FactureDto::getDateEmission,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return resultats;
    }

    @Transactional
    public FactureDto emettre(Long demandeId, Long clientIdIgnore, BigDecimal montantHt) {
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
        return toDto(factureRepository.save(facture), TenantContext.getCurrentTenant());
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
        return toDto(facture, TenantContext.getCurrentTenant());
    }

    private Facture charger(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", id));
    }

    private void verifierProprieteClient(Facture facture) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        if (!principal.isClient()) {
            return;
        }
        Demande demande = facture.getDemande();
        Long proprietaire = demande != null && demande.getClient() != null
                ? demande.getClient().getUtilisateurId() : null;
        if (proprietaire == null || !proprietaire.equals(principal.getId())) {
            throw new UnauthorizedTenantException("Cette facture ne vous appartient pas");
        }
    }

    private FactureDto toDto(Facture facture, String laboratoireCode) {
        Demande demande = facture.getDemande();
        Long clientId = demande != null && demande.getClient() != null ? demande.getClient().getId() : null;
        return FactureDto.builder()
                .id(facture.getId())
                .numero(facture.getNumero())
                .demandeId(demande == null ? null : demande.getId())
                .clientId(clientId)
                .laboratoireCode(laboratoireCode)
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
