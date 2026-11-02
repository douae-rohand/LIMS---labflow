package com.backend.common.tenant;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.entity.Laboratoire;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioner {

    private static final String CENTRAL_SCHEMA = "lims_central";
    private static final String SCHEMA_PATTERN = "lims_[a-z0-9_]+";

    private final TenantDataSourceRegistry registry;
    private final JournalAuditService journalAuditService;

    public void provisionner(Laboratoire laboratoire) {
        String nomSchema = laboratoire.getNomSchema();
        if (nomSchema == null || !nomSchema.matches(SCHEMA_PATTERN)) {
            throw new BusinessRuleException("SCHEMA_TENANT_INVALIDE",
                    "Nom de schéma tenant invalide : " + nomSchema);
        }
        
        // Vérifier que le schéma n'existe pas déjà (éviter double soumission)
        if (schemaExiste(nomSchema)) {
            throw new BusinessRuleException("SCHEMA_DEJA_EXISTE",
                    "Le schéma " + nomSchema + " existe déjà. Annulation de la création.");
        }
        
        creerBase(nomSchema);
        DataSource tenantDataSource = registry.enregistrer(nomSchema);
        Flyway.configure()
                .dataSource(tenantDataSource)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .outOfOrder(true)
                .ignoreMigrationPatterns("*:missing")
                .load()
                .migrate();
        log.info("Tenant provisionné : schema={}", nomSchema);
    }

    /**
     * Déprovisionne un tenant : supprime la base de données et retire le DataSource du cache.
     * Utilisé en compensation après échec de création d'administrateur.
     * 
     * @param nomSchema Nom du schéma à supprimer (doit correspondre au pattern lims_[a-z0-9_]+)
     * @throws BusinessRuleException si le nom est invalide, c'est central, ou si le DROP échoue
     */
    public void deprovisionner(String nomSchema) {
        // Validation stricte du nom de schéma
        if (nomSchema == null || nomSchema.isBlank()) {
            throw new BusinessRuleException("SCHEMA_TENANT_INVALIDE",
                    "Nom de schéma null ou vide");
        }
        if (!nomSchema.matches(SCHEMA_PATTERN)) {
            throw new BusinessRuleException("SCHEMA_TENANT_INVALIDE",
                    "Format de schéma invalide (attendu : lims_[a-z0-9_]+) : " + nomSchema);
        }
        if (CENTRAL_SCHEMA.equals(nomSchema)) {
            throw new BusinessRuleException("SCHEMA_CENTRAL_INTERDIT",
                    "Impossible de déprovisionner le schéma central : " + nomSchema);
        }

        log.info("Déprovisionnement du tenant : schema={}", nomSchema);

        try {
            // Fermer le pool de connexions avant de dropper
            DataSource dataSource = registry.resolve(nomSchema);
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikari) {
                hikari.close();
                log.info("Pool Hikari fermé pour le tenant : {}", nomSchema);
            }

            // Supprimer la base de données
            try (Connection connection = registry.getCentral().getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS `" + nomSchema + "`");
                log.info("Base de données supprimée : {}", nomSchema);
            }

            // Retirer du cache
            registry.desenregistrer(nomSchema);
            log.info("Tenant déprovisionné avec succès : schema={}", nomSchema);

        } catch (Exception ex) {
            String msg = "Échec du déprovisionnement du tenant " + nomSchema + " : " + ex.getMessage();
            log.error(msg);
            
            // Enregistrer dans le journal d'audit pour nettoyage manuel
            try {
                journalAuditService.enregistrer(
                        "DEPROVISIONNEMENT_ECHOUE",
                        "Tenant",
                        null,
                        "SYSTEM",
                        "Échec de compensation : " + msg + ". Schéma orphelin à nettoyer manuellement : " + nomSchema
                );
            } catch (Exception auditEx) {
                log.error("Échec de l'enregistrement d'audit pour le déprovisionnement : {}", auditEx.getMessage());
            }
            
            throw new BusinessRuleException("DEPROVISIONNEMENT_ECHOUE", msg);
        }
    }

    private void creerBase(String nomSchema) {
        try (Connection connection = registry.getCentral().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + nomSchema
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (Exception ex) {
            throw new BusinessRuleException("PROVISIONNEMENT_TENANT_ECHOUE",
                    "Impossible de créer la base " + nomSchema + " : " + ex.getMessage());
        }
    }

    private boolean schemaExiste(String nomSchema) {
        try (Connection connection = registry.getCentral().getConnection();
             java.sql.Statement statement = connection.createStatement()) {
            java.sql.ResultSet rs = statement.executeQuery(
                    "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + nomSchema + "'");
            return rs.next();
        } catch (Exception ex) {
            log.error("Erreur lors de la vérification de l'existence du schéma {} : {}", nomSchema, ex.getMessage());
            return false;
        }
    }
}
