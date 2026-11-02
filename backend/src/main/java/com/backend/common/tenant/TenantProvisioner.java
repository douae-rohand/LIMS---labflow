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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioner {

    static final String CENTRAL_SCHEMA = "lims_central";
    private static final Pattern SCHEMA_VALIDE = Pattern.compile("lims_[a-z0-9_]+");
    private static final int ER_DB_CREATE_EXISTS = 1007;

    private final TenantDataSourceRegistry registry;
    private final JournalAuditService journalAuditService;

    public void provisionner(Laboratoire laboratoire) {
        provisionner(laboratoire, null);
    }

    /**
     * Crée le schéma du tenant puis applique les migrations.
     * Échoue si le schéma existe déjà : un second appel (double clic) ne le supprime pas.
     * En cas d'échec après le CREATE, seul le schéma créé par cet appel est retiré.
     */
    public void provisionner(Laboratoire laboratoire, String acteurEmail) {
        String nomSchema = laboratoire.getNomSchema();
        exigerNomSchema(nomSchema);
        if (schemaExiste(nomSchema)) {
            throw new BusinessRuleException("SCHEMA_DEJA_EXISTE",
                    "Le schéma " + nomSchema + " existe déjà. Création annulée.");
        }

        boolean schemaCree = false;
        try {
            creerBase(nomSchema);
            schemaCree = true;
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
        } catch (RuntimeException ex) {
            if (schemaCree) {
                try {
                    deprovisionner(nomSchema, acteurEmail);
                } catch (Exception nettoyage) {
                    log.error("Compensation interne impossible pour le schéma {} : {}",
                            nomSchema, nettoyage.getMessage());
                }
            }
            throw ex;
        }
    }

    /**
     * Supprime un schéma créé par l'appel en cours. Le nom est validé avant toute SQL.
     * Ferme le pool, puis retire l'entrée du registry.
     */
    public void deprovisionner(String nomSchema) {
        deprovisionner(nomSchema, null);
    }

    public void deprovisionner(String nomSchema, String acteurEmail) {
        exigerNomSchema(nomSchema);
        log.info("Déprovisionnement du tenant : schema={}", nomSchema);
        try {
            registry.fermerEtRetirer(nomSchema);
            try (Connection connection = registry.getCentral().getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS `" + nomSchema + "`");
            }
            log.info("Tenant déprovisionné : schema={}", nomSchema);
        } catch (Exception ex) {
            String message = "Échec du déprovisionnement du tenant " + nomSchema + " : " + ex.getMessage();
            log.error(message);
            journalAuditService.enregistrer(
                    "DEPROVISIONNEMENT_ECHOUE",
                    "SchemaTenant",
                    null,
                    acteurEmail,
                    null,
                    "{\"schema\":\"" + nomSchema + "\"}",
                    "Schéma orphelin à nettoyer manuellement : " + nomSchema);
            throw new BusinessRuleException("DEPROVISIONNEMENT_ECHOUE", message);
        }
    }

    boolean schemaExiste(String nomSchema) {
        exigerNomSchema(nomSchema);
        try (Connection connection = registry.getCentral().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?")) {
            statement.setString(1, nomSchema);
            try (ResultSet resultat = statement.executeQuery()) {
                return resultat.next();
            }
        } catch (SQLException ex) {
            throw new BusinessRuleException("SCHEMA_VERIFICATION_ECHOUEE",
                    "Impossible de vérifier le schéma " + nomSchema + " : " + ex.getMessage());
        }
    }

    private void creerBase(String nomSchema) {
        try (Connection connection = registry.getCentral().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + nomSchema
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException ex) {
            if (baseDejaExistante(ex)) {
                throw new BusinessRuleException("SCHEMA_DEJA_EXISTE",
                        "Le schéma " + nomSchema + " existe déjà. Création annulée.");
            }
            throw new BusinessRuleException("PROVISIONNEMENT_TENANT_ECHOUE",
                    "Impossible de créer la base " + nomSchema + " : " + ex.getMessage());
        }
    }

    private void exigerNomSchema(String nomSchema) {
        if (nomSchema == null || !SCHEMA_VALIDE.matcher(nomSchema).matches()) {
            throw new BusinessRuleException("SCHEMA_TENANT_INVALIDE",
                    "Nom de schéma tenant invalide (attendu : lims_[a-z0-9_]+) : " + nomSchema);
        }
        if (CENTRAL_SCHEMA.equals(nomSchema)) {
            throw new BusinessRuleException("SCHEMA_CENTRAL_INTERDIT",
                    "Impossible de déprovisionner le schéma central : " + nomSchema);
        }
    }

    private static boolean baseDejaExistante(Throwable erreur) {
        Throwable courant = erreur;
        while (courant != null) {
            if (courant instanceof SQLException sql && sql.getErrorCode() == ER_DB_CREATE_EXISTS) {
                return true;
            }
            String message = courant.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("database exists")) {
                return true;
            }
            courant = courant.getCause();
        }
        return false;
    }
}
