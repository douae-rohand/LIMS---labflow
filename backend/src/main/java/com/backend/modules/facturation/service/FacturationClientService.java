package com.backend.modules.facturation.service;

import com.backend.common.dto.ListeAgregeeDto;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.facturation.dto.FactureClientDetailDto;
import com.backend.modules.facturation.dto.FactureClientResumeDto;
import com.backend.modules.facturation.dto.FactureLigneClientDto;
import com.backend.modules.facturation.dto.PaiementClientDto;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.Paiement;
import com.backend.modules.facturation.entity.StatutFacture;
import com.backend.modules.facturation.repository.FactureLigneRepository;
import com.backend.modules.facturation.repository.FactureRepository;
import com.backend.modules.facturation.repository.PaiementRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factures de l'espace client : liste agrégée sur tous les laboratoires du client.
 *
 * <p>Le statut affiché n'est jamais le statut stocké : il est recalculé
 * ({@code PAYEE} si {@code somme(paiements) >= montant_ht}, sinon {@code IMPAYEE}).
 * L'encaisseur des paiements n'est jamais exposé.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturationClientService {

    private final FactureRepository factureRepository;
    private final FactureLigneRepository factureLigneRepository;
    private final PaiementRepository paiementRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final ClientLaboratoireRepository clientLaboratoireRepository;
    private final TenantExecutor tenantExecutor;

    public ListeAgregeeDto<FactureClientResumeDto> listerFacturesClient(Long utilisateurId) {
        List<ClientLaboratoire> liaisons = tenantExecutor.inCentral(
                () -> clientLaboratoireRepository.findByUtilisateur_Id(utilisateurId));
        List<FactureClientResumeDto> resultats = new ArrayList<>();
        int laboratoiresIndisponibles = 0;
        for (ClientLaboratoire liaison : liaisons) {
            Laboratoire laboratoire = liaison.getLaboratoire();
            try {
                List<FactureClientResumeDto> duLaboratoire = tenantExecutor.inTenant(laboratoire.getNomSchema(),
                        () -> toResumes(factureRepository.findParUtilisateurAvecDemande(utilisateurId), laboratoire));
                resultats.addAll(duLaboratoire);
            } catch (Exception ex) {
                laboratoiresIndisponibles++;
                log.warn("Factures indisponibles pour le laboratoire {} : {}",
                        laboratoire.getCode(), ex.getMessage());
            }
        }
        resultats.sort(Comparator.comparing(FactureClientResumeDto::getDateEmission,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return ListeAgregeeDto.de(resultats, laboratoiresIndisponibles);
    }

    public FactureClientDetailDto obtenirDetailClient(Long utilisateurId, Long laboratoireId, Long factureId) {
        Laboratoire laboratoire = exigerLaboratoire(laboratoireId);
        return tenantExecutor.inTenant(laboratoire.getNomSchema(), () -> {
            Facture facture = factureRepository.findVisibleClientParId(factureId, utilisateurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", factureId));

            List<FactureLigneClientDto> lignes = factureLigneRepository.findByFacture_IdOrderByIdAsc(factureId)
                    .stream()
                    .map(l -> FactureLigneClientDto.builder()
                            .ligneId(l.getId())
                            .ligneEssaiId(l.getLigneEssai() == null ? null : l.getLigneEssai().getId())
                            .designation(l.getDesignation())
                            .montant(l.getMontant())
                            .build())
                    .toList();

            List<Paiement> paiements = paiementRepository.findByFacture_IdIn(List.of(factureId));
            BigDecimal montantPaye = sommePaiements(paiements);
            Demande demande = facture.getDemande();
            String ice = demande.getClient() == null ? null : demande.getClient().getIce();

            return FactureClientDetailDto.builder()
                    .factureId(facture.getId())
                    .laboratoireId(laboratoire.getId())
                    .laboratoireNom(laboratoire.getRaisonSociale())
                    .numero(facture.getNumero())
                    .type(facture.getType())
                    .demandeId(demande.getId())
                    .demandeReference(demande.getNumero())
                    .demandeTitre(demande.getTitre())
                    .dateEmission(facture.getDateFacture())
                    .dateEcheance(facture.getDateEcheance())
                    .montantHt(facture.getMontantHt())
                    .montantPaye(montantPaye)
                    .resteAPayer(resteAPayer(facture.getMontantHt(), montantPaye))
                    .statut(statutEffectif(facture.getMontantHt(), montantPaye))
                    .ice(ice)
                    .lignes(lignes)
                    .paiements(paiements.stream()
                            .map(p -> PaiementClientDto.builder()
                                    .date(p.getDatePaiement())
                                    .montant(p.getMontant())
                                    .mode(p.getMode())
                                    .build())
                            .toList())
                    .build();
        });
    }

    /**
     * Une requête principale (factures + demande) puis une requête pour les
     * paiements rattachés (findBy…In) : aucun N+1.
     */
    private List<FactureClientResumeDto> toResumes(List<Facture> factures, Laboratoire laboratoire) {
        if (factures.isEmpty()) {
            return List.of();
        }
        List<Long> ids = factures.stream().map(Facture::getId).toList();
        Map<Long, BigDecimal> payesParFacture = paiementRepository.findByFacture_IdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        p -> p.getFacture().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Paiement::getMontant, BigDecimal::add)));
        return factures.stream()
                .map(f -> {
                    BigDecimal paye = payesParFacture.getOrDefault(f.getId(), BigDecimal.ZERO);
                    Demande demande = f.getDemande();
                    return FactureClientResumeDto.builder()
                            .factureId(f.getId())
                            .laboratoireId(laboratoire.getId())
                            .laboratoireNom(laboratoire.getRaisonSociale())
                            .numero(f.getNumero())
                            .demandeReference(demande.getNumero())
                            .demandeTitre(demande.getTitre())
                            .dateEmission(f.getDateFacture())
                            .dateEcheance(f.getDateEcheance())
                            .montantTotal(f.getMontantHt())
                            .montantPaye(paye)
                            .resteAPayer(resteAPayer(f.getMontantHt(), paye))
                            .statut(statutEffectif(f.getMontantHt(), paye))
                            .build();
                })
                .toList();
    }

    private Laboratoire exigerLaboratoire(Long laboratoireId) {
        return tenantExecutor.inCentral(() -> laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", laboratoireId)));
    }

    private static BigDecimal sommePaiements(List<Paiement> paiements) {
        return paiements.stream()
                .map(Paiement::getMontant)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Statut recalculé : PAYEE si la somme des paiements couvre le HT, sinon IMPAYEE. */
    private static StatutFacture statutEffectif(BigDecimal montantHt, BigDecimal montantPaye) {
        BigDecimal ht = montantHt == null ? BigDecimal.ZERO : montantHt;
        BigDecimal paye = montantPaye == null ? BigDecimal.ZERO : montantPaye;
        return paye.compareTo(ht) >= 0 ? StatutFacture.PAYEE : StatutFacture.IMPAYEE;
    }

    /** Reste à payer, jamais négatif. */
    private static BigDecimal resteAPayer(BigDecimal montantHt, BigDecimal montantPaye) {
        BigDecimal ht = montantHt == null ? BigDecimal.ZERO : montantHt;
        BigDecimal paye = montantPaye == null ? BigDecimal.ZERO : montantPaye;
        BigDecimal reste = ht.subtract(paye);
        return reste.signum() < 0 ? BigDecimal.ZERO : reste;
    }
}
