package com.backend.modules.facturation.service;

import com.backend.common.config.SchemaConstants;
import com.backend.common.tenant.TenantDataSourceRegistry;
import com.backend.modules.notification.service.NotificationsLaboratoire;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Passe « EN_RETARD » les factures non soldées dont l'échéance est dépassée (une fois par heure).
 * Une facture payée ou annulée par avoir n'est jamais concernée ; l'accueil est prévenu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacturesEnRetard {

    private final TenantDataSourceRegistry registry;
    private final NotificationsLaboratoire notificationsLaboratoire;

    @Scheduled(initialDelay = 90_000, fixedDelay = 3_600_000)
    public void marquer() {
        laboratoires().forEach((laboratoireId, schema) -> {
            String s = "`" + schema + "`";
            try (Connection connexion = registry.getCentral().getConnection();
                 PreparedStatement requete = connexion.prepareStatement(
                         "UPDATE " + s + ".facture f SET f.statut = 'EN_RETARD'"
                                 + " WHERE f.statut IN ('IMPAYEE', 'EMISE') AND f.date_echeance < CURDATE()"
                                 + " AND f.montant_ht > COALESCE((SELECT SUM(p.montant) FROM " + s + ".paiement p"
                                 + "   WHERE p.facture_id = f.id_facture), 0)")) {
                int nombre = requete.executeUpdate();
                if (nombre > 0) {
                    log.info("{} facture(s) passée(s) en retard pour {}", nombre, schema);
                    notificationsLaboratoire.notifierApresCommit(RoleUtilisateur.ACCUEIL, laboratoireId,
                            "FACTURES_EN_RETARD", nombre + " facture(s) en retard",
                            "Leur échéance est dépassée et elles ne sont pas réglées.", "/app/caisse");
                }
            } catch (SQLException ex) {
                log.error("Factures en retard : laboratoire {} non traité : {}", schema, ex.getMessage());
            }
        });
    }

    private Map<Long, String> laboratoires() {
        Map<Long, String> laboratoires = new LinkedHashMap<>();
        try (Connection connexion = registry.getCentral().getConnection();
             PreparedStatement requete = connexion.prepareStatement(
                     "SELECT l.id_laboratoire, l.nom_schema FROM " + SchemaConstants.CENTRAL_SCHEMA + ".laboratoire l"
                             + " JOIN INFORMATION_SCHEMA.SCHEMATA x ON x.SCHEMA_NAME = l.nom_schema"
                             + " WHERE l.nom_schema REGEXP '^lims_[a-z0-9_]+$'");
             ResultSet rs = requete.executeQuery()) {
            while (rs.next()) {
                laboratoires.put(rs.getLong(1), rs.getString(2));
            }
        } catch (SQLException ex) {
            log.error("Factures en retard : liste des laboratoires illisible : {}", ex.getMessage());
        }
        return laboratoires;
    }
}
