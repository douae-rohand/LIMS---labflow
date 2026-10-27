package com.backend.modules.demande.service;

import com.backend.common.audit.JournalAudit;
import com.backend.common.audit.JournalAuditRepository;
import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.exception.UnauthorizedTenantException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.client.entity.Client;
import com.backend.modules.client.entity.ClientLaboratoire;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.client.repository.ClientRepository;
import com.backend.modules.client.service.ClientService;
import com.backend.common.dto.PageResponse;
import com.backend.modules.demande.dto.AnalyseDemandeeDto;
import com.backend.modules.demande.dto.ClientDemandeDto;
import com.backend.modules.demande.dto.DemandeDto;
import com.backend.modules.demande.dto.DemandeDetailClientDto;
import com.backend.modules.demande.dto.DemandeLaboratoireDto;
import com.backend.modules.demande.dto.DemandeResumeDto;
import com.backend.modules.demande.dto.DocumentDemandeDto;
import com.backend.modules.demande.dto.EchantillonClientDto;
import com.backend.modules.demande.dto.EchantillonDemandeDto;
import com.backend.modules.demande.dto.HistoriqueEntreeDto;
import com.backend.modules.demande.dto.IndicateursDemandesDto;
import com.backend.modules.demande.dto.LigneEssaiClientDto;
import com.backend.modules.demande.dto.PatientDemandeDto;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.PieceJointe;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.demande.repository.PieceJointeRepository;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.echantillon.repository.EchantillonRepository;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.repository.EssaiRepository;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.panier.entity.LignePanier;
import com.backend.modules.panier.entity.Panier;
import com.backend.modules.panier.repository.LignePanierRepository;
import com.backend.modules.panier.repository.PanierRepository;
import com.backend.modules.patient.entity.Patient;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final ClientLaboratoireRepository clientLaboratoireRepository;
    private final TenantExecutor tenantExecutor;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final EchantillonRepository echantillonRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PanierRepository panierRepository;
    private final LignePanierRepository lignePanierRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final EssaiRepository essaiRepository;
    private final JournalAuditService journalAuditService;
    private final JournalAuditRepository journalAuditRepository;

    private static final String STATUT_LABORATOIRE_ACTIF = "ACTIF";
    private static final int MOTIF_MIN = 5;
    private static final int MOTIF_MAX = 2000;
    private static final int PAGE_SIZE_MAX = 50;
    private static final int MAX_ESSAIS_NUMERO = 3;
    private static final DateTimeFormatter FORMAT_PREFIXE = DateTimeFormatter.ofPattern("yyyyMM");

    // ------------------------------------------------------------------
    // Endpoints client : création, annulation, détail
    // ------------------------------------------------------------------

    /**
     * Crée une demande depuis le panier central du client pour le laboratoire cible.
     *
     * <p>La demande et toutes ses lignes sont créées dans un même {@code inTenant}.
     * En cas de conflit sur le numéro ({@code uk_demande_numero}), jusqu'à 3 essais
     * sont effectués, chacun dans une nouvelle transaction.
     */
    public DemandeDetailClientDto creerDepuisPanier(Long utilisateurId, Long laboratoireId,
                                                    String titre, String objectif) {
        Laboratoire labo = exigerLaboratoireActif(laboratoireId);
        String nomSchema = labo.getNomSchema();

        List<Long> essaiIds = tenantExecutor.inCentral(() -> {
            Optional<Panier> panierOpt = panierRepository
                    .findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId);
            if (panierOpt.isEmpty()) {
                return List.<Long>of();
            }
            return lignePanierRepository.findByPanierId(panierOpt.get().getId())
                    .stream()
                    .map(LignePanier::getEssaiId)
                    .toList();
        });

        if (essaiIds.isEmpty()) {
            throw new BusinessRuleException("PANIER_VIDE", "Le panier est vide.");
        }

        Map<Long, Essai> essaisValides = tenantExecutor.inTenant(nomSchema, () -> {
            Map<Long, Essai> map = essaiRepository.findByIdsAvecDomaine(essaiIds).stream()
                    .collect(Collectors.toMap(Essai::getId, e -> e, (a, b) -> a));
            List<String> codesIndisponibles = new ArrayList<>();
            Map<Long, Essai> valides = new HashMap<>();
            for (Long id : essaiIds) {
                Essai e = map.get(id);
                if (e == null || !Boolean.TRUE.equals(e.getActif()) || e.getTarif() == null) {
                    codesIndisponibles.add(e == null ? "essai#" + id : e.getCode());
                } else {
                    valides.put(id, e);
                }
            }
            if (!codesIndisponibles.isEmpty()) {
                throw new BusinessRuleException("ESSAI_INDISPONIBLE",
                        "Essais indisponibles : " + String.join(", ", codesIndisponibles));
            }
            return valides;
        });

        Client fiche = clientService.assurerFicheLocale(utilisateurId, nomSchema);

        String prefixe = LocalDate.now().format(FORMAT_PREFIXE) + "-";
        String suffixeAleatoire = "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Demande demande = null;
        for (int tentative = 0; tentative < MAX_ESSAIS_NUMERO; tentative++) {
            try {
                demande = tenantExecutor.inTenant(nomSchema, () -> {
                    long maxSeq = demandeRepository.findMaxSequenceForPrefix(prefixe);
                    String numero = prefixe + String.format("%03d", maxSeq + 1) + suffixeAleatoire;
                    Client client = clientRepository.findById(fiche.getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Client", "id", fiche.getId()));
                    Demande d = Demande.builder()
                            .numero(numero)
                            .titre(titre)
                            .objectif(objectif)
                            .dateSoumission(Instant.now())
                            .statut(StatutDemande.NOUVEAU.name())
                            .client(client)
                            .build();
                    d = demandeRepository.save(d);
                    int index = 1;
                    for (Long essaiId : essaiIds) {
                        Essai ref = essaiRepository.getReferenceById(essaiId);
                        LigneEssai ligne = LigneEssai.builder()
                                .code(d.getNumero() + "-L" + index)
                                .statut(null)
                                .montant(essaisValides.get(essaiId).getTarif())
                                .demande(d)
                                .essai(ref)
                                .build();
                        ligneEssaiRepository.save(ligne);
                        index++;
                    }
                    return d;
                });
                break;
            } catch (DataIntegrityViolationException ex) {
                log.warn("Conflit sur le numéro de demande (tentative {}/{}).", tentative + 1, MAX_ESSAIS_NUMERO);
                if (tentative == MAX_ESSAIS_NUMERO - 1) {
                    throw new BusinessRuleException("NUMERO_INDISPONIBLE",
                            "Impossible de générer un numéro de demande, réessayez.");
                }
            }
        }

        viderPanierApresCommit(utilisateurId, laboratoireId, demande.getNumero());

        enregistrerAuditDemande("CREATION_DEMANDE", nomSchema, demande.getId(),
                null, StatutDemande.NOUVEAU.name(), null);

        return obtenirDetailClient(utilisateurId, laboratoireId, demande.getId());
    }

    /**
     * Annule une demande au statut NOUVEAU. Transition atomique.
     */
    public DemandeDetailClientDto annuler(Long utilisateurId, Long laboratoireId,
                                          Long demandeId, String motif) {
        Laboratoire labo = exigerLaboratoireExistant(laboratoireId);
        String nomSchema = labo.getNomSchema();
        String motifPropre = motif.trim();

        int misesAJour = tenantExecutor.inTenant(nomSchema, () -> {
            Demande demande = demandeRepository.findByIdAndClient_UtilisateurId(demandeId, utilisateurId)
                    .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId));
            if (!StatutDemande.estNouvelle(demande.getStatut())) {
                throw new BusinessRuleException("ANNULATION_IMPOSSIBLE",
                        "Cette demande ne peut plus être annulée.");
            }
            return demandeRepository.annulerSiNouvelle(demandeId, motifPropre,
                    StatutDemande.NOUVEAU.aliasesPersistés());
        });

        if (misesAJour == 0) {
            throw new BusinessRuleException("ANNULATION_IMPOSSIBLE",
                    "Cette demande ne peut plus être annulée.");
        }

        enregistrerAuditDemande("ANNULATION_DEMANDE", nomSchema, demandeId,
                StatutDemande.NOUVEAU.name(), StatutDemande.ANNULEE.name(), motifPropre);

        return obtenirDetailClient(utilisateurId, laboratoireId, demandeId);
    }

    /**
     * Détail enrichi d'une demande du client. N'expose pas de limites de référence,
     * de seuils, d'identifiants d'utilisateurs ni de commentaires internes.
     */
    public DemandeDetailClientDto obtenirDetailClient(Long utilisateurId, Long laboratoireId, Long demandeId) {
        Laboratoire labo = exigerLaboratoireExistant(laboratoireId);
        String nomSchema = labo.getNomSchema();

        Demande demande = tenantExecutor.inTenant(nomSchema, () ->
                demandeRepository.findByIdAndClient_UtilisateurId(demandeId, utilisateurId)
                        .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", demandeId)));

        List<LigneEssaiClientDto> lignesDto = tenantExecutor.inTenant(nomSchema, () ->
                ligneEssaiRepository.findByDemande_IdIn(List.of(demandeId)).stream()
                        .map(this::toLigneClient)
                        .toList());

        List<EchantillonClientDto> echantillonsDto = tenantExecutor.inTenant(nomSchema, () ->
                echantillonRepository.findByDemande_Id(demandeId, PageRequest.of(0, 50))
                        .getContent().stream()
                        .map(this::toEchantillonClient)
                        .toList());

        String objetAudit = nomSchema + "/Demande#" + demandeId;
        List<HistoriqueEntreeDto> historiqueDto = tenantExecutor.inCentral(() ->
                journalAuditRepository.findByObjet(objetAudit,
                                PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "dateAction")))
                        .getContent().stream()
                        .map(ja -> toHistorique(ja, utilisateurId))
                        .toList());

        BigDecimal total = lignesDto.stream()
                .map(LigneEssaiClientDto::getTarif)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DemandeDetailClientDto.builder()
                .id(demande.getId())
                .reference(demande.getNumero())
                .titre(demande.getTitre())
                .objectif(demande.getObjectif())
                .statut(StatutDemande.depuis(demande.getStatut()))
                .dateSoumission(demande.getDateSoumission())
                .motif(demande.getMotif())
                .laboratoireNom(labo.getRaisonSociale())
                .lignes(lignesDto)
                .total(total)
                .echantillons(echantillonsDto)
                .historique(historiqueDto)
                .build();
    }

    // ------------------------------------------------------------------
    // Liste des demandes du client (tous laboratoires)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DemandeDto trouverParId(Long id) {
        Demande demande = charger(id);
        verifierProprieteClient(demande);
        return toDto(demande, TenantContext.getCurrentTenant(), null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerParClient(Long clientId, Pageable pageable) {
        return demandeRepository.findByClient_Id(clientId, pageable)
                .map(d -> toDto(d, TenantContext.getCurrentTenant(), null, null, null));
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerMesDemandes(Pageable pageable) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        return demandeRepository.findByClient_UtilisateurId(principal.getId(), pageable)
                .map(d -> toDto(d, TenantContext.getCurrentTenant(), null, null, null));
    }

    /**
     * Liste toutes les demandes du client, tous laboratoires confondus.
     * Un laboratoire défaillant (schema inaccessible, suspendu) ne casse pas la liste.
     */
    public List<DemandeDto> listerToutesMesDemandes() {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        List<ClientLaboratoire> liaisons = tenantExecutor.inCentral(
                () -> clientLaboratoireRepository.findByUtilisateur_Id(principal.getId()));
        List<DemandeDto> resultats = new ArrayList<>();
        for (ClientLaboratoire liaison : liaisons) {
            String nomSchema = liaison.getLaboratoire().getNomSchema();
            String code = liaison.getLaboratoire().getCode();
            String nom = liaison.getLaboratoire().getRaisonSociale();
            try {
                List<DemandeDto> duTenant = tenantExecutor.inTenant(nomSchema, () -> {
                    List<Demande> demandes = demandeRepository.findByClient_UtilisateurId(principal.getId());
                    if (demandes.isEmpty()) {
                        return List.<DemandeDto>of();
                    }
                    List<Long> demandeIds = demandes.stream().map(Demande::getId).toList();
                    Map<Long, BigDecimal> totaux = ligneEssaiRepository
                            .findByDemande_IdIn(demandeIds).stream()
                            .collect(Collectors.groupingBy(
                                    l -> l.getDemande().getId(),
                                    Collectors.reducing(BigDecimal.ZERO, LigneEssai::getMontant, BigDecimal::add)));
                    return demandes.stream()
                            .map(d -> toDto(d, code, nom, totaux.getOrDefault(d.getId(), BigDecimal.ZERO),
                                    liaison.getLaboratoire().getId()))
                            .toList();
                });
                resultats.addAll(duTenant);
            } catch (Exception e) {
                log.warn("Impossible de lire les demandes du laboratoire {} ({}).", code, e.getMessage());
            }
        }
        resultats.sort(Comparator.comparing(DemandeDto::getDateSoumission,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return resultats;
    }

    @Transactional(readOnly = true)
    public Page<DemandeDto> listerParStatut(StatutDemande statut, Pageable pageable) {
        return demandeRepository.findByStatut(statut.name(), pageable)
                .map(d -> toDto(d, TenantContext.getCurrentTenant(), null, null, null));
    }

    // ------------------------------------------------------------------
    // Endpoints laboratoire
    // ------------------------------------------------------------------

    @Transactional
    public DemandeDto changerStatut(Long id, StatutDemande statut) {
        if (statut == StatutDemande.ACCEPTEE) {
            Demande acceptee = enregistrerAcceptation(id);
            enregistrerAuditDemande("ACCEPTATION_DEMANDE",
                    TenantContext.getCurrentTenant(), id,
                    StatutDemande.NOUVEAU.name(), StatutDemande.ACCEPTEE.name(), null);
            return toDto(acceptee, TenantContext.getCurrentTenant(), null, null, null);
        }
        if (statut == StatutDemande.REFUSEE) {
            throw new BusinessRuleException("MOTIF_OBLIGATOIRE",
                    "Le refus d'une demande exige un motif.");
        }
        Demande demande = charger(id);
        demande.setStatut(statut.name());
        return toDto(demandeRepository.save(demande), TenantContext.getCurrentTenant(), null, null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<DemandeResumeDto> listerPourLaboratoire(String q, String statut, int page, int size) {
        String recherche = normaliserRecherche(q);
        StatutDemande statutFiltre = normaliserStatutFiltre(statut);
        boolean filtrerStatut = statutFiltre != null;
        List<String> statuts = filtrerStatut ? statutFiltre.aliasesPersistés() : List.of("_");
        int pageSafe = Math.max(page, 0);
        int sizeSafe = size < 1 ? 20 : Math.min(size, PAGE_SIZE_MAX);
        Pageable pageable = PageRequest.of(pageSafe, sizeSafe,
                Sort.by(Sort.Direction.DESC, "dateSoumission"));
        Page<DemandeResumeDto> resultat = demandeRepository.rechercher(recherche, filtrerStatut, statuts, pageable)
                .map(this::toResume);
        return PageResponse.from(resultat);
    }

    @Transactional(readOnly = true)
    public IndicateursDemandesDto indicateursLaboratoire() {
        long total = demandeRepository.count();
        return IndicateursDemandesDto.builder()
                .enAttente(compter(StatutDemande.NOUVEAU))
                .acceptees(compter(StatutDemande.ACCEPTEE))
                .refusees(compter(StatutDemande.REFUSEE))
                .total(total)
                .build();
    }

    @Transactional(readOnly = true)
    public DemandeLaboratoireDto detailLaboratoire(Long id) {
        return construireDetail(charger(id));
    }

    @Transactional
    public DemandeLaboratoireDto accepter(Long id) {
        Demande demande = enregistrerAcceptation(id);
        enregistrerAuditDemande("ACCEPTATION_DEMANDE",
                TenantContext.getCurrentTenant(), id,
                StatutDemande.NOUVEAU.name(), StatutDemande.ACCEPTEE.name(), null);
        return construireDetail(demandeRepository.save(demande));
    }

    @Transactional
    public DemandeLaboratoireDto refuser(Long id, String motif) {
        Demande demande = charger(id);
        exigerNouvelle(demande);
        appliquerDecision(demande, StatutDemande.REFUSEE, validerMotif(motif));
        Demande sauvegardee = demandeRepository.save(demande);
        enregistrerAuditDemande("REFUS_DEMANDE",
                TenantContext.getCurrentTenant(), id,
                StatutDemande.NOUVEAU.name(), StatutDemande.REFUSEE.name(), sauvegardee.getMotif());
        return construireDetail(sauvegardee);
    }

    private Demande enregistrerAcceptation(Long id) {
        Demande demande = charger(id);
        exigerNouvelle(demande);
        appliquerDecision(demande, StatutDemande.ACCEPTEE, null);
        return demandeRepository.save(demande);
    }

    private void exigerNouvelle(Demande demande) {
        if (!StatutDemande.estNouvelle(demande.getStatut())) {
            throw new BusinessRuleException("DEMANDE_DEJA_TRAITEE",
                    "Cette demande ne peut plus être acceptée ou refusée.");
        }
    }

    private void appliquerDecision(Demande demande, StatutDemande statut, String motif) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        demande.setStatut(statut.name());
        demande.setDateDecision(Instant.now());
        demande.setDecideurId(principal.getId());
        demande.setMotif(motif);
    }

    private String validerMotif(String motif) {
        if (motif == null || motif.isBlank()) {
            throw new BusinessRuleException("MOTIF_OBLIGATOIRE",
                    "Le motif du refus est obligatoire.");
        }
        String propre = motif.trim();
        if (propre.length() < MOTIF_MIN) {
            throw new BusinessRuleException("MOTIF_TROP_COURT",
                    "Le motif doit contenir au moins 5 caractères.");
        }
        if (propre.length() > MOTIF_MAX) {
            throw new BusinessRuleException("MOTIF_TROP_LONG",
                    "Le motif ne peut pas dépasser 2000 caractères.");
        }
        return propre;
    }

    private String normaliserRecherche(String q) {
        if (q == null || q.isBlank()) {
            return "";
        }
        String trimmed = q.trim();
        if (trimmed.length() > 100) {
            throw new BusinessRuleException("RECHERCHE_TROP_LONGUE",
                    "La recherche ne peut pas dépasser 100 caractères.");
        }
        return trimmed;
    }

    private StatutDemande normaliserStatutFiltre(String statut) {
        if (statut == null || statut.isBlank()) {
            return null;
        }
        StatutDemande lu = StatutDemande.depuis(statut.trim());
        if (lu == null) {
            throw new BusinessRuleException("STATUT_INVALIDE", "Statut de demande inconnu.");
        }
        return lu;
    }

    private long compter(StatutDemande statut) {
        long total = 0;
        for (String alias : statut.aliasesPersistés()) {
            total += demandeRepository.countByStatut(alias);
        }
        return total;
    }

    private DemandeResumeDto toResume(Demande demande) {
        return DemandeResumeDto.builder()
                .id(demande.getId())
                .reference(demande.getNumero())
                .objet(demande.getTitre())
                .statut(StatutDemande.depuis(demande.getStatut()))
                .demandeur(libelleDemandeur(demande))
                .dateSoumission(demande.getDateSoumission())
                .dateDecision(demande.getDateDecision())
                .decisionPossible(StatutDemande.estNouvelle(demande.getStatut()))
                .build();
    }

    private String libelleDemandeur(Demande demande) {
        if (demande.getClient() != null && demande.getClient().getRaisonSociale() != null
                && !demande.getClient().getRaisonSociale().isBlank()) {
            return demande.getClient().getRaisonSociale();
        }
        if (demande.getPatient() != null) {
            return nomComplet(demande.getPatient().getPrenom(), demande.getPatient().getNom());
        }
        return null;
    }

    private DemandeLaboratoireDto construireDetail(Demande demande) {
        Long id = demande.getId();
        List<LigneEssai> lignes = ligneEssaiRepository.findByDemande_Id(id);
        List<PieceJointe> pieces = pieceJointeRepository.findByDemande_Id(id);
        List<Echantillon> echantillons = echantillonRepository
                .findByDemande_Id(id, PageRequest.of(0, 50))
                .getContent();

        Client client = demande.getClient();
        Patient patient = demande.getPatient();
        for (LigneEssai ligne : lignes) {
            Essai essai = ligne.getEssai();
            if (essai != null) {
                essai.getDesignation();
            }
        }

        Set<Long> utilisateurIds = new HashSet<>();
        if (demande.getDecideurId() != null) {
            utilisateurIds.add(demande.getDecideurId());
        }
        if (client != null && client.getUtilisateurId() != null) {
            utilisateurIds.add(client.getUtilisateurId());
        }
        Map<Long, Utilisateur> utilisateurs = chargerUtilisateurs(utilisateurIds);
        Utilisateur contactClient = client == null || client.getUtilisateurId() == null
                ? null
                : utilisateurs.get(client.getUtilisateurId());
        Utilisateur decideur = demande.getDecideurId() == null
                ? null
                : utilisateurs.get(demande.getDecideurId());

        return DemandeLaboratoireDto.builder()
                .id(demande.getId())
                .reference(demande.getNumero())
                .objet(demande.getTitre())
                .objectif(demande.getObjectif())
                .statut(StatutDemande.depuis(demande.getStatut()))
                .dateSoumission(demande.getDateSoumission())
                .dateDecision(demande.getDateDecision())
                .motif(demande.getMotif())
                .decideurId(demande.getDecideurId())
                .decideurNom(decideur == null ? null : nomComplet(decideur.getPrenom(), decideur.getNom()))
                .decisionPossible(StatutDemande.estNouvelle(demande.getStatut()))
                .client(client == null ? null : toClientDto(client, contactClient))
                .patient(patient == null ? null : toPatientDto(patient))
                .analyses(lignes.stream().map(this::toAnalyse).toList())
                .echantillons(echantillons.stream().map(this::toEchantillon).toList())
                .documents(pieces.stream().map(this::toDocument).toList())
                .build();
    }

    private Map<Long, Utilisateur> chargerUtilisateurs(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return tenantExecutor.inCentral(() -> {
            Map<Long, Utilisateur> map = new HashMap<>();
            utilisateurRepository.findAllById(ids).forEach(u -> map.put(u.getId(), u));
            return map;
        });
    }

    private ClientDemandeDto toClientDto(Client client, Utilisateur contact) {
        return ClientDemandeDto.builder()
                .id(client.getId())
                .code(client.getCode())
                .raisonSociale(client.getRaisonSociale())
                .ice(client.getIce())
                .adresse(client.getAdresse())
                .nomComplet(contact == null ? null : nomComplet(contact.getPrenom(), contact.getNom()))
                .email(contact == null ? null : contact.getEmail())
                .telephone(contact == null ? null : contact.getTelephone())
                .build();
    }

    private PatientDemandeDto toPatientDto(Patient patient) {
        return PatientDemandeDto.builder()
                .id(patient.getId())
                .code(patient.getCode())
                .nomComplet(nomComplet(patient.getPrenom(), patient.getNom()))
                .dateNaissance(patient.getDateNaissance())
                .telephone(patient.getTelephone())
                .email(patient.getEmail())
                .build();
    }

    private AnalyseDemandeeDto toAnalyse(LigneEssai ligne) {
        Essai essai = ligne.getEssai();
        return AnalyseDemandeeDto.builder()
                .id(ligne.getId())
                .code(essai == null ? ligne.getCode() : essai.getCode())
                .designation(essai == null ? null : essai.getDesignation())
                .methode(essai == null ? null : essai.getMethode())
                .unite(essai == null ? null : essai.getUnite())
                .tarif(essai == null ? null : essai.getTarif())
                .montant(ligne.getMontant())
                .statut(ligne.getStatut() == null ? null : ligne.getStatut().name())
                .build();
    }

    private EchantillonDemandeDto toEchantillon(Echantillon echantillon) {
        return EchantillonDemandeDto.builder()
                .id(echantillon.getId())
                .reference(echantillon.getReference())
                .nature(echantillon.getNature())
                .dateReception(echantillon.getDateReception())
                .conformite(echantillon.getConformite())
                .build();
    }

    private DocumentDemandeDto toDocument(PieceJointe piece) {
        return DocumentDemandeDto.builder()
                .id(piece.getId())
                .code(piece.getCode())
                .nomFichier(piece.getNomFichier())
                .typeMime(piece.getTypeMime())
                .taille(piece.getTaille())
                .dateAjout(piece.getDateAjout())
                .build();
    }

    private static String nomComplet(String prenom, String nom) {
        String gauche = prenom == null ? "" : prenom.trim();
        String droite = nom == null ? "" : nom.trim();
        String complet = (gauche + " " + droite).trim();
        return complet.isEmpty() ? null : complet;
    }

    private Demande charger(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande", "id", id));
    }

    private void verifierProprieteClient(Demande demande) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        if (!principal.isClient()) {
            return;
        }
        Long proprietaire = demande.getClient() == null ? null : demande.getClient().getUtilisateurId();
        if (proprietaire == null || !proprietaire.equals(principal.getId())) {
            throw new UnauthorizedTenantException("Cette demande ne vous appartient pas");
        }
    }

    private DemandeDto toDto(Demande d, String laboratoireCode, String laboratoireNom,
                             BigDecimal total, Long laboratoireId) {
        Long clientId = d.getClient() == null ? null : d.getClient().getId();
        return DemandeDto.builder()
                .id(d.getId())
                .reference(d.getNumero())
                .objet(d.getTitre())
                .statut(StatutDemande.depuis(d.getStatut()))
                .clientId(clientId)
                .laboratoireId(laboratoireId)
                .laboratoireCode(laboratoireCode)
                .laboratoireNom(laboratoireNom)
                .dateSoumission(d.getDateSoumission())
                .commentaire(d.getObjectif())
                .total(total)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers client (laboratoire, panier, audit, mapping)
    // ------------------------------------------------------------------

    private Laboratoire exigerLaboratoireExistant(Long laboratoireId) {
        return tenantExecutor.inCentral(() ->
                laboratoireRepository.findById(laboratoireId)
                        .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", laboratoireId)));
    }

    private Laboratoire exigerLaboratoireActif(Long laboratoireId) {
        return tenantExecutor.inCentral(() -> {
            Laboratoire l = laboratoireRepository
                    .findByIdAndStatut(laboratoireId, STATUT_LABORATOIRE_ACTIF)
                    .orElse(null);
            if (l != null) {
                return l;
            }
            if (laboratoireRepository.existsById(laboratoireId)) {
                throw new BusinessRuleException(
                        "LABORATOIRE_INDISPONIBLE", "Le laboratoire n'est pas actif.");
            }
            throw new ResourceNotFoundException("Laboratoire", "id", laboratoireId);
        });
    }

    private void viderPanierApresCommit(Long utilisateurId, Long laboratoireId, String numeroDemande) {
        try {
            tenantExecutor.inCentral(() -> {
                Optional<Panier> panierOpt = panierRepository
                        .findByUtilisateurIdAndLaboratoireId(utilisateurId, laboratoireId);
                panierOpt.ifPresent(p -> panierRepository.deleteById(p.getId()));
                return null;
            });
        } catch (Exception e) {
            log.warn("Impossible de vider le panier après création de la demande {} : {}",
                    numeroDemande, e.getMessage());
        }
    }

    private LigneEssaiClientDto toLigneClient(LigneEssai ligne) {
        Essai essai = ligne.getEssai();
        return LigneEssaiClientDto.builder()
                .essaiId(essai == null ? null : essai.getId())
                .code(ligne.getCode())
                .designation(essai == null ? null : essai.getDesignation())
                .domaineLibelle(essai == null || essai.getDomaine() == null ? null : essai.getDomaine().getLibelle())
                .tarif(ligne.getMontant())
                .statut(ligne.getStatut() == null ? null : ligne.getStatut().name())
                .build();
    }

    private EchantillonClientDto toEchantillonClient(Echantillon e) {
        return EchantillonClientDto.builder()
                .reference(e.getReference())
                .nature(e.getNature())
                .conformite(e.getConformite())
                .motif(e.getMotif())
                .build();
    }

    private HistoriqueEntreeDto toHistorique(JournalAudit ja, Long utilisateurId) {
        String acteur;
        if (ja.getUtilisateur() != null && ja.getUtilisateur().getId().equals(utilisateurId)) {
            acteur = "Vous";
        } else {
            acteur = "Laboratoire";
        }
        return HistoriqueEntreeDto.builder()
                .action(ja.getAction())
                .statutAvant(extraireStatut(ja.getValeurAvant()))
                .statutApres(extraireStatut(ja.getValeurApres()))
                .motif(ja.getMotif())
                .dateAction(ja.getDateAction())
                .acteur(acteur)
                .build();
    }

    private String extraireStatut(String json) {
        if (json == null) {
            return null;
        }
        int idx = json.indexOf("\"statut\":\"");
        if (idx < 0) {
            return null;
        }
        int start = idx + "\"statut\":\"".length();
        int end = json.indexOf("\"", start);
        return end < 0 ? null : json.substring(start, end);
    }

    /**
     * Enregistre une entrée d'audit pour une transition de statut de demande.
     * L'objet est qualifié par le schéma du laboratoire : {@code <nomSchema>/Demande#<id>}.
     * L'acteur est résolu dans le thread appelant. Si une transaction est active
     * (méthodes laboratoire @Transactional), l'enregistrement est différé à afterCommit ;
     * sinon il est exécuté immédiatement (méthodes client hors transaction).
     */
    private void enregistrerAuditDemande(String action, String nomSchema, Long demandeId,
                                         String statutAvant, String statutApres, String motif) {
        JournalAudit entree = construireEntreeAudit(action, nomSchema, demandeId,
                statutAvant, statutApres, motif);
        if (entree == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    journalAuditService.enregistrer(entree);
                }
            });
        } else {
            journalAuditService.enregistrer(entree);
        }
    }

    private JournalAudit construireEntreeAudit(String action, String nomSchema, Long demandeId,
                                               String statutAvant, String statutApres, String motif) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        Utilisateur auteur = utilisateurRepository.findByEmail(principal.getEmail()).orElse(null);
        if (auteur == null) {
            log.warn("Audit {} ignoré : utilisateur introuvable pour {}", action, principal.getEmail());
            return null;
        }
        String objet = nomSchema + "/Demande#" + demandeId;
        String avant = statutAvant == null ? null : "{\"statut\":\"" + statutAvant + "\"}";
        String apres = statutApres == null ? null : "{\"statut\":\"" + statutApres + "\"}";
        return JournalAudit.builder()
                .code("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .dateAction(Instant.now())
                .action(action)
                .objet(objet)
                .valeurAvant(avant)
                .valeurApres(apres)
                .motif(motif)
                .utilisateur(auteur)
                .build();
    }
}
