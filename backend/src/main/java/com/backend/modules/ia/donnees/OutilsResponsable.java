package com.backend.modules.ia.donnees;

import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.planification.dto.SuiviResponsableDto;
import com.backend.modules.planification.dto.TechnicienChargeDto;
import com.backend.modules.planification.service.SuiviResponsableService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Comparator;

/**
 * Outils du Responsable : lecture seule du tableau de suivi de son laboratoire.
 * Aucun nom de patient ni de client ; les techniciens apparaissent sous pseudonyme.
 */
public final class OutilsResponsable {

    private final SessionOutils session;
    private final SuiviResponsableService suiviService;
    private SuiviResponsableDto suivi;

    OutilsResponsable(SessionOutils session, SuiviResponsableService suiviService) {
        this.session = session;
        this.suiviService = suiviService;
    }

    @Tool(description = "Indicateurs actuels du laboratoire : demandes à accepter, analyses à affecter, "
            + "en cours, en retard, résultats à valider, résultats en alerte d'anomalie, rapports à préparer.")
    public String indicateursDuLaboratoire() {
        return session.lire("Tableau de suivi", "/app/suivi", () -> {
            SuiviResponsableDto.Indicateurs i = suivi().indicateurs();
            return "Demandes à accepter ou refuser : " + i.demandesADecider()
                    + "\nAnalyses sans technicien : " + i.analysesAAffecter()
                    + " (dont demandes prêtes à affecter : " + i.demandesAAffecter() + ")"
                    + "\nAnalyses à réaffecter (technicien désactivé) : " + i.analysesAReaffecter()
                    + "\nAnalyses en cours : " + i.analysesEnCours()
                    + "\nAnalyses en retard : " + i.analysesEnRetard()
                    + "\nRésultats à valider : " + i.resultatsAValider()
                    + "\nRésultats en alerte d'anomalie (score ≥ 40) : " + i.resultatsEnAlerte()
                    + "\nRapports à préparer ou signer : " + i.rapportsAPreparer();
        });
    }

    @Tool(description = "Analyses en retard du laboratoire, de la plus en retard à la moins en retard "
            + "(référence de la demande, analyse, technicien, jours de retard).")
    public String analysesEnRetard() {
        return session.lire("Analyses en retard", "/app/suivi", () -> {
            SuiviResponsableDto s = suivi();
            if (s.retards().isEmpty()) return "Aucune analyse en retard.";
            StringBuilder texte = new StringBuilder("Total en retard : " + s.indicateurs().analysesEnRetard() + "\n");
            s.retards().stream().limit(SessionOutils.MAX_LIGNES).forEach(a -> texte
                    .append("- ").append(a.getDemandeReference()).append(" : ").append(analyse(a))
                    .append(", technicien ").append(session.personne(a.getTechnicienId(), a.getTechnicienNom()))
                    .append(", ").append(a.getJoursRetard()).append(" jour(s) de retard\n"));
            return texte.toString();
        });
    }

    @Tool(description = "Charge de travail de chaque technicien du laboratoire : analyses à faire, en cours et en retard.")
    public String chargeDesTechniciens() {
        return session.lire("Charge des techniciens", "/app/suivi", () -> {
            var techniciens = suivi().techniciens();
            if (techniciens.isEmpty()) return "Aucun technicien actif.";
            StringBuilder texte = new StringBuilder();
            techniciens.stream()
                    .sorted(Comparator.comparingLong(TechnicienChargeDto::total).reversed())
                    .limit(SessionOutils.MAX_LIGNES)
                    .forEach(t -> texte.append("- ").append(session.personne(t.id(), t.nomComplet()))
                            .append(" (").append(String.join(", ", t.domaines())).append(") : ")
                            .append(t.aFaire()).append(" à faire, ").append(t.enCours()).append(" en cours, ")
                            .append(t.enRetard()).append(" en retard\n"));
            return texte.toString();
        });
    }

    @Tool(description = "Résultats à valider en alerte d'anomalie (score de 0 à 100, critique, hors intervalle, "
            + "inhabituel), du score le plus élevé au plus bas.")
    public String resultatsEnAlerte() {
        return session.lire("Résultats en alerte", "/app/validation-resultats", () -> {
            var alertes = suivi().alertesAnomalies();
            if (alertes.isEmpty()) return "Aucun résultat à valider n'est en alerte.";
            StringBuilder texte = new StringBuilder();
            alertes.stream().limit(SessionOutils.MAX_LIGNES).forEach(a -> texte
                    .append("- ").append(a.getDemandeReference()).append(" : ").append(analyse(a))
                    .append(" = ").append(a.getValeur()).append(a.getUnite() == null ? "" : " " + a.getUnite())
                    .append(", score ").append(a.getScoreAnomalie()).append(" (").append(a.getTypeAnomalie()).append(")")
                    .append(a.getMotifAnomalie() == null ? "" : " : " + a.getMotifAnomalie()).append('\n'));
            return texte.toString();
        });
    }

    private SuiviResponsableDto suivi() {
        if (suivi == null) suivi = suiviService.suivi();
        return suivi;
    }

    static String analyse(LigneTravailDto a) {
        return a.getEssaiCode() + (a.getEssaiLibelle() == null ? "" : " (" + a.getEssaiLibelle() + ")");
    }
}
