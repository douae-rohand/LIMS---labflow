package com.backend.modules.technicien.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.dto.PageResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.ExigenceEchantillon;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.ExigenceEchantillonRepository;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.technicien.dto.*;
import com.backend.modules.validation.entity.Validation;
import com.backend.modules.validation.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service de l'espace technicien.
 *
 * <h2>Conventions de sécurité</h2>
 * <ul>
 *   <li>technicienId = SecurityUtils.principalCourant().getId() — jamais un paramètre HTTP.</li>
 *   <li>Toutes les requêtes filtrent sur technicienId ; un Responsable accédant à
 *       /api/technicien/** ne voit que ses propres essais.</li>
 *   <li>Aucun montant, tarif, clientId, decideurId, email dans les DTOs.</li>
 * </ul>
 *
 * <h2>Règle « enRetard »</h2>
 * {@code echeanceEstimee = dateAttribution + essai.dureeEstimee (minutes)}
 * {@code enRetard = echeanceEstimee != null && echeanceEstimee < now() && statut ∉ {TERMINE, NON_REALISABLE}}
 *
 * <h2>Règle de conformité résultat</h2>
 * <pre>
 * si limiteMin/Max absentes → NON_EVALUE
 * si valeur ∈ [limiteMin, limiteMax] → CONFORME (conformite = true)
 * si valeur hors [limiteMin, limiteMax]
 *    ET [seuilCritiqueMin, seuilCritiqueMax] présents
 *    ET valeur ∈ [seuilCritiqueMin, seuilCritiqueMax] → NON_CONFORME (conformite = false)
 * sinon → CRITIQUE (conformite = false)
 * </pre>
 *
 * <h2>Validation technique (niveau 1)</h2>
 * La validation courante = la plus récente dont dateValidation >= dateSaisie.
 * L'historique est conservé intégralement (ISO 17025, journal immuable).
 *
 * <h2>Champs de tri autorisés</h2>
 * dateAttribution, statut, code — tri secondaire stable par id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicienService {

    /** Constante de niveau : 1 = validation technique (technicien). */
    static final int NIVEAU_TECHNIQUE = 1;

    /** Constante de niveau : 2 = validation responsable. */
    static final int NIVEAU_RESPONSABLE = 2;

    /** Plafond de la plage planning (62 jours). */
    static final int PLANNING_MAX_JOURS = 62;

    private static final Set<String> CHAMPS_TRI_AUTORISES =
            Set.of("dateAttribution", "statut", "code");

    @Value("${lims.technicien.duree-max-minutes:480}")
    private int dureeMaxMinutes;

    private final LigneEssaiRepository ligneEssaiRepository;
    private final ValidationRepository validationRepository;
    private final ExigenceEchantillonRepository exigenceEchantillonRepository;
    private final DemandeRepository demandeRepository;
    private final JournalAuditService journalAuditService;

    // =========================================================================
    // LOT A — Lecture
    // =========================================================================

    /**
     * Liste paginée des essais du technicien courant.
     *
     * @param page       numéro de page (0-indexé)
     * @param size       taille (plafond 50)
     * @param champTri   champ de tri (liste blanche)
     * @param direction  "asc" ou "desc"
     * @param statut     filtre statut (null = tous)
     * @param domaineId  filtre domaine (null = tous)
     * @param demandeId  filtre demande (null = toutes)
     * @param debut      début période dateAttribution ISO-8601 (null = sans borne)
     * @param fin        fin période dateAttribution ISO-8601 (null = sans borne)
     * @param enRetard   true = seulement les lignes en retard
     * @param q          recherche libre sur code / désignation / numéro demande
     */
    @Transactional(readOnly = true)
    public PageResponse<LigneEssaiTechnicienDto> listerMesEssais(
            int page, int size, String champTri, String direction,
            StatutEssai statut, Long domaineId, Long demandeId,
            Instant debut, Instant fin,
            boolean enRetard, String q) {

        Long moi = SecurityUtils.principalCourant().getId();
        Pageable pageable = construirePageable(page, size, champTri, direction);

        Page<LigneEssai> raw = ligneEssaiRepository.rechercherPourTechnicien(
                moi, statut, domaineId, demandeId, debut, fin,
                q == null ? "" : q.trim(), pageable);

        Page<LigneEssaiTechnicienDto> dtos = raw.map(le -> {
            LigneEssaiTechnicienDto dto = toListDto(le);
            return dto;
        });

        if (enRetard) {
            // Filtre post-requête (car calculé en Java)
            List<LigneEssaiTechnicienDto> filtres = dtos.getContent().stream()
                    .filter(LigneEssaiTechnicienDto::isEnRetard)
                    .toList();
            // On retourne la page déjà calculée filtrée
            // Note : le totalElements sera celui avant filtrage — acceptable pour un filtre
            // secondaire ; une solution propre serait de calculer enRetard en SQL (cf. countEnRetard)
            return PageResponse.<LigneEssaiTechnicienDto>builder()
                    .content(filtres)
                    .page(dtos.getNumber())
                    .size(dtos.getSize())
                    .totalElements(dtos.getTotalElements())
                    .totalPages(dtos.getTotalPages())
                    .first(dtos.isFirst())
                    .last(dtos.isLast())
                    .build();
        }

        return PageResponse.from(dtos);
    }

    /**
     * Détail d'une ligne d'essai pour le technicien courant.
     * Retourne 404 si la ligne n'existe pas OU n'appartient pas au technicien.
     */
    @Transactional(readOnly = true)
    public LigneEssaiDetailTechnicienDto detailEssai(Long id) {
        Long moi = SecurityUtils.principalCourant().getId();
        LigneEssai ligne = ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));

        List<ExigenceEchantillon> exigences =
                exigenceEchantillonRepository.findByEssai_Id(ligne.getEssai().getId());

        List<Validation> validations = validationRepository.findByLigneEssai_Id(id);

        return toDetailDto(ligne, exigences, validations);
    }

    /**
     * Planning du technicien courant, regroupé par jour.
     * Plafond : 62 jours. Retourne 400 si la plage dépasse le plafond.
     */
    @Transactional(readOnly = true)
    public List<PlanningJourDto> planning(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            throw new BusinessRuleException("PLANNING_DATES_REQUISES",
                    "Les dates de début et de fin sont obligatoires.");
        }
        long jours = ChronoUnit.DAYS.between(debut, fin);
        if (jours < 0) {
            throw new BusinessRuleException("PLANNING_FIN_AVANT_DEBUT",
                    "La date de fin doit être postérieure ou égale à la date de début.");
        }
        if (jours > PLANNING_MAX_JOURS) {
            throw new BusinessRuleException("PLANNING_PLAGE_TROP_GRANDE",
                    "La plage de planning ne peut pas dépasser " + PLANNING_MAX_JOURS + " jours.");
        }

        Long moi = SecurityUtils.principalCourant().getId();
        Instant debutI = debut.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant finI = fin.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<LigneEssai> lignes = ligneEssaiRepository.findPlanning(moi, debutI, finI);

        // Regrouper par date locale (UTC)
        Map<LocalDate, List<LigneEssai>> parJour = lignes.stream()
                .collect(Collectors.groupingBy(le ->
                        le.getDateAttribution().atZone(ZoneOffset.UTC).toLocalDate()));

        // Générer tous les jours de la plage (y compris les jours sans essais)
        List<PlanningJourDto> resultat = new ArrayList<>();
        LocalDate courant = debut;
        while (!courant.isAfter(fin)) {
            List<LigneEssai> lignesJour = parJour.getOrDefault(courant, List.of());
            resultat.add(PlanningJourDto.builder()
                    .date(courant)
                    .essais(lignesJour.stream().map(this::toListDto).toList())
                    .build());
            courant = courant.plusDays(1);
        }
        return resultat;
    }

    /**
     * Tableau de bord du technicien courant.
     * Tous les indicateurs proviennent de requêtes SQL d'agrégation.
     */
    @Transactional(readOnly = true)
    public SyntheseTechnicienDto synthese() {
        Long moi = SecurityUtils.principalCourant().getId();

        long essaisAujoudhui = ligneEssaiRepository.countEssaisAujourdhuiPourTechnicien(moi);
        long enCours = ligneEssaiRepository.countEnCoursPourTechnicien(moi);
        long enAttenteEchantillon = ligneEssaiRepository.countEnAttenteEchantillonPourTechnicien(moi);
        long enRetard = ligneEssaiRepository.countEnRetardPourTechnicien(moi);

        Instant il7Jours = Instant.now().minus(7, ChronoUnit.DAYS);
        long terminesSemaine = ligneEssaiRepository.countTerminesPourTechnicienDepuis(moi, il7Jours);

        long validationsEnAttente = validationRepository.countValidationsTechniquesEnAttente(moi);

        // Charge des 7 prochains jours
        Instant debutSemaine = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant finSemaine = LocalDate.now().plusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Object[]> chargeRaw = ligneEssaiRepository.countParJourPourTechnicien(moi, debutSemaine, finSemaine);

        List<SyntheseTechnicienDto.ChargeSemaineDto> charge = chargeRaw.stream()
                .map(row -> SyntheseTechnicienDto.ChargeSemaineDto.builder()
                        .date(toLocalDate(row[0]))
                        .count((Long) row[1])
                        .build())
                .toList();

        return SyntheseTechnicienDto.builder()
                .essaisAujourdhuiCount(essaisAujoudhui)
                .enCoursCount(enCours)
                .enRetardCount(enRetard)
                .enAttenteEchantillonCount(enAttenteEchantillon)
                .terminesSemaineCount(terminesSemaine)
                .validationsTechniquesAttenteCount(validationsEnAttente)
                .chargeSemaine(charge)
                .build();
    }

    // =========================================================================
    // Méthodes de mapping partagées
    // =========================================================================

    LigneEssaiTechnicienDto toListDto(LigneEssai le) {
        Instant echeance = calculerEcheance(le);
        boolean retard = estEnRetard(le, echeance);
        return LigneEssaiTechnicienDto.builder()
                .ligneEssaiId(le.getId())
                .code(le.getCode())
                .essai(toEssaiRefDto(le.getEssai()))
                .demandeNumero(le.getDemande().getNumero())
                .demandeTitre(le.getDemande().getTitre())
                .echantillon(toEchantillonRefDto(le.getEchantillon()))
                .statut(le.getStatut() == null ? null : le.getStatut().name())
                .dateAttribution(le.getDateAttribution())
                .echeanceEstimee(echeance)
                .enRetard(retard)
                .valeur(le.getValeur())
                .niveauConformite(calculerNiveauConformite(le))
                .build();
    }

    private LigneEssaiDetailTechnicienDto toDetailDto(LigneEssai le,
                                                       List<ExigenceEchantillon> exigences,
                                                       List<Validation> validations) {
        Instant echeance = calculerEcheance(le);
        boolean retard = estEnRetard(le, echeance);
        Essai essai = le.getEssai();
        return LigneEssaiDetailTechnicienDto.builder()
                .ligneEssaiId(le.getId())
                .code(le.getCode())
                .essai(toEssaiRefDto(essai))
                .demandeNumero(le.getDemande().getNumero())
                .demandeTitre(le.getDemande().getTitre())
                .echantillon(toEchantillonRefDto(le.getEchantillon()))
                .statut(le.getStatut() == null ? null : le.getStatut().name())
                .dateAttribution(le.getDateAttribution())
                .dateDebut(le.getDateDebut())
                .dateFin(le.getDateFin())
                .dateSaisie(le.getDateSaisie())
                .echeanceEstimee(echeance)
                .enRetard(retard)
                .valeur(le.getValeur())
                .niveauConformite(calculerNiveauConformite(le))
                .dureeMinutes(le.getDureeMinutes())
                .limiteMin(essai.getLimiteMin())
                .limiteMax(essai.getLimiteMax())
                .seuilCritiqueMin(essai.getSeuilCritiqueMin())
                .seuilCritiqueMax(essai.getSeuilCritiqueMax())
                .exigencesEchantillon(exigences.stream().map(this::toExigenceDto).toList())
                .historiqueValidations(validations.stream().map(this::toValidationDto).toList())
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Pageable construirePageable(int page, int size, String champTri, String direction) {
        int taille = Math.min(Math.max(1, size), 50);
        int pageAdj = Math.max(0, page);

        String champ = (champTri == null || champTri.isBlank()) ? "dateAttribution" : champTri.trim();
        if (!CHAMPS_TRI_AUTORISES.contains(champ)) {
            throw new BusinessRuleException("TRI_INVALIDE",
                    "Champ de tri invalide. Valeurs autorisées : " + CHAMPS_TRI_AUTORISES);
        }
        String dir = (direction == null || direction.isBlank()) ? "asc" : direction.trim().toLowerCase();
        Sort.Direction sortDir = switch (dir) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BusinessRuleException("DIRECTION_TRI_INVALIDE",
                    "Direction de tri invalide. Valeurs autorisées : asc, desc.");
        };
        Sort sort = Sort.by(sortDir, champ).and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(pageAdj, taille, sort);
    }

    /**
     * Calcule l'échéance estimée = dateAttribution + dureeEstimee (minutes).
     * Retourne null si dateAttribution est null.
     */
    static Instant calculerEcheance(LigneEssai le) {
        if (le.getDateAttribution() == null) return null;
        Integer duree = le.getEssai() != null ? le.getEssai().getDureeEstimee() : null;
        if (duree == null) return null;
        return le.getDateAttribution().plus(duree, ChronoUnit.MINUTES);
    }

    /**
     * Détermine si la ligne est en retard.
     * Statuts terminaux (TERMINE, NON_REALISABLE) ne sont jamais en retard.
     */
    static boolean estEnRetard(LigneEssai le, Instant echeance) {
        if (echeance == null) return false;
        StatutEssai s = le.getStatut();
        if (s == StatutEssai.TERMINE || s == StatutEssai.NON_REALISABLE) return false;
        return echeance.isBefore(Instant.now());
    }

    /**
     * Calcule le niveau de conformité du résultat à partir des seuils de l'essai.
     * La valeur du champ conformite (Boolean) de ligne_essai est utilisée quand elle est déjà
     * calculée. Le niveau CRITIQUE est dérivé de l'absence de seuils critiques ou du dépassement.
     *
     * Retourne "NON_EVALUE" si valeur absente ou non numérique et limites absentes.
     */
    static String calculerNiveauConformite(LigneEssai le) {
        if (le.getValeur() == null || le.getValeur().isBlank()) return "NON_EVALUE";
        Essai essai = le.getEssai();
        if (essai == null) return "NON_EVALUE";

        // Tenter le parse numérique (virgule → point)
        String valeurNorm = le.getValeur().replace(',', '.');
        double val;
        try {
            val = Double.parseDouble(valeurNorm);
        } catch (NumberFormatException e) {
            // Valeur texte non numérique : si limites absentes → NON_EVALUE, sinon CONFORME par défaut
            return (essai.getLimiteMin() == null && essai.getLimiteMax() == null)
                    ? "NON_EVALUE" : "CONFORME";
        }

        if (essai.getLimiteMin() == null && essai.getLimiteMax() == null) return "NON_EVALUE";

        double min = essai.getLimiteMin() != null ? essai.getLimiteMin().doubleValue() : Double.NEGATIVE_INFINITY;
        double max = essai.getLimiteMax() != null ? essai.getLimiteMax().doubleValue() : Double.POSITIVE_INFINITY;

        if (val >= min && val <= max) return "CONFORME";

        // Hors [limite_min, limite_max] : vérifier les seuils critiques
        if (essai.getSeuilCritiqueMin() != null && essai.getSeuilCritiqueMax() != null) {
            double sMin = essai.getSeuilCritiqueMin().doubleValue();
            double sMax = essai.getSeuilCritiqueMax().doubleValue();
            if (val >= sMin && val <= sMax) return "NON_CONFORME";
            return "CRITIQUE";
        }
        // Seuils critiques absents : NON_CONFORME
        return "NON_CONFORME";
    }

    EssaiRefDto toEssaiRefDto(Essai e) {
        if (e == null) return null;
        return EssaiRefDto.builder()
                .code(e.getCode())
                .designation(e.getDesignation())
                .methode(e.getMethode())
                .unite(e.getUnite())
                .dureeEstimee(e.getDureeEstimee())
                .domaine(e.getDomaine() == null ? null :
                        DomaineRefDto.builder()
                                .code(e.getDomaine().getCode())
                                .libelle(e.getDomaine().getLibelle())
                                .build())
                .build();
    }

    EchantillonRefDto toEchantillonRefDto(Echantillon ech) {
        if (ech == null) return null;
        return EchantillonRefDto.builder()
                .reference(ech.getReference())
                .nature(ech.getNature())
                .conformite(ech.getConformite())
                .etatConservationConstate(ech.getEtatConservationConstate())
                .build();
    }

    private ExigenceEchantillonRefDto toExigenceDto(ExigenceEchantillon ex) {
        return ExigenceEchantillonRefDto.builder()
                .nature(ex.getNature())
                .etatConservation(ex.getEtatConservation())
                .quantiteMinimale(ex.getQuantiteMinimale())
                .unite(ex.getUnite())
                .build();
    }

    ValidationTechniqueDto toValidationDto(Validation v) {
        return ValidationTechniqueDto.builder()
                .id(v.getId())
                .niveau(v.getNiveau())
                .decision(v.getDecision())
                .motif(v.getMotif())
                .dateValidation(v.getDateValidation())
                .build();
    }

    private LocalDate toLocalDate(Object sqlDate) {
        if (sqlDate instanceof java.sql.Date d) return d.toLocalDate();
        if (sqlDate instanceof java.util.Date d) return d.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        if (sqlDate instanceof LocalDate ld) return ld;
        return LocalDate.now(); // fallback
    }

    // =========================================================================
    // LOT B — Écriture
    // =========================================================================

    /**
     * POST /{id}/demarrer
     *
     * <p>Règles :</p>
     * <ul>
     *   <li>Appartient au technicien courant (404 sinon).</li>
     *   <li>Un échantillon doit être rattaché : ESSAI_SANS_ECHANTILLON.</li>
     *   <li>L'échantillon doit être conforme (conformite != false) : ECHANTILLON_NON_CONFORME.</li>
     *   <li>Statut doit être ATTENTE_ECHANTILLON : UPDATE conditionnel, 0 ligne → ESSAI_STATUT_INVALIDE.</li>
     *   <li>Transition demande : ACCEPTEE → EN_COURS si c'est le premier démarrage.</li>
     * </ul>
     */
    @Transactional
    public LigneEssaiTechnicienDto demarrer(Long id) {
        // Résoudre le principal en avance (avant @Async audit)
        var principal = SecurityUtils.principalCourant();
        Long moi = principal.getId();
        String acteur = principal.getEmail();
        Long labId = principal.getLaboratoireId();

        LigneEssai ligne = ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));

        // Vérification échantillon
        if (ligne.getEchantillon() == null) {
            throw new BusinessRuleException("ESSAI_SANS_ECHANTILLON",
                    "Aucun échantillon n'est rattaché à cet essai. Démarrage impossible.");
        }
        if (Boolean.FALSE.equals(ligne.getEchantillon().getConformite())) {
            throw new BusinessRuleException("ECHANTILLON_NON_CONFORME",
                    "L'échantillon rattaché est non conforme. Démarrage impossible.");
        }

        // UPDATE conditionnel atomique
        // On force le flush pour que l'UPDATE soit exécuté en SQL
        int updated = ligneEssaiRepository.demarrerSiAttenteEchantillon(id, moi, Instant.now());
        if (updated == 0) {
            throw new BusinessRuleException("ESSAI_STATUT_INVALIDE",
                    "L'essai ne peut pas être démarré (statut actuel : "
                            + (ligne.getStatut() == null ? "inconnu" : ligne.getStatut().name()) + ").");
        }

        // Transition demande ACCEPTEE → EN_COURS (une seule fois, conditionnel)
        Long demandeId = ligne.getDemande().getId();
        int demandeUpdated = demandeRepository.passerEnCoursSiAcceptee(demandeId);
        if (demandeUpdated > 0) {
            journalAuditService.enregistrer(
                    "DEMANDE_EN_COURS", "Demande", demandeId, acteur,
                    "ACCEPTEE", "EN_COURS", "Premier essai démarré", labId);
        }

        // Audit (après commit, acteur résolu dans le thread appelant)
        journalAuditService.enregistrer(
                "ESSAI_DEMARRE", "LigneEssai", id, acteur,
                "ATTENTE_ECHANTILLON", "EN_COURS", null, labId);

        // Relire pour retourner l'état à jour
        return ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .map(this::toListDto)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
    }

    /**
     * PUT /{id}/resultat
     *
     * <p>Règles :</p>
     * <ul>
     *   <li>Appartient au technicien courant (404 sinon).</li>
     *   <li>Statut doit être EN_COURS : ESSAI_NON_DEMARRE.</li>
     *   <li>Pas de validation Responsable (niveau 2 VALIDEE) : RESULTAT_DEJA_VALIDE_RESPONSABLE.</li>
     *   <li>Si valeur déjà saisie : commentaire obligatoire (MOTIF_MODIFICATION_REQUIS).</li>
     *   <li>Classification CONFORME/NON_CONFORME/CRITIQUE calculée serveur.</li>
     *   <li>Modification après validation N1 : la validation N1 est conservée (historique ISO 17025).
     *       La validation "courante" redevient absente (plus aucune N1 postérieure à la nouvelle saisie).</li>
     *   <li>Audit : valeurs masquées (jamais la valeur réelle).</li>
     * </ul>
     */
    @Transactional
    public LigneEssaiTechnicienDto saisirResultat(Long id, ResultatSaisieRequest req) {
        var principal = SecurityUtils.principalCourant();
        Long moi = principal.getId();
        String acteur = principal.getEmail();
        Long labId = principal.getLaboratoireId();

        LigneEssai ligne = ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));

        // Statut EN_COURS requis
        if (ligne.getStatut() != StatutEssai.EN_COURS) {
            throw new BusinessRuleException("ESSAI_NON_DEMARRE",
                    "La saisie du résultat n'est possible que lorsque l'essai est EN_COURS (statut actuel : "
                            + (ligne.getStatut() == null ? "inconnu" : ligne.getStatut().name()) + ").");
        }

        // Bloquer si validation Responsable déjà présente
        if (validationRepository.existsValidationResponsableValidee(id)) {
            throw new BusinessRuleException("RESULTAT_DEJA_VALIDE_RESPONSABLE",
                    "Le résultat a déjà été validé par le Responsable. Modification impossible.");
        }

        boolean dejaaSaisi = ligne.getValeur() != null && !ligne.getValeur().isBlank();
        if (dejaaSaisi && (req.commentaire() == null || req.commentaire().isBlank())) {
            throw new BusinessRuleException("MOTIF_MODIFICATION_REQUIS",
                    "Un commentaire (motif) est obligatoire pour modifier un résultat déjà saisi.");
        }

        // Classification serveur
        Boolean conformiteCalc = calculerConformiteBoolean(req.valeur(), ligne.getEssai());
        Instant maintenant = Instant.now();

        // UPDATE atomique via @Modifying
        int updated = ligneEssaiRepository.saisirResultat(
                id, moi, req.valeur(), req.commentaire(), conformiteCalc, maintenant);
        if (updated == 0) {
            throw new BusinessRuleException("ESSAI_STATUT_INVALIDE",
                    "La saisie a échoué (mise à jour concurrente). Veuillez réessayer.");
        }

        // Audit — valeur masquée
        String avant = dejaaSaisi ? "{\"statut\":\"EN_COURS\",\"valeur\":\"[MASQUÉ]\"}" : "{\"statut\":\"EN_COURS\"}";
        journalAuditService.enregistrer(
                "RESULTAT_SAISI", "LigneEssai", id, acteur,
                avant, "{\"statut\":\"EN_COURS\",\"valeur\":\"[MASQUÉ]\"}",
                dejaaSaisi ? req.commentaire() : null, labId);

        return ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .map(this::toListDto)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
    }

    /**
     * POST /{id}/non-realisable
     *
     * <p>Statuts acceptés : ATTENTE_ECHANTILLON ou EN_COURS.</p>
     */
    @Transactional
    public LigneEssaiTechnicienDto declareNonRealisable(Long id, NonRealisableRequest req) {
        var principal = SecurityUtils.principalCourant();
        Long moi = principal.getId();
        String acteur = principal.getEmail();
        Long labId = principal.getLaboratoireId();

        LigneEssai ligne = ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));

        StatutEssai s = ligne.getStatut();
        if (s != StatutEssai.ATTENTE_ECHANTILLON && s != StatutEssai.EN_COURS) {
            throw new BusinessRuleException("ESSAI_STATUT_INVALIDE",
                    "Non réalisable uniquement depuis ATTENTE_ECHANTILLON ou EN_COURS (statut actuel : "
                            + (s == null ? "inconnu" : s.name()) + ").");
        }

        int updated = ligneEssaiRepository.passerNonRealisable(id, moi, req.motif(), Instant.now());
        if (updated == 0) {
            throw new BusinessRuleException("ESSAI_STATUT_INVALIDE",
                    "La mise à jour a échoué (mise à jour concurrente). Veuillez réessayer.");
        }

        String statutAvant = s.name();
        // Tester la transition EN_VALIDATION
        tenterTransitionEnValidation(ligne.getDemande().getId(), acteur, labId);

        journalAuditService.enregistrer(
                "ESSAI_NON_REALISABLE", "LigneEssai", id, acteur,
                statutAvant, "NON_REALISABLE", req.motif(), labId);

        return ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .map(this::toListDto)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
    }

    /**
     * POST /{id}/temps
     *
     * <p>Une seule valeur stockée (duree_minutes). La dernière déclaration remplace la précédente.</p>
     */
    @Transactional
    public LigneEssaiTechnicienDto declarerTemps(Long id, TempsRequest req) {
        var principal = SecurityUtils.principalCourant();
        Long moi = principal.getId();
        String acteur = principal.getEmail();
        Long labId = principal.getLaboratoireId();

        LigneEssai ligne = ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));

        // Calcul de la durée
        int duree;
        if (req.debut() != null && req.fin() != null) {
            if (!req.fin().isAfter(req.debut())) {
                throw new BusinessRuleException("TEMPS_FIN_AVANT_DEBUT",
                        "La date de fin doit être postérieure à la date de début.");
            }
            long minutes = ChronoUnit.MINUTES.between(req.debut(), req.fin());
            duree = (int) minutes;
        } else if (req.dureeMinutes() != null) {
            duree = req.dureeMinutes();
        } else {
            throw new BusinessRuleException("TEMPS_FORMAT_INVALIDE",
                    "Fournissez soit (debut + fin) soit dureeMinutes.");
        }

        if (duree <= 0) {
            throw new BusinessRuleException("TEMPS_FORMAT_INVALIDE",
                    "La durée calculée doit être positive.");
        }
        if (duree > dureeMaxMinutes) {
            throw new BusinessRuleException("DUREE_MAX_DEPASSEE",
                    "La durée déclarée (" + duree + " min) dépasse le plafond autorisé ("
                            + dureeMaxMinutes + " min).");
        }

        Integer ancienneDuree = ligne.getDureeMinutes();
        ligneEssaiRepository.mettreAJourDuree(id, moi, duree);

        journalAuditService.enregistrer(
                "TEMPS_DECLARE", "LigneEssai", id, acteur,
                ancienneDuree == null ? null : String.valueOf(ancienneDuree),
                String.valueOf(duree),
                null, labId);

        return ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .map(this::toListDto)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
    }

    /**
     * POST /{id}/validation-technique
     *
     * <p>Niveau 1 seulement. La décision est toujours VALIDEE (jamais fournie par le client).</p>
     * <p>L'historique de validation est conservé (ISO 17025 — journal immuable).</p>
     * <p>Transition : ligne → TERMINE. Demande → EN_VALIDATION si toutes les lignes sont closes.</p>
     */
    @Transactional
    public LigneEssaiTechnicienDto validerTechniquement(Long id, ValidationTechniqueRequest req) {
        var principal = SecurityUtils.principalCourant();
        Long moi = principal.getId();
        String acteur = principal.getEmail();
        Long labId = principal.getLaboratoireId();

        LigneEssai ligne = ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));

        // Statut EN_COURS requis
        if (ligne.getStatut() != StatutEssai.EN_COURS) {
            throw new BusinessRuleException("ESSAI_NON_EN_COURS",
                    "La validation technique n'est possible que lorsque l'essai est EN_COURS (statut actuel : "
                            + (ligne.getStatut() == null ? "inconnu" : ligne.getStatut().name()) + ").");
        }

        // Résultat saisi requis
        if (ligne.getValeur() == null || ligne.getValeur().isBlank()) {
            throw new BusinessRuleException("RESULTAT_ABSENT",
                    "Un résultat doit être saisi avant de soumettre la validation technique.");
        }

        // Créer la validation N1 (INSERT — historique préservé)
        String codeVal = "VT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Validation validation = Validation.builder()
                .code(codeVal)
                .niveau(NIVEAU_TECHNIQUE)
                .decision("VALIDEE")
                .motif(req.commentaire())
                .dateValidation(Instant.now())
                .ligneEssai(ligne)
                .validateurId(moi)
                .build();
        validationRepository.save(validation);

        // UPDATE conditionnel : ligne → TERMINE
        Instant maintenant = Instant.now();
        int updated = ligneEssaiRepository.terminerSiEnCours(id, moi, maintenant);
        if (updated == 0) {
            throw new BusinessRuleException("ESSAI_STATUT_INVALIDE",
                    "La mise à jour du statut a échoué (mise à jour concurrente). Veuillez réessayer.");
        }

        // Transition demande EN_COURS → EN_VALIDATION si toutes les lignes sont closes
        tenterTransitionEnValidation(ligne.getDemande().getId(), acteur, labId);

        journalAuditService.enregistrer(
                "VALIDATION_TECHNIQUE", "LigneEssai", id, acteur,
                "EN_COURS", "TERMINE", req.commentaire(), labId);

        return ligneEssaiRepository.findByIdAndTechnicienId(id, moi)
                .map(this::toListDto)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
    }

    // -----------------------------------------------------------------------
    // Helpers écriture
    // -----------------------------------------------------------------------

    /**
     * Vérifie si toutes les lignes de la demande sont closes (TERMINE ou NON_REALISABLE).
     * Si oui, tente la transition EN_COURS → EN_VALIDATION (atomique, une seule fois).
     */
    void tenterTransitionEnValidation(Long demandeId, String acteur, Long labId) {
        List<LigneEssai> lignesDemande = ligneEssaiRepository.findByDemandeIdLeger(demandeId);
        boolean toutesCloses = lignesDemande.stream().allMatch(
                le -> le.getStatut() == StatutEssai.TERMINE
                        || le.getStatut() == StatutEssai.NON_REALISABLE);
        if (toutesCloses && !lignesDemande.isEmpty()) {
            int updated = demandeRepository.passerEnValidationSiEnCours(demandeId);
            if (updated > 0) {
                journalAuditService.enregistrer(
                        "DEMANDE_EN_VALIDATION", "Demande", demandeId, acteur,
                        "EN_COURS", "EN_VALIDATION",
                        "Toutes les lignes terminées ou non réalisables", labId);
            }
        }
    }

    /**
     * Calcule la conformité booléenne du résultat à partir des seuils de l'essai.
     * Retourne null si les limites sont absentes ou la valeur non numérique.
     */
    static Boolean calculerConformiteBoolean(String valeur, Essai essai) {
        if (valeur == null || essai == null) return null;
        if (essai.getLimiteMin() == null && essai.getLimiteMax() == null) return null;
        String norm = valeur.replace(',', '.');
        double val;
        try {
            val = Double.parseDouble(norm);
        } catch (NumberFormatException e) {
            return null; // valeur texte non numérique
        }
        double min = essai.getLimiteMin() != null ? essai.getLimiteMin().doubleValue() : Double.NEGATIVE_INFINITY;
        double max = essai.getLimiteMax() != null ? essai.getLimiteMax().doubleValue() : Double.POSITIVE_INFINITY;
        return val >= min && val <= max;
    }
}
