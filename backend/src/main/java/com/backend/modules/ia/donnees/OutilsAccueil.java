package com.backend.modules.ia.donnees;

import com.backend.modules.accueil.dto.CaisseDtos.JournalCaisse;
import com.backend.modules.accueil.dto.ReceptionDtos.TableauAccueilDto;
import com.backend.modules.accueil.service.CaisseService;
import com.backend.modules.accueil.service.TableauAccueilService;
import org.springframework.ai.tool.annotation.Tool;

import java.time.LocalDate;

/** Outils de l'Accueil : totaux de réception et de caisse, jamais le nom d'un patient ou d'un client. */
public final class OutilsAccueil {

    private final SessionOutils session;
    private final TableauAccueilService tableauService;
    private final CaisseService caisseService;

    OutilsAccueil(SessionOutils session, TableauAccueilService tableauService, CaisseService caisseService) {
        this.session = session;
        this.tableauService = tableauService;
        this.caisseService = caisseService;
    }

    @Tool(description = "Réception du jour : demandes dont les échantillons sont à recevoir, réceptions partielles, "
            + "échantillons reçus et non conformes aujourd'hui, factures à encaisser et reste à encaisser.")
    public String receptionEtFacturesDuJour() {
        return session.lire("Réception", "/app/reception", () -> {
            TableauAccueilDto t = tableauService.tableau();
            return "Demandes dont les échantillons sont à recevoir (nombre de demandes, pas d'échantillons) : " + t.demandesARecevoir()
                    + "\nRéceptions partielles : " + t.receptionsPartielles()
                    + "\nÉchantillons reçus aujourd'hui : " + t.echantillonsRecusAujourdhui()
                    + "\nÉchantillons non conformes aujourd'hui : " + t.nonConformesAujourdhui()
                    + "\nFactures à encaisser : " + t.facturesAEncaisser()
                    + "\nReste à encaisser : " + SessionOutils.montant(t.resteAEncaisser());
        });
    }

    @Tool(description = "Journal de caisse du jour : total encaissé, total par mode de paiement, nombre d'opérations et d'annulations.")
    public String caisseDuJour() {
        return session.lire("Journal de caisse", "/app/caisse", () -> {
            JournalCaisse j = caisseService.journal(LocalDate.now());
            StringBuilder texte = new StringBuilder("Journée du " + j.jour()
                    + "\nTotal encaissé : " + SessionOutils.montant(j.total())
                    + "\nOpérations : " + j.lignes().size()
                    + "\nAnnulations : " + j.annulations());
            j.totaux().forEach(m -> texte.append("\n- ").append(m.mode()).append(" : ")
                    .append(SessionOutils.montant(m.montant())).append(" (").append(m.operations()).append(" opération(s))"));
            return texte.toString();
        });
    }
}
