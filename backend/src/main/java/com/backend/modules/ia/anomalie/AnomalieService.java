package com.backend.modules.ia.anomalie;

import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.planification.dto.LigneTravailDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Score d'anomalie des résultats (CDC : § 2.2.1 étape 5 « appel au module d'anomalies »,
 * M04 « indicateurs OOS / OOT », M08 « alertes anomalies » du Responsable).
 *
 * <p>Score de 0 à 100, calculé sans IA générative : règles et statistique robuste, donc gratuit,
 * immédiat et explicable. Aucune donnée ne sort du serveur.
 * <ul>
 *   <li><b>CRITIQUE</b> : valeur hors des seuils critiques de l'essai → 100.</li>
 *   <li><b>HORS_INTERVALLE</b> (OOS, Out Of Specification) : hors de l'intervalle de référence
 *       → 50 à 90 selon l'écart rapporté à la largeur de l'intervalle.</li>
 *   <li><b>HORS_TENDANCE</b> (OOT, Out Of Trend) : valeur inhabituelle par rapport aux résultats
 *       précédents du même essai au laboratoire (score z robuste : médiane et écart absolu médian,
 *       |z| ≥ 3,5 avec au moins {@value #HISTORIQUE_MIN} résultats) → 40 à 80. Repère une valeur
 *       « normale » mais surprenante : erreur de saisie, d'unité, d'échantillon ou d'appareil.</li>
 *   <li><b>NON_CONFORME</b> : résultat non numérique déclaré non conforme → 50.</li>
 * </ul>
 * Une alerte est levée à partir de {@value #SEUIL_ALERTE}. Le score aide le Responsable à prioriser :
 * il ne valide ni ne rejette rien.
 */
@Slf4j
@Service
public class AnomalieService {

    public static final int SEUIL_ALERTE = 40;
    static final int HISTORIQUE_MIN = 10;
    private static final int HISTORIQUE_MAX = 200;
    private static final double Z_SEUIL = 3.5;

    public enum TypeAnomalie { CRITIQUE, HORS_INTERVALLE, HORS_TENDANCE, NON_CONFORME }

    /** Score arrondi à l'entier, type principal et motif lisible ; {@code type} nul si aucune anomalie. */
    public record Anomalie(BigDecimal score, TypeAnomalie type, String motif) {
        public boolean alerte() {
            return score != null && score.intValue() >= SEUIL_ALERTE;
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TenantExecutor tenantExecutor;

    /**
     * Évalue les lignes ayant un résultat et enregistre le score dans {@code ligne_essai.score_anomalie}
     * quand il a changé. Les scores sont recalculés à chaque lecture : un résultat modifié ou retourné
     * est donc toujours évalué sur sa valeur actuelle.
     *
     * @return anomalie par identifiant de ligne (lignes sans résultat absentes)
     */
    public Map<Long, Anomalie> evaluer(Collection<LigneEssai> lignes) {
        List<LigneEssai> avecResultat = lignes.stream()
                .filter(l -> l.getEssai() != null && StringUtils.hasText(l.getValeur()))
                .toList();
        if (avecResultat.isEmpty()) return Map.of();

        Map<Long, List<Historique>> historiques = historiques(
                avecResultat.stream().map(l -> l.getEssai().getId()).collect(Collectors.toSet()));
        Map<Long, Anomalie> resultats = new HashMap<>();
        Map<Long, BigDecimal> aEnregistrer = new HashMap<>();
        for (LigneEssai ligne : avecResultat) {
            List<Double> precedents = historiques.getOrDefault(ligne.getEssai().getId(), List.of()).stream()
                    .filter(h -> !h.ligneId().equals(ligne.getId()))
                    .map(Historique::valeur)
                    .limit(HISTORIQUE_MAX)
                    .toList();
            Anomalie anomalie = evaluer(ligne, precedents);
            resultats.put(ligne.getId(), anomalie);
            if (!Objects.equals(normaliser(ligne.getScoreAnomalie()), anomalie.score())) {
                aEnregistrer.put(ligne.getId(), anomalie.score());
            }
        }
        enregistrer(aEnregistrer);
        return resultats;
    }

    /**
     * Évalue les lignes et reporte score, type et motif sur les analyses affichées au Responsable.
     *
     * @return les analyses en alerte (score ≥ {@value #SEUIL_ALERTE}), du score le plus haut au plus bas
     */
    public List<LigneTravailDto> completer(List<LigneTravailDto> analyses, Collection<LigneEssai> lignes) {
        Map<Long, Anomalie> anomalies = evaluer(lignes);
        List<LigneTravailDto> alertes = new ArrayList<>();
        for (LigneTravailDto analyse : analyses) {
            Anomalie anomalie = anomalies.get(analyse.getId());
            if (anomalie == null) continue;
            analyse.setScoreAnomalie(anomalie.score().intValue());
            analyse.setTypeAnomalie(anomalie.type() == null ? null : anomalie.type().name());
            analyse.setMotifAnomalie(anomalie.motif());
            if (anomalie.alerte()) alertes.add(analyse);
        }
        alertes.sort((a, b) -> Integer.compare(b.getScoreAnomalie(), a.getScoreAnomalie()));
        return alertes;
    }

    // -------------------------------------------------------------------------
    // Calcul (méthodes pures, testables sans base)
    // -------------------------------------------------------------------------

    static Anomalie evaluer(LigneEssai ligne, List<Double> precedents) {
        Essai essai = ligne.getEssai();
        String unite = StringUtils.hasText(essai.getUnite()) ? " " + essai.getUnite().trim() : "";
        Double valeur = nombre(ligne.getValeur());

        if (valeur == null) {
            return Boolean.FALSE.equals(ligne.getConformite())
                    ? new Anomalie(BigDecimal.valueOf(50), TypeAnomalie.NON_CONFORME, "Résultat déclaré non conforme.")
                    : new Anomalie(BigDecimal.ZERO, null, null);
        }

        // 1. Seuils critiques
        if (essai.getSeuilCritiqueMin() != null && valeur < essai.getSeuilCritiqueMin().doubleValue()) {
            return new Anomalie(BigDecimal.valueOf(100), TypeAnomalie.CRITIQUE,
                    "Valeur critique : " + texte(valeur) + unite + " sous le seuil critique de "
                            + texte(essai.getSeuilCritiqueMin().doubleValue()) + unite + ".");
        }
        if (essai.getSeuilCritiqueMax() != null && valeur > essai.getSeuilCritiqueMax().doubleValue()) {
            return new Anomalie(BigDecimal.valueOf(100), TypeAnomalie.CRITIQUE,
                    "Valeur critique : " + texte(valeur) + unite + " au-dessus du seuil critique de "
                            + texte(essai.getSeuilCritiqueMax().doubleValue()) + unite + ".");
        }

        // 2. Intervalle de référence (OOS)
        double scoreOos = 0;
        String motifOos = null;
        Double min = essai.getLimiteMin() == null ? null : essai.getLimiteMin().doubleValue();
        Double max = essai.getLimiteMax() == null ? null : essai.getLimiteMax().doubleValue();
        double ecart = 0;
        if (min != null && valeur < min) ecart = min - valeur;
        else if (max != null && valeur > max) ecart = valeur - max;
        if (ecart > 0) {
            double largeur = (min != null && max != null && max > min) ? max - min
                    : Math.max(Math.abs(min != null ? min : max), 1e-9);
            scoreOos = 50 + 40 * Math.min(1, ecart / largeur);
            motifOos = (min != null && valeur < min ? "En dessous" : "Au-dessus") + " de l'intervalle de référence : "
                    + texte(valeur) + unite + " pour " + intervalle(min, max) + unite + ".";
        }

        // 3. Tendance du laboratoire (OOT)
        double scoreOot = 0;
        String motifOot = null;
        if (precedents.size() >= HISTORIQUE_MIN) {
            double mediane = mediane(precedents);
            double mad = mediane(precedents.stream().map(v -> Math.abs(v - mediane)).toList());
            if (mad > 0) {
                double z = 0.6745 * (valeur - mediane) / mad;
                if (Math.abs(z) >= Z_SEUIL) {
                    scoreOot = Math.min(80, 40 + 8 * (Math.abs(z) - Z_SEUIL));
                    motifOot = "Valeur inhabituelle pour cet essai : nettement " + (z > 0 ? "plus haute" : "plus basse")
                            + " que les " + precedents.size() + " résultats précédents (médiane "
                            + texte(mediane) + unite + ").";
                }
            }
        }

        if (scoreOos == 0 && scoreOot == 0) return new Anomalie(BigDecimal.ZERO, null, null);
        // Hors intervalle prime sur hors tendance : c'est l'écart réglementaire (OOS)
        TypeAnomalie type = motifOos != null ? TypeAnomalie.HORS_INTERVALLE : TypeAnomalie.HORS_TENDANCE;
        String motif = motifOos != null && motifOot != null ? motifOos + " " + motifOot
                : motifOos != null ? motifOos : motifOot;
        return new Anomalie(arrondi(Math.max(scoreOos, scoreOot)), type, motif);
    }

    static double mediane(List<Double> valeurs) {
        List<Double> triees = new ArrayList<>(valeurs);
        triees.sort(Double::compare);
        int n = triees.size();
        return n % 2 == 1 ? triees.get(n / 2) : (triees.get(n / 2 - 1) + triees.get(n / 2)) / 2;
    }

    static Double nombre(String valeur) {
        if (valeur == null) return null;
        try {
            double v = Double.parseDouble(valeur.trim().replace(',', '.').replace(" ", ""));
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal arrondi(double score) {
        return BigDecimal.valueOf(score).setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal normaliser(BigDecimal score) {
        return score == null ? null : score.setScale(0, RoundingMode.HALF_UP);
    }

    private static String texte(double nombre) {
        return BigDecimal.valueOf(nombre).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                .replace('.', ',');
    }

    private static String intervalle(Double min, Double max) {
        if (min != null && max != null) return texte(min) + " à " + texte(max);
        if (min != null) return "au moins " + texte(min);
        return "au plus " + texte(max);
    }

    // -------------------------------------------------------------------------
    // Accès aux données (schéma du laboratoire courant)
    // -------------------------------------------------------------------------

    private record Historique(Long ligneId, double valeur) {}

    /** Derniers résultats numériques terminés de chaque essai, du plus récent au plus ancien. */
    private Map<Long, List<Historique>> historiques(Collection<Long> essaiIds) {
        List<Object[]> lignes = entityManager.createQuery("""
                        SELECT le.essai.id, le.id, le.valeur FROM LigneEssai le
                        WHERE le.essai.id IN :essais AND le.valeur IS NOT NULL AND le.statut = :termine
                        ORDER BY le.dateSaisie DESC, le.id DESC""", Object[].class)
                .setParameter("essais", essaiIds)
                .setParameter("termine", StatutEssai.TERMINE)
                .getResultList();
        Map<Long, List<Historique>> parEssai = new HashMap<>();
        for (Object[] l : lignes) {
            Double v = nombre((String) l[2]);
            if (v == null) continue;
            List<Historique> liste = parEssai.computeIfAbsent((Long) l[0], k -> new ArrayList<>());
            if (liste.size() <= HISTORIQUE_MAX) liste.add(new Historique((Long) l[1], v));
        }
        return parEssai;
    }

    /** Écriture dans sa propre transaction : l'appelant peut être en lecture seule. */
    private void enregistrer(Map<Long, BigDecimal> scores) {
        if (scores.isEmpty()) return;
        String schema = TenantContext.getCurrentTenant();
        if (schema == null) return;
        try {
            tenantExecutor.inTenant(schema, () -> {
                scores.forEach((id, score) -> entityManager
                        .createQuery("UPDATE LigneEssai le SET le.scoreAnomalie = :score WHERE le.id = :id")
                        .setParameter("score", score)
                        .setParameter("id", id)
                        .executeUpdate());
                return null;
            });
        } catch (RuntimeException ex) {
            // Le score reste affiché ; il sera enregistré à la prochaine lecture
            log.warn("Scores d'anomalie non enregistrés ({}) : {}", schema, ex.getMessage());
        }
    }
}
