package com.backend.modules.accueil.service;

import com.backend.common.audit.JournalAudit;
import com.backend.common.audit.JournalAuditRepository;
import com.backend.common.dto.PageResponse;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.accueil.repository.CaisseRepository;
import com.backend.modules.demande.dto.AnalyseDemandeeDto;
import com.backend.modules.demande.dto.DemandeLaboratoireDto;
import com.backend.modules.demande.dto.DemandeResumeDto;
import com.backend.modules.demande.service.DemandeService;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.Paiement;
import com.backend.modules.facturation.entity.StatutFacture;
import com.backend.modules.rapport.entity.Rapport;
import com.backend.modules.rapport.repository.RapportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Suivi des demandes en lecture seule pour l'accueil : renseigner un client au guichet
 * (statut, historique, échantillons, analyses, facture, rapport) sans pouvoir rien modifier.
 */
@Service
@RequiredArgsConstructor
public class SuiviAccueilService {

    private static final Map<String, String> LIBELLES = Map.ofEntries(
            Map.entry("CREATION_DEMANDE", "Demande soumise"),
            Map.entry("ACCEPTATION_DEMANDE", "Demande acceptée"),
            Map.entry("REFUS_DEMANDE", "Demande refusée"),
            Map.entry("REFUS_AUTOMATIQUE_DEMANDE", "Refusée automatiquement (48 h sans échantillon)"),
            Map.entry("ANNULATION_DEMANDE", "Demande annulée par le client"),
            Map.entry("CHANGEMENT_STATUT_DEMANDE", "Changement de statut"));

    private final DemandeService demandeService;
    private final CaisseRepository caisseRepository;
    private final RapportRepository rapportRepository;
    private final JournalAuditRepository journalAuditRepository;
    private final TenantExecutor tenantExecutor;

    public record EtapeDto(String libelle, String action, String statutAvant, String statutApres, String motif,
                           Instant date, String acteur) {
    }

    public record AvancementDto(int analyses, int attenteEchantillon, int enCours, int terminees, int nonRealisables) {
    }

    public record FactureResumeDto(Long id, String numero, BigDecimal montant, BigDecimal paye, BigDecimal reste,
                                   String statut) {
    }

    public record RapportResumeDto(String numero, String statut, Instant dateSignature, boolean diffuse) {
    }

    public record SuiviDemandeDto(DemandeLaboratoireDto demande, AvancementDto avancement, List<EtapeDto> historique,
                                  FactureResumeDto facture, RapportResumeDto rapport) {
    }

    public PageResponse<DemandeResumeDto> lister(String q, String statut, int page, int size) {
        return demandeService.listerPourLaboratoire(q, statut, "dateSoumission", "desc", page, size);
    }

    @Transactional(readOnly = true)
    public SuiviDemandeDto detail(Long demandeId) {
        DemandeLaboratoireDto demande = demandeService.detailLaboratoire(demandeId);
        List<AnalyseDemandeeDto> analyses = demande.getAnalyses() == null ? List.of() : demande.getAnalyses();
        AvancementDto avancement = new AvancementDto(analyses.size(),
                compter(analyses, "ATTENTE_ECHANTILLON", true), compter(analyses, "EN_COURS", false),
                compter(analyses, "TERMINE", false), compter(analyses, "NON_REALISABLE", false));

        FactureResumeDto facture = caisseRepository.facturesDeLaDemande(demandeId).stream().findFirst()
                .map(this::resumeFacture).orElse(null);
        RapportResumeDto rapport = rapportRepository.findByDemande_IdInOrderByIdDesc(List.of(demandeId)).stream()
                .findFirst()
                .map(r -> new RapportResumeDto(r.getNumero(), r.getStatut(), r.getDateSignature(), Boolean.TRUE.equals(r.getDiffuse())))
                .orElse(null);

        String objet = TenantContext.getCurrentTenant() + "/Demande#" + demandeId;
        List<EtapeDto> historique = tenantExecutor.inCentral(() -> journalAuditRepository
                .findByObjet(objet, PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "dateAction")))
                .getContent().stream()
                .map(SuiviAccueilService::etape)
                .toList());
        return new SuiviDemandeDto(demande, avancement, historique, facture, rapport);
    }

    private FactureResumeDto resumeFacture(Facture f) {
        BigDecimal paye = caisseRepository.paiementsDes(List.of(f.getId())).stream()
                .map(Paiement::getMontant).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reste = StatutFacture.PAYEE.name().equals(f.getStatut()) ? BigDecimal.ZERO
                : f.getMontantHt().subtract(paye).max(BigDecimal.ZERO);
        return new FactureResumeDto(f.getId(), f.getNumero(), f.getMontantHt(), paye, reste, f.getStatut());
    }

    /** Les analyses sans statut (créées depuis le panier) attendent elles aussi leur échantillon. */
    private static int compter(List<AnalyseDemandeeDto> analyses, String statut, boolean inclureSansStatut) {
        return (int) analyses.stream()
                .filter(a -> statut.equals(a.getStatut()) || (inclureSansStatut && a.getStatut() == null))
                .count();
    }

    private static EtapeDto etape(JournalAudit ja) {
        String acteur = ja.getUtilisateur() == null ? "Système" : ja.getUtilisateur().getNomComplet();
        return new EtapeDto(LIBELLES.getOrDefault(ja.getAction(), ja.getAction()), ja.getAction(),
                statut(ja.getValeurAvant()), statut(ja.getValeurApres()), ja.getMotif(), ja.getDateAction(), acteur);
    }

    private static String statut(String json) {
        if (json == null) {
            return null;
        }
        int debut = json.indexOf("\"statut\":\"");
        if (debut < 0) {
            return null;
        }
        debut += "\"statut\":\"".length();
        int fin = json.indexOf('"', debut);
        return fin < 0 ? null : json.substring(debut, fin);
    }
}
