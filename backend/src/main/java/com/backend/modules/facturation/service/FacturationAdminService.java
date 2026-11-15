package com.backend.modules.facturation.service;

import com.backend.common.audit.JournalAudit;
import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
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
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private final TenantExecutor tenantExecutor;
    private final UtilisateurRepository utilisateurRepository;
    private final com.backend.modules.plateforme.service.ParametrePlateformeService parametrePlateformeService;

    private static final int RETRY_MAX = 3;

    @Value("${facturation.delai-echeance-jours:30}")
    private int delaiEcheanceJours;

    public Page<FactureAdminDto> listerFactures(String statut, LocalDate debut, LocalDate fin, Long clientId,
                                                   String q, int page, int size, String tri, String direction) {
        String proprieteTri = switch (tri) {
            case "numero" -> "numero";
            case "date" -> "dateFacture";
            default -> "dateFacture";
        };
        Sort.Direction sensTri = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sensTri, proprieteTri));
        return tenantExecutor.inTenant(schemaCourant(), () -> {
            Page<Facture> pageResult = factureRepository.rechercher(
                    statut, debut, fin, clientId, q, pageable);
            return pageResult.map(this::toAdminDto);
        });
    }

    public FactureAdminDto getFacture(Long id) {
        return tenantExecutor.inTenant(schemaCourant(), () -> {
            Facture facture = factureRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", id));
            return toAdminDto(facture);
        });
    }

    public FactureAdminDto emettreFacture(EmettreFactureRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        return emettrePourDemande(
                request.getDemandeId(), request.getDateEcheance(), acteur, schemaCourant(), false);
    }

    public List<DemandeFacturableDto> listerDemandesFacturables() {
        return tenantExecutor.inTenant(schemaCourant(), () ->
                factureRepository.listerDemandesFacturables(
                        List.of("ACCEPTEE", "EN_COURS", "EN_VALIDATION", "TERMINEE"),
                        PageRequest.of(0, 200)));
    }

    public FactureAdminDto emettrePourDemande(
            Long demandeId,
            LocalDate dateEcheance,
            UtilisateurPrincipal acteur,
            String nomSchema,
            boolean automatique) {
        if (nomSchema == null || nomSchema.isBlank()) {
            throw new BusinessRuleException("SCHEMA_LABORATOIRE_REQUIS",
                    "Le schéma du laboratoire est obligatoire pour émettre une facture.");
        }
        java.math.BigDecimal tauxCommission = parametrePlateformeService.tauxCommission();
        for (int essai = 0; essai < RETRY_MAX; essai++) {
            try {
                return tenantExecutor.inTenant(nomSchema,
                        () -> emettreFactureInterne(demandeId, dateEcheance, acteur, nomSchema, automatique, tauxCommission));
            } catch (DataIntegrityViolationException ex) {
                if (essai == RETRY_MAX - 1) {
                    throw ex;
                }
                log.warn("Réessai émission facture demandeId={} tentative={}/{} code=CONFLIT_NUMERO",
                        demandeId, essai + 1, RETRY_MAX);
            }
        }
        throw new BusinessRuleException("EMISSION_ECHOUEE", "Échec de l'émission après réessais");
    }

    private FactureAdminDto emettreFactureInterne(
            Long demandeId,
            LocalDate dateEcheance,
            UtilisateurPrincipal acteur,
            String nomSchema,
            boolean automatique,
            BigDecimal tauxCommission) {
        Demande demande = demandeRepository.findByIdForUpdate(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId));

        StatutDemande statut = StatutDemande.depuis(demande.getStatut());
        if (statut != StatutDemande.ACCEPTEE && statut != StatutDemande.EN_COURS
                && statut != StatutDemande.EN_VALIDATION && statut != StatutDemande.TERMINEE) {
            throw new BusinessRuleException("DEMANDE_NON_FACTURABLE",
                    "Cette demande n'est pas facturable (statut: " + demande.getStatut() + ")");
        }

        if (factureRepository.existsByDemandeId(demandeId)) {
            throw new BusinessRuleException("FACTURE_DEJA_EMISE",
                    "Une facture existe déjà pour cette demande");
        }

        List<LigneEssai> lignesEssai = ligneEssaiRepository.findByDemande_Id(demandeId);
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
        LocalDate echeance = dateEcheance != null
                ? dateEcheance
                : dateFacture.plusDays(delaiEcheanceJours);

        Facture facture = Facture.builder()
                .numero(numero)
                .type("STANDARD")
                .dateFacture(dateFacture)
                .dateEcheance(echeance)
                .montantHt(montantTotal)
                .tauxCommission(tauxCommission)
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

        enregistrerAuditEmission(facture, nomSchema, acteur, automatique);

        return toAdminDto(facture);
    }

    private void enregistrerAuditEmission(
            Facture facture, String nomSchema, UtilisateurPrincipal acteur, boolean automatique) {
        Utilisateur utilisateur = tenantExecutor.inCentral(() ->
                utilisateurRepository.findByEmail(acteur.getEmail()).orElse(null));
        if (utilisateur == null) {
            throw new BusinessRuleException("ACTEUR_AUDIT_INTRouvable",
                    "L'acteur de l'émission est introuvable pour l'audit.");
        }
        JournalAudit entree = JournalAudit.builder()
                .code("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .dateAction(Instant.now())
                .action("EMISSION_FACTURE")
                .objet(nomSchema + "/Facture#" + facture.getId())
                .valeurApres("{\"statut\":\"" + facture.getStatut() + "\"}")
                .motif(automatique ? "AUTO_ACCEPTATION" : null)
                .utilisateur(utilisateur)
                .build();
        journalAuditService.enregistrer(entree);
    }

    private String schemaCourant() {
        String nomSchema = SecurityUtils.principalCourant().getNomSchema();
        if (nomSchema == null || nomSchema.isBlank()) {
            throw new BusinessRuleException("SCHEMA_LABORATOIRE_REQUIS",
                    "Le schéma du laboratoire est obligatoire.");
        }
        return nomSchema;
    }

    @Transactional
    public FactureAdminDto enregistrerPaiement(Long id, EnregistrerPaiementRequest request) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", id));

        BigDecimal dejaPaye = paiementRepository.sumByFactureId(id);
        if (dejaPaye == null) dejaPaye = BigDecimal.ZERO;

        if (StatutFacture.ANNULEE.name().equals(facture.getStatut())) {
            throw new BusinessRuleException("FACTURE_ANNULEE", "Cette facture est annulée par l'avoir "
                    + facture.getNumeroAvoir() + " : aucun paiement n'est possible.");
        }
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

        BigDecimal taux = facture.getTauxCommission() != null
                ? facture.getTauxCommission() : parametrePlateformeService.tauxCommission();
        boolean annulee = StatutFacture.ANNULEE.name().equals(facture.getStatut());
        // Une facture annulée ne rapporte rien : ni au laboratoire, ni à la plateforme.
        BigDecimal commission = annulee ? BigDecimal.ZERO.setScale(2)
                : com.backend.modules.plateforme.service.ParametrePlateformeService.commissionSur(facture.getMontantHt(), taux);
        BigDecimal net = facture.getMontantHt() == null || annulee ? BigDecimal.ZERO.setScale(2)
                : facture.getMontantHt().subtract(commission);

        return FactureAdminDto.builder()
                .id(facture.getId())
                .tauxCommission(taux)
                .montantCommission(commission)
                .montantNetLaboratoire(net)
                .numeroAvoir(facture.getNumeroAvoir())
                .dateAnnulation(facture.getDateAnnulation())
                .motifAnnulation(facture.getMotifAnnulation())
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
