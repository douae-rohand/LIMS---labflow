package com.backend.modules.facturation.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.facturation.dto.*;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.FactureLigne;
import com.backend.modules.facturation.entity.Paiement;
import com.backend.modules.facturation.entity.StatutFacture;
import com.backend.modules.facturation.repository.FactureLigneRepository;
import com.backend.modules.facturation.repository.FactureRepository;
import com.backend.modules.facturation.repository.PaiementRepository;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturationAdminService {

    private final FactureRepository factureRepository;
    private final FactureLigneRepository factureLigneRepository;
    private final PaiementRepository paiementRepository;
    private final DemandeRepository demandeRepository;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final JournalAuditService journalAuditService;

    private static final int DELAI_ECHEANCE_DEFAUT = 30;
    private static final int RETRY_MAX = 3;

    @Transactional(readOnly = true)
    public Page<FactureAdminDto> listerFactures(Boolean statutPayee, Boolean echeanceDepassee,
                                                   LocalDate debut, LocalDate fin, Long clientId,
                                                   String q, Pageable pageable) {
        Page<Facture> page = factureRepository.rechercher(statutPayee, echeanceDepassee, debut, fin, clientId, q, pageable);
        return page.map(this::toAdminDto);
    }

    @Transactional(readOnly = true)
    public FactureAdminDto getFacture(Long id) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", id));
        return toAdminDto(facture);
    }

    @Transactional
    public FactureAdminDto emettreFacture(EmettreFactureRequest request) {
        for (int essai = 0; essai < RETRY_MAX; essai++) {
            try {
                return emettreFactureInterne(request);
            } catch (Exception ex) {
                if (essai == RETRY_MAX - 1) {
                    throw ex;
                }
                log.warn("Réessai émission facture ({}/{}): {}", essai + 1, RETRY_MAX, ex.getMessage());
            }
        }
        throw new BusinessRuleException("EMISSION_ECHOUEE", "Échec de l'émission après réessais");
    }

    private FactureAdminDto emettreFactureInterne(EmettreFactureRequest request) {
        Demande demande = demandeRepository.findById(request.getDemandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", request.getDemandeId()));

        StatutDemande statut = StatutDemande.depuis(demande.getStatut());
        if (statut != StatutDemande.ACCEPTEE && statut != StatutDemande.EN_COURS
                && statut != StatutDemande.EN_VALIDATION && statut != StatutDemande.TERMINEE) {
            throw new BusinessRuleException("DEMANDE_NON_FACTURABLE",
                    "Cette demande n'est pas facturable (statut: " + demande.getStatut() + ")");
        }

        if (factureRepository.existsByDemandeId(request.getDemandeId())) {
            throw new BusinessRuleException("FACTURE_DEJA_EMISE",
                    "Une facture existe déjà pour cette demande");
        }

        List<LigneEssai> lignesEssai = ligneEssaiRepository.findByDemande_Id(request.getDemandeId());
        if (lignesEssai.isEmpty()) {
            throw new BusinessRuleException("DEMANDE_SANS_MONTANT",
                    "Cette demande n'a aucune ligne d'essai");
        }

        BigDecimal montantTotal = lignesEssai.stream()
                .map(LigneEssai::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (montantTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("DEMANDE_SANS_MONTANT",
                    "Le montant total de la demande est nul");
        }

        String numero = genererNumeroFacture();
        LocalDate dateFacture = LocalDate.now();
        LocalDate dateEcheance = request.getDateEcheance() != null
                ? request.getDateEcheance()
                : dateFacture.plusDays(DELAI_ECHEANCE_DEFAUT);

        Facture facture = Facture.builder()
                .numero(numero)
                .type("STANDARD")
                .dateFacture(dateFacture)
                .dateEcheance(dateEcheance)
                .montantHt(montantTotal)
                .statut(StatutFacture.IMPAYEE.name())
                .demande(demande)
                .build();

        facture = factureRepository.save(facture);

        for (LigneEssai ligneEssai : lignesEssai) {
            FactureLigne ligne = FactureLigne.builder()
                    .facture(facture)
                    .ligneEssai(ligneEssai)
                    .designation(ligneEssai.getEssai().getDesignation())
                    .montant(ligneEssai.getMontant())
                    .build();
            factureLigneRepository.save(ligne);
        }

        journalAuditService.enregistrer("EMISSION_FACTURE", "Facture", facture.getId(),
                SecurityUtils.principalCourant().getEmail(), null, "{\"statut\": \"" + facture.getStatut() + "\"}", null);

        return toAdminDto(facture);
    }

    @Transactional
    public FactureAdminDto enregistrerPaiement(Long id, EnregistrerPaiementRequest request) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", id));

        BigDecimal dejaPaye = paiementRepository.sumByFactureId(id);
        if (dejaPaye == null) dejaPaye = BigDecimal.ZERO;

        BigDecimal resteAPayer = facture.getMontantHt().subtract(dejaPaye);
        if (resteAPayer.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("FACTURE_DEJA_PAYEE",
                    "Cette facture est déjà payée");
        }

        Paiement paiement = Paiement.builder()
                .code("PAY-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .datePaiement(java.time.Instant.now())
                .montant(resteAPayer)
                .mode(request.getMode())
                .reference(request.getReference())
                .facture(facture)
                .encaisseurId(SecurityUtils.principalCourant().getId())
                .build();

        paiementRepository.save(paiement);

        facture.setStatut(StatutFacture.PAYEE.name());
        factureRepository.save(facture);

        journalAuditService.enregistrer("ENCAISSEMENT_FACTURE", "Facture", facture.getId(),
                SecurityUtils.principalCourant().getEmail(), "{\"statut\": \"IMPAYEE\"}",
                "{\"statut\": \"PAYEE\", \"montant\": " + resteAPayer + "}", null);

        return toAdminDto(facture);
    }

    @Transactional(readOnly = true)
    public FactureStatsDto getStats(LocalDate debut, LocalDate fin) {
        if (debut == null) {
            debut = LocalDate.now().minusMonths(12);
        }
        if (fin == null) {
            fin = LocalDate.now();
        }

        java.time.Instant debutInstant = debut.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        java.time.Instant finInstant = fin.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant();

        BigDecimal facture = factureRepository.sumMontantHtBetween(debut, fin);
        if (facture == null) facture = BigDecimal.ZERO;

        BigDecimal encaisse = paiementRepository.sumMontantBetween(debutInstant, finInstant);
        if (encaisse == null) encaisse = BigDecimal.ZERO;

        BigDecimal impaye = factureRepository.sumImpayeBetween(debut, fin);
        if (impaye == null) impaye = BigDecimal.ZERO;

        Long nombreFacturesImpayees = factureRepository.countImpayeesBetween(debut, fin);
        Long nombreEnRetard = factureRepository.countEnRetardBefore(LocalDate.now());

        List<SerieMensuelleDto> serieMensuelle = new ArrayList<>();
        YearMonth current = YearMonth.from(debut);
        YearMonth end = YearMonth.from(fin);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        while (!current.isAfter(end)) {
            String mois = current.format(formatter);
            BigDecimal moisFacture = factureRepository.sumMontantHtByMonth(current.getYear(), current.getMonthValue());
            BigDecimal moisEncaisse = paiementRepository.sumMontantByMonth(current.getYear(), current.getMonthValue());
            if (moisFacture == null) moisFacture = BigDecimal.ZERO;
            if (moisEncaisse == null) moisEncaisse = BigDecimal.ZERO;

            serieMensuelle.add(SerieMensuelleDto.builder()
                    .mois(mois)
                    .facture(moisFacture)
                    .encaisse(moisEncaisse)
                    .build());

            current = current.plusMonths(1);
        }

        return FactureStatsDto.builder()
                .facture(facture)
                .encaisse(encaisse)
                .impaye(impaye)
                .nombreFacturesImpayees(nombreFacturesImpayees)
                .nombreEnRetard(nombreEnRetard)
                .serieMensuelle(serieMensuelle)
                .build();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<FactureAdminDto> toutesFactures(org.springframework.data.domain.Pageable pageable) {
        return factureRepository.findAll(pageable).map(this::toAdminDto);
    }

    private String genererNumeroFacture() {
        YearMonth maintenant = YearMonth.now();
        int annee = maintenant.getYear();
        int mois = maintenant.getMonthValue();

        Integer maxNumero = factureRepository.maxNumeroDuMois(annee, mois);
        int sequence = (maxNumero == null ? 0 : maxNumero) + 1;

        return String.format("FAC-%04d%02d-%03d", annee, mois, sequence);
    }

    private FactureAdminDto toAdminDto(Facture facture) {
        Demande demande = facture.getDemande();
        List<FactureLigne> lignes = factureLigneRepository.findByFactureId(facture.getId());
        List<Paiement> paiements = paiementRepository.findByFactureId(facture.getId());

        List<FactureLigneAdminDto> lignesDto = lignes.stream()
                .map(l -> FactureLigneAdminDto.builder()
                        .id(l.getId())
                        .designation(l.getDesignation())
                        .montant(l.getMontant())
                        .ligneEssaiId(l.getLigneEssai() != null ? l.getLigneEssai().getId() : null)
                        .build())
                .toList();

        List<PaiementAdminDto> paiementsDto = paiements.stream()
                .map(p -> PaiementAdminDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .datePaiement(p.getDatePaiement())
                        .montant(p.getMontant())
                        .mode(p.getMode())
                        .reference(p.getReference())
                        .encaisseurNom(p.getEncaisseurId() != null ? "Encaisseur #" + p.getEncaisseurId() : null)
                        .build())
                .toList();

        return FactureAdminDto.builder()
                .id(facture.getId())
                .numero(facture.getNumero())
                .type(facture.getType())
                .dateFacture(facture.getDateFacture())
                .dateEcheance(facture.getDateEcheance())
                .montantHt(facture.getMontantHt())
                .statut(StatutFacture.depuis(facture.getStatut()).name())
                .demandeId(demande != null ? demande.getId() : null)
                .demandeReference(demande != null ? demande.getNumero() : null)
                .demandeTitre(demande != null ? demande.getTitre() : null)
                .clientId(demande != null && demande.getClient() != null ? demande.getClient().getId() : null)
                .clientNom(demande != null && demande.getClient() != null ? demande.getClient().getRaisonSociale() : null)
                .clientIce(demande != null && demande.getClient() != null ? demande.getClient().getIce() : null)
                .lignes(lignesDto)
                .paiements(paiementsDto)
                .build();
    }
}
