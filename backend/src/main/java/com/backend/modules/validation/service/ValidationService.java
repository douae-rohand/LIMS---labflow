package com.backend.modules.validation.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.ProprieteClient;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.entity.StatutDemande;
import com.backend.modules.demande.repository.DemandeRepository;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.planification.service.LigneTravailAssembleur;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.validation.dto.DecisionValidationRequest;
import com.backend.modules.validation.dto.ValidationDto;
import com.backend.modules.validation.entity.StatutValidation;
import com.backend.modules.validation.entity.Validation;
import com.backend.modules.validation.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Validation des résultats en deux niveaux :
 * <ol>
 *   <li>validation technique par le technicien affecté, une fois le résultat saisi ;</li>
 *   <li>décision du responsable : valider, retourner au technicien ou rejeter (essai à refaire).</li>
 * </ol>
 * Le validateur est toujours l'utilisateur connecté. Chaque décision est signée par
 * une empreinte SHA-256 (analyse, valeur, validateur, date).
 */
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ValidationRepository validationRepository;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final DemandeRepository demandeRepository;
    private final ProprieteClient proprieteClient;
    private final LigneTravailAssembleur assembleur;
    private final JournalAuditService journalAuditService;

    @Transactional(readOnly = true)
    public List<ValidationDto> listerParEssai(Long essaiId) {
        if (proprieteClient.clientConnecte()) {
            LigneEssai ligne = ligneEssaiRepository.findById(essaiId).orElseThrow(proprieteClient::introuvable);
            proprieteClient.exiger(ligne.getDemande());
        }
        List<Validation> historique = validationRepository.findByLigneEssai_IdOrderByDateValidationAscIdAsc(essaiId);
        Map<Long, String> noms = assembleur.nomsUtilisateurs(
                historique.stream().map(Validation::getValidateurId).toList());
        return historique.stream().map(v -> toDto(v, noms.get(v.getValidateurId()))).toList();
    }

    /** Résultats validés techniquement, en attente de la décision du responsable. */
    @Transactional(readOnly = true)
    public List<LigneTravailDto> resultatsAValider() {
        List<LigneEssai> lignes = ligneEssaiRepository.findLignesDesDemandes(statutsDemandesSuivies()).stream()
                .filter(l -> l.getStatut() == StatutEssai.TERMINE)
                .toList();
        // Les plus récemment soumis d'abord.
        return assembleur.assembler(lignes).stream()
                .filter(l -> LigneTravailAssembleur.TECHNIQUE_VALIDEE.equals(l.getEtatValidation()))
                .sorted(Comparator.comparing(LigneTravailDto::getDerniereDecisionDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Niveau 1 : le technicien affecté confirme son résultat. */
    @Transactional
    public ValidationDto validerTechniquement(Long ligneId, String commentaire) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        LigneEssai ligne = charger(ligneId);
        if (acteur.getRole() == RoleUtilisateur.TECHNICIEN && !acteur.getId().equals(ligne.getTechnicienId())) {
            throw new BusinessRuleException("ANALYSE_NON_AFFECTEE", "Cette analyse ne vous est pas affectée.");
        }
        if (ligne.getStatut() != StatutEssai.TERMINE || !StringUtils.hasText(ligne.getValeur())) {
            throw new BusinessRuleException("RESULTAT_ABSENT", "Saisissez le résultat avant de le valider.");
        }
        String etat = LigneTravailAssembleur.etatValidation(historique(ligneId));
        if (LigneTravailAssembleur.TECHNIQUE_VALIDEE.equals(etat) || LigneTravailAssembleur.VALIDEE.equals(etat)) {
            throw new BusinessRuleException("DEJA_VALIDEE", "Ce résultat est déjà validé.");
        }
        Validation validation = enregistrer(ligne, 1, StatutValidation.VALIDEE, texte(commentaire), acteur);
        Demande demande = ligne.getDemande();
        if (StatutDemande.depuis(demande.getStatut()) == StatutDemande.EN_COURS
                || StatutDemande.depuis(demande.getStatut()) == StatutDemande.ACCEPTEE) {
            demande.setStatut(StatutDemande.EN_VALIDATION.name());
            demandeRepository.save(demande);
        }
        return toDto(validation, acteur.getNomComplet());
    }

    /** Niveau 2 : décision du responsable sur un résultat validé techniquement. */
    @Transactional
    public LigneTravailDto decider(Long ligneId, DecisionValidationRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        LigneEssai ligne = charger(ligneId);
        String etat = LigneTravailAssembleur.etatValidation(historique(ligneId));
        if (!LigneTravailAssembleur.TECHNIQUE_VALIDEE.equals(etat)) {
            throw new BusinessRuleException("PAS_EN_ATTENTE",
                    "Ce résultat n'attend pas de décision (il doit d'abord être validé par le technicien).");
        }
        String motif = texte(request.motif());
        if (request.decision() != DecisionValidationRequest.Decision.VALIDER && motif == null) {
            throw new BusinessRuleException("MOTIF_OBLIGATOIRE",
                    "Indiquez au technicien la raison du retour ou du rejet.");
        }
        Demande demande = ligne.getDemande();
        String valeurAvant = ligne.getValeur();
        switch (request.decision()) {
            case VALIDER -> enregistrer(ligne, 2, StatutValidation.VALIDEE, motif, acteur);
            case RETOURNER -> {
                enregistrer(ligne, 2, StatutValidation.REVISION_DEMANDEE, motif, acteur);
                ligne.setStatut(StatutEssai.EN_COURS);
                ligne.setDateFin(null);
                ligneEssaiRepository.save(ligne);
            }
            case REJETER -> {
                enregistrer(ligne, 2, StatutValidation.REJETEE, motif, acteur);
                ligne.setStatut(StatutEssai.EN_COURS);
                ligne.setValeur(null);
                ligne.setConformite(null);
                ligne.setDateSaisie(null);
                ligne.setDateFin(null);
                ligneEssaiRepository.save(ligne);
            }
        }
        mettreAJourDemande(demande);
        journalAuditService.enregistrer("DECISION_RESULTAT_" + request.decision().name(), "LigneEssai", ligneId,
                acteur.getEmail(), valeurAvant == null ? null : "{\"valeur\":\"" + valeurAvant.replace("\"", "") + "\"}",
                "{\"decision\":\"" + request.decision().name() + "\"}", motif, acteur.getLaboratoireId());
        return assembleur.assembler(ligneEssaiRepository.findAvecDetails(List.of(ligneId))).getFirst();
    }

    /**
     * Toutes les analyses réalisables validées par le responsable : la demande est terminée.
     * Sinon, une demande « en validation » sans résultat en attente repasse « en cours ».
     */
    private void mettreAJourDemande(Demande demande) {
        List<LigneEssai> lignes = ligneEssaiRepository.findByDemande_Id(demande.getId()).stream()
                .filter(l -> l.getStatut() != StatutEssai.NON_REALISABLE)
                .toList();
        Map<Long, String> etats = assembleur.etats(lignes.stream().map(LigneEssai::getId).toList());
        boolean toutesValidees = !lignes.isEmpty() && etats.values().stream()
                .allMatch(LigneTravailAssembleur.VALIDEE::equals);
        StatutDemande actuel = StatutDemande.depuis(demande.getStatut());
        if (toutesValidees) {
            demande.setStatut(StatutDemande.TERMINEE.name());
            demandeRepository.save(demande);
            return;
        }
        boolean enAttente = etats.values().stream().anyMatch(LigneTravailAssembleur.TECHNIQUE_VALIDEE::equals);
        if (actuel == StatutDemande.EN_VALIDATION && !enAttente) {
            demande.setStatut(StatutDemande.EN_COURS.name());
            demandeRepository.save(demande);
        }
    }

    private Validation enregistrer(LigneEssai ligne, int niveau, StatutValidation decision, String motif,
                                   UtilisateurPrincipal acteur) {
        Instant maintenant = Instant.now();
        Validation validation = Validation.builder()
                .code("VAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .niveau(niveau)
                .decision(decision.name())
                .motif(motif)
                .dateValidation(maintenant)
                .signature(empreinte(ligne.getId() + "|" + ligne.getValeur() + "|" + acteur.getId()
                        + "|" + niveau + "|" + decision.name() + "|" + maintenant))
                .ligneEssai(ligne)
                .validateurId(acteur.getId())
                .build();
        return validationRepository.save(validation);
    }

    private List<Validation> historique(Long ligneId) {
        return validationRepository.findByLigneEssai_IdOrderByDateValidationAscIdAsc(ligneId);
    }

    private static List<String> statutsDemandesSuivies() {
        List<String> statuts = new ArrayList<>();
        statuts.addAll(StatutDemande.EN_COURS.aliasesPersistés());
        statuts.addAll(StatutDemande.EN_VALIDATION.aliasesPersistés());
        statuts.addAll(StatutDemande.ACCEPTEE.aliasesPersistés());
        return statuts;
    }

    private LigneEssai charger(Long id) {
        return ligneEssaiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LigneEssai", "id", id));
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    static String empreinte(String contenu) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(contenu.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 non disponible", ex);
        }
    }

    private ValidationDto toDto(Validation validation, String validateurNom) {
        return ValidationDto.builder()
                .id(validation.getId())
                .essaiId(validation.getLigneEssai() == null ? null : validation.getLigneEssai().getId())
                .validateurId(validation.getValidateurId())
                .validateurNom(validateurNom)
                .niveau(validation.getNiveau())
                .statut(enumOuNull(validation.getDecision()))
                .commentaire(validation.getMotif())
                .dateValidation(validation.getDateValidation())
                .build();
    }

    private static StatutValidation enumOuNull(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return StatutValidation.valueOf(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
