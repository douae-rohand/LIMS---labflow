package com.backend.modules.planification.service;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.validation.entity.StatutValidation;
import com.backend.modules.validation.entity.Validation;
import com.backend.modules.validation.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Construit la vue « analyse » commune à la planification, au suivi et à la validation :
 * technicien, échéance, retard et état du circuit de validation à deux niveaux.
 */
@Component
@RequiredArgsConstructor
public class LigneTravailAssembleur {

    public static final String NON_SOUMIS = "NON_SOUMIS";
    public static final String TECHNIQUE_VALIDEE = "TECHNIQUE_VALIDEE";
    public static final String VALIDEE = "VALIDEE";
    public static final String RETOURNEE = "RETOURNEE";
    public static final String REJETEE = "REJETEE";

    private final ValidationRepository validationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantExecutor tenantExecutor;

    public List<LigneTravailDto> assembler(List<LigneEssai> lignes) {
        if (lignes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = lignes.stream().map(LigneEssai::getId).toList();
        Map<Long, List<Validation>> historiques = validationRepository
                .findByLigneEssai_IdInOrderByDateValidationAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(v -> v.getLigneEssai().getId()));
        Set<Long> utilisateurs = new HashSet<>();
        lignes.forEach(l -> {
            if (l.getTechnicienId() != null) utilisateurs.add(l.getTechnicienId());
        });
        historiques.values().forEach(h -> h.forEach(v -> utilisateurs.add(v.getValidateurId())));
        Map<Long, String> noms = nomsUtilisateurs(utilisateurs);
        Instant maintenant = Instant.now();
        return lignes.stream()
                .map(l -> versDto(l, historiques.getOrDefault(l.getId(), List.of()), noms, maintenant))
                .toList();
    }

    /** État de validation de plusieurs analyses, en une seule requête. */
    public Map<Long, String> etats(Collection<Long> ligneIds) {
        if (ligneIds == null || ligneIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Validation>> historiques = validationRepository
                .findByLigneEssai_IdInOrderByDateValidationAscIdAsc(ligneIds).stream()
                .collect(Collectors.groupingBy(v -> v.getLigneEssai().getId()));
        Map<Long, String> etats = new HashMap<>();
        for (Long id : ligneIds) {
            etats.put(id, etatValidation(historiques.getOrDefault(id, List.of())));
        }
        return etats;
    }

    /** Historiques de validation de plusieurs analyses, en une seule requête. */
    public Map<Long, List<Validation>> historiques(Collection<Long> ligneIds) {
        if (ligneIds == null || ligneIds.isEmpty()) {
            return Map.of();
        }
        return validationRepository.findByLigneEssai_IdInOrderByDateValidationAscIdAsc(ligneIds).stream()
                .collect(Collectors.groupingBy(v -> v.getLigneEssai().getId()));
    }

    public Map<Long, String> nomsUtilisateurs(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return tenantExecutor.inCentral(() -> {
            Map<Long, String> noms = new HashMap<>();
            for (Utilisateur u : utilisateurRepository.findAllById(ids)) {
                noms.put(u.getId(), nomComplet(u));
            }
            return noms;
        });
    }

    /** État courant du circuit de validation, déduit de la dernière décision enregistrée. */
    public static String etatValidation(List<Validation> historique) {
        if (historique == null || historique.isEmpty()) {
            return NON_SOUMIS;
        }
        Validation derniere = historique.get(historique.size() - 1);
        String decision = derniere.getDecision();
        if (StatutValidation.REVISION_DEMANDEE.name().equals(decision)) {
            return RETOURNEE;
        }
        if (StatutValidation.REJETEE.name().equals(decision)) {
            return REJETEE;
        }
        if (StatutValidation.VALIDEE.name().equals(decision)) {
            return derniere.getNiveau() != null && derniere.getNiveau() >= 2 ? VALIDEE : TECHNIQUE_VALIDEE;
        }
        return NON_SOUMIS;
    }

    /** Échéance = date d'affectation + durée estimée de l'essai, en minutes (catalogue client, espace technicien). */
    public static Instant echeance(LigneEssai ligne) {
        Essai essai = ligne.getEssai();
        if (ligne.getDateAttribution() == null || essai == null || essai.getDureeEstimee() == null) {
            return null;
        }
        return ligne.getDateAttribution().plus(Duration.ofMinutes(essai.getDureeEstimee()));
    }

    public static boolean terminee(LigneEssai ligne) {
        return ligne.getStatut() == StatutEssai.TERMINE || ligne.getStatut() == StatutEssai.NON_REALISABLE;
    }

    public static boolean enRetard(LigneEssai ligne, Instant maintenant) {
        Instant echeance = echeance(ligne);
        return echeance != null && !terminee(ligne) && maintenant.isAfter(echeance);
    }

    private LigneTravailDto versDto(LigneEssai ligne, List<Validation> historique, Map<Long, String> noms,
                                    Instant maintenant) {
        Demande demande = ligne.getDemande();
        Essai essai = ligne.getEssai();
        Instant echeance = echeance(ligne);
        boolean retard = enRetard(ligne, maintenant);
        Validation derniere = historique.isEmpty() ? null : historique.get(historique.size() - 1);
        return LigneTravailDto.builder()
                .id(ligne.getId())
                .code(ligne.getCode())
                .demandeId(demande == null ? null : demande.getId())
                .demandeReference(demande == null ? null : demande.getNumero())
                .demandeTitre(demande == null ? null : demande.getTitre())
                .demandeStatut(demande == null ? null : demande.getStatut())
                .demandeDateSoumission(demande == null ? null : demande.getDateSoumission())
                .essaiCode(essai == null ? null : essai.getCode())
                .essaiLibelle(essai == null ? null : essai.getDesignation())
                .methode(essai == null ? null : essai.getMethode())
                .unite(essai == null ? null : essai.getUnite())
                .limiteMin(essai == null ? null : essai.getLimiteMin())
                .limiteMax(essai == null ? null : essai.getLimiteMax())
                .dureeEstimeeMinutes(essai == null ? null : essai.getDureeEstimee())
                .domaineCode(essai == null || essai.getDomaine() == null ? null : essai.getDomaine().getCode())
                .domaineLibelle(essai == null || essai.getDomaine() == null ? null : essai.getDomaine().getLibelle())
                .technicienId(ligne.getTechnicienId())
                .technicienNom(ligne.getTechnicienId() == null ? null : noms.get(ligne.getTechnicienId()))
                .statut(ligne.getStatut())
                .dateAttribution(ligne.getDateAttribution())
                .datePrevue(ligne.getDateDebut() == null ? null : LocalDate.ofInstant(ligne.getDateDebut(), java.time.ZoneId.of("Africa/Casablanca")))
                .echeance(echeance)
                .enRetard(retard)
                .joursRetard(retard ? Duration.between(echeance, maintenant).toDays() : 0)
                .valeur(ligne.getValeur())
                .conformite(ligne.getConformite())
                .critique(critique(ligne.getValeur(), essai))
                .etatEchantillon(ligne.getEchantillon() == null ? "ATTENDU"
                        : Boolean.TRUE.equals(ligne.getEchantillon().getConformite()) ? "CONFORME" : "NON_CONFORME")
                .dateSaisie(ligne.getDateSaisie())
                .observations(ligne.getMotif())
                .etatValidation(etatValidation(historique))
                .derniereDecisionMotif(derniere == null ? null : derniere.getMotif())
                .derniereDecisionDate(derniere == null ? null : derniere.getDateValidation())
                .derniereDecisionPar(derniere == null ? null : noms.get(derniere.getValidateurId()))
                .build();
    }

    private static String nomComplet(Utilisateur u) {
        String prenom = u.getPrenom() == null ? "" : u.getPrenom().trim();
        return (prenom + " " + (u.getNom() == null ? "" : u.getNom().trim())).trim();
    }

    /** Vrai si la valeur est numérique et sort des seuils critiques définis pour l'essai. */
    static boolean critique(String valeur, com.backend.modules.essai.entity.Essai essai) {
        if (valeur == null || essai == null
                || (essai.getSeuilCritiqueMin() == null && essai.getSeuilCritiqueMax() == null)) {
            return false;
        }
        try {
            java.math.BigDecimal nombre = new java.math.BigDecimal(valeur.trim().replace(',', '.'));
            return (essai.getSeuilCritiqueMin() != null && nombre.compareTo(essai.getSeuilCritiqueMin()) < 0)
                    || (essai.getSeuilCritiqueMax() != null && nombre.compareTo(essai.getSeuilCritiqueMax()) > 0);
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
