package com.backend.modules.ia.donnees;

import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.planification.service.LigneTravailAssembleur;
import com.backend.modules.planification.service.PlanificationService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Outils du Technicien : ses propres analyses uniquement (filtre sur son identifiant, tiré du jeton).
 * Lecture faite dans le module IA : le code de l'espace Technicien n'est pas modifié.
 */
public final class OutilsTechnicien {

    private final SessionOutils session;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final LigneTravailAssembleur assembleur;
    private List<LigneTravailDto> mesAnalyses;

    OutilsTechnicien(SessionOutils session, LigneEssaiRepository ligneEssaiRepository, LigneTravailAssembleur assembleur) {
        this.session = session;
        this.ligneEssaiRepository = ligneEssaiRepository;
        this.assembleur = assembleur;
    }

    @Tool(description = "Résumé des analyses affectées à l'utilisateur (technicien) : en attente d'échantillon, "
            + "en cours, en retard, terminées en attente de validation, retournées pour correction.")
    public String mesAnalyses() {
        return session.lire("Mes essais", "/app/essais", () -> {
            List<LigneTravailDto> analyses = analyses();
            if (analyses.isEmpty()) return "Aucune analyse ne vous est affectée sur les demandes en cours.";
            return "Analyses affectées : " + analyses.size()
                    + "\nEn attente d'échantillon : " + compter(analyses, StatutEssai.ATTENTE_ECHANTILLON)
                    + "\nEn cours : " + compter(analyses, StatutEssai.EN_COURS)
                    + "\nEn retard : " + analyses.stream().filter(LigneTravailDto::isEnRetard).count()
                    + "\nTerminées, en attente de validation : " + analyses.stream()
                            .filter(a -> a.getStatut() == StatutEssai.TERMINE && !"VALIDEE".equals(a.getEtatValidation()))
                            .count()
                    + "\nRetournées pour correction : " + retournees(analyses).size()
                    + "\nNon réalisables : " + compter(analyses, StatutEssai.NON_REALISABLE);
        });
    }

    @Tool(description = "Analyses en retard affectées à l'utilisateur (technicien), avec la référence de la demande "
            + "et le nombre de jours de retard.")
    public String mesAnalysesEnRetard() {
        return session.lire("Mes essais en retard", "/app/essais", () -> {
            List<LigneTravailDto> retards = analyses().stream()
                    .filter(LigneTravailDto::isEnRetard)
                    .sorted(Comparator.comparingLong(LigneTravailDto::getJoursRetard).reversed())
                    .toList();
            if (retards.isEmpty()) return "Aucune de vos analyses n'est en retard.";
            StringBuilder texte = new StringBuilder("Total : " + retards.size() + "\n");
            retards.stream().limit(SessionOutils.MAX_LIGNES).forEach(a -> texte
                    .append("- ").append(a.getDemandeReference()).append(" : ").append(OutilsResponsable.analyse(a))
                    .append(", ").append(a.getJoursRetard()).append(" jour(s) de retard\n"));
            return texte.toString();
        });
    }

    @Tool(description = "Résultats de l'utilisateur (technicien) retournés par le responsable pour correction.")
    public String mesResultatsRetournes() {
        return session.lire("Résultats retournés", "/app/essais", () -> {
            List<LigneTravailDto> retournees = retournees(analyses());
            if (retournees.isEmpty()) return "Aucun résultat ne vous a été retourné.";
            StringBuilder texte = new StringBuilder("Total : " + retournees.size() + "\n");
            retournees.stream().limit(SessionOutils.MAX_LIGNES).forEach(a -> texte
                    .append("- ").append(a.getDemandeReference()).append(" : ").append(OutilsResponsable.analyse(a))
                    .append(" (valeur saisie ").append(a.getValeur()).append(")\n"));
            return texte.append("Le motif du retour est visible dans « Essais ».").toString();
        });
    }

    private List<LigneTravailDto> analyses() {
        if (mesAnalyses == null) {
            mesAnalyses = assembleur.assembler(
                            ligneEssaiRepository.findLignesDesDemandes(PlanificationService.STATUTS_DEMANDES_ACTIVES))
                    .stream()
                    .filter(a -> Objects.equals(a.getTechnicienId(), session.utilisateurId()))
                    .toList();
        }
        return mesAnalyses;
    }

    private static long compter(List<LigneTravailDto> analyses, StatutEssai statut) {
        return analyses.stream().filter(a -> a.getStatut() == statut).count();
    }

    private static List<LigneTravailDto> retournees(List<LigneTravailDto> analyses) {
        return analyses.stream().filter(a -> "RETOURNEE".equals(a.getEtatValidation())).toList();
    }
}
