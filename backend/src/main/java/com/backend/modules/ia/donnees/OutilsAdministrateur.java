package com.backend.modules.ia.donnees;

import com.backend.modules.facturation.dto.FactureStatsDto;
import com.backend.modules.facturation.service.FacturationAdminService;
import com.backend.modules.produit.dto.LotDto;
import com.backend.modules.produit.dto.ProduitDto;
import com.backend.modules.produit.service.StockAdminService;
import com.backend.modules.satisfaction.dto.SatisfactionStatsDto;
import com.backend.modules.satisfaction.service.SatisfactionAdminService;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;
import java.util.Map;

/**
 * Outils de l'Administrateur du laboratoire : stock, facturation et satisfaction, en totaux.
 * Les services de l'espace Administrateur sont seulement appelés en lecture.
 */
public final class OutilsAdministrateur {

    private final SessionOutils session;
    private final StockAdminService stockService;
    private final FacturationAdminService facturationService;
    private final SatisfactionAdminService satisfactionService;

    OutilsAdministrateur(SessionOutils session, StockAdminService stockService,
                         FacturationAdminService facturationService, SatisfactionAdminService satisfactionService) {
        this.session = session;
        this.stockService = stockService;
        this.facturationService = facturationService;
        this.satisfactionService = satisfactionService;
    }

    @Tool(description = "Alertes de stock : produits en rupture ou sous le seuil minimal, lots périmés ou proches de la péremption.")
    public String alertesStock() {
        return session.lire("Stock", "/app/stock", () -> {
            Map<String, List<?>> alertes = stockService.getAlertes();
            return produits("En rupture", alertes.get("rupture"))
                    + produits("Sous le seuil minimal", alertes.get("sousSeuil"))
                    + lots("Lots périmés", alertes.get("perimes"))
                    + lots("Lots proches de la péremption", alertes.get("peremptionProche"));
        });
    }

    @Tool(description = "Facturation des 12 derniers mois : montant facturé, encaissé, impayé, nombre de factures impayées et en retard.")
    public String facturation() {
        return session.lire("Facturation", "/app/facturation", () -> {
            FactureStatsDto s = facturationService.getStats(null, null);
            return "Sur les 12 derniers mois :"
                    + "\nFacturé : " + SessionOutils.montant(s.getFacture())
                    + "\nEncaissé : " + SessionOutils.montant(s.getEncaisse())
                    + "\nImpayé : " + SessionOutils.montant(s.getImpaye())
                    + "\nFactures impayées : " + s.getNombreFacturesImpayees()
                    + "\nFactures en retard : " + s.getNombreEnRetard();
        });
    }

    @Tool(description = "Satisfaction des clients sur 12 mois : enquêtes envoyées, taux de réponse, notes moyennes sur 5.")
    public String satisfaction() {
        return session.lire("Satisfaction", "/app/satisfaction", () -> {
            SatisfactionStatsDto s = satisfactionService.getStats(null, null);
            return "Sur les 12 derniers mois :"
                    + "\nEnquêtes envoyées : " + s.getEnvoyees() + ", répondues : " + s.getRepondues()
                    + "\nTaux de réponse : " + arrondi(s.getTauxReponse()) + " %"
                    + "\nNote moyenne globale : " + arrondi(s.getMoyenneGlobale())
                    + "\nDélai : " + arrondi(s.getMoyenneDelai())
                    + ", clarté : " + arrondi(s.getMoyenneClarte())
                    + ", relation : " + arrondi(s.getMoyenneRelation());
        });
    }

    private static String produits(String titre, List<?> liste) {
        if (liste == null || liste.isEmpty()) return titre + " : aucun\n";
        StringBuilder texte = new StringBuilder(titre + " (" + liste.size() + ") :\n");
        liste.stream().limit(SessionOutils.MAX_LIGNES).map(ProduitDto.class::cast).forEach(p -> texte
                .append("- ").append(p.getNom()).append(" (").append(p.getReference()).append(") : stock ")
                .append(SessionOutils.nombre(p.getStockCourant())).append(p.getUnite() == null ? "" : " " + p.getUnite())
                .append(", seuil ").append(SessionOutils.nombre(p.getSeuilMinimal())).append('\n'));
        return texte.toString();
    }

    private static String lots(String titre, List<?> liste) {
        if (liste == null || liste.isEmpty()) return titre + " : aucun\n";
        StringBuilder texte = new StringBuilder(titre + " (" + liste.size() + ") :\n");
        liste.stream().limit(SessionOutils.MAX_LIGNES).map(LotDto.class::cast).forEach(l -> texte
                .append("- lot ").append(l.getNumero()).append(" de ").append(l.getProduitReference())
                .append(", péremption ").append(l.getPeremption()).append('\n'));
        return texte.toString();
    }

    private static String arrondi(Double valeur) {
        return valeur == null ? "non disponible" : String.format(java.util.Locale.FRANCE, "%.1f", valeur);
    }
}
