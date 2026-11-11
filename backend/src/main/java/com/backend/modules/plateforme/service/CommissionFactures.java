package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.config.SchemaConstants;
import com.backend.common.tenant.TenantDataSourceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Report d'un nouveau taux de commission sur les factures pas encore réglées de tous les
 * laboratoires. Les factures payées ou annulées par avoir gardent le taux de leur émission.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionFactures {

    private final TenantDataSourceRegistry registry;
    private final JournalAuditService journalAuditService;

    /** @return nombre de factures mises à jour, tous laboratoires confondus */
    public int appliquerAuxFacturesNonPayees(BigDecimal taux, String acteurEmail) {
        int total = 0;
        for (String schema : schemas()) {
            try (Connection connexion = registry.getCentral().getConnection();
                 PreparedStatement requete = connexion.prepareStatement("UPDATE `" + schema + "`.facture"
                         + " SET taux_commission = ? WHERE statut NOT IN ('PAYEE', 'ANNULEE')")) {
                requete.setBigDecimal(1, taux);
                total += requete.executeUpdate();
            } catch (SQLException ex) {
                log.error("Commission : factures de {} non mises à jour : {}", schema, ex.getMessage());
            }
        }
        journalAuditService.enregistrer("APPLIQUER_COMMISSION_FACTURES", "ParametresPlateforme", null, acteurEmail,
                null, "{\"tauxCommission\":" + taux.toPlainString() + ",\"facturesMisesAJour\":" + total + "}",
                "Nouveau taux appliqué aux factures non réglées");
        return total;
    }

    private List<String> schemas() {
        List<String> schemas = new ArrayList<>();
        try (Connection connexion = registry.getCentral().getConnection();
             PreparedStatement requete = connexion.prepareStatement(
                     "SELECT l.nom_schema FROM " + SchemaConstants.CENTRAL_SCHEMA + ".laboratoire l"
                             + " JOIN INFORMATION_SCHEMA.SCHEMATA x ON x.SCHEMA_NAME = l.nom_schema"
                             + " WHERE l.nom_schema REGEXP '^lims_[a-z0-9_]+$'");
             ResultSet rs = requete.executeQuery()) {
            while (rs.next()) {
                schemas.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            log.error("Commission : liste des laboratoires illisible : {}", ex.getMessage());
        }
        return schemas;
    }
}
