package com.backend.common.tenant;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.entity.Laboratoire;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioner {

    static final String CENTRAL_SCHEMA = "lims_central";
    private static final Pattern SCHEMA_VALIDE = Pattern.compile("lims_[a-z0-9_]+");
    private static final int ER_DB_CREATE_EXISTS = 1007;

    /** Laboratoires de démonstration : seuls à garder les données fictives de V22. */
    private static final Set<String> SCHEMAS_DEMO = Set.of("lims_atlas", "lims_nord");
    private static final String VERSION_SEED_DEMO = "22";
    /** Tables d'activité remplies par V22, dans l'ordre des clés étrangères. Le catalogue est conservé. */
    private static final List<String> TABLES_ACTIVITE = List.of(
            "consommation_lot", "validation", "rapport_ligne", "rapport", "paiement", "facture_ligne", "facture",
            "enquete_satisfaction", "synthese_ia", "evenement_metier", "ligne_essai", "echantillon",
            "piece_jointe", "demande", "lot", "patient", "client");

    private final TenantDataSourceRegistry registry;
    private final JournalAuditService journalAuditService;

    public void provisionner(Laboratoire laboratoire) {
        provisionner(laboratoire, null);
    }

    /**
     * Crée le schéma du tenant puis applique les migrations.
     * Échoue si le schéma existe déjà : un second appel (double clic) ne le supprime pas.
     * En cas d'échec après le CREATE, seul le schéma créé par cet appel est retiré, y compris
     * si la transaction appelante est annulée plus tard (jusqu'au commit).
     * À appeler avant toute écriture dans le schéma central : certaines migrations tenant lisent
     * {@code lims_central.laboratoire} et attendraient une ligne non validée par l'appelant.
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
            if (!SCHEMAS_DEMO.contains(nomSchema)) {
                // V22 insère des données fictives partout, et V26 échoue sur ses échantillons tant que
                // le laboratoire n'a pas d'utilisateur : elles sont retirées entre les deux.
                migrations(tenantDataSource).target(VERSION_SEED_DEMO).load().migrate();
                viderDonneesDemo(tenantDataSource, nomSchema);
            }
            migrations(tenantDataSource).load().migrate();
            retirerSiTransactionAnnulee(nomSchema, acteurEmail);
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
            journalAuditService.enregistrerEchec(
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

    private static FluentConfiguration migrations(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .outOfOrder(true)
                .ignoreMigrationPatterns("*:missing");
    }

    /** Uniquement sur un schéma que cet appel vient de créer : aucune donnée réelle n'y existe encore. */
    private static void viderDonneesDemo(DataSource dataSource, String nomSchema) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String table : TABLES_ACTIVITE) {
                statement.executeUpdate("DELETE FROM `" + nomSchema + "`.`" + table + "`");
            }
        } catch (SQLException ex) {
            throw new BusinessRuleException("PROVISIONNEMENT_TENANT_ECHOUE",
                    "Impossible de retirer les données de démonstration de " + nomSchema + " : " + ex.getMessage());
        }
    }

    /** CREATE DATABASE n'est pas annulable : le schéma est supprimé si la transaction appelante échoue. */
    private void retirerSiTransactionAnnulee(String nomSchema, String acteurEmail) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_UNKNOWN) {
                    // Commit peut-être effectué : ne pas supprimer un schéma utilisé.
                    log.error("Résultat de la transaction inconnu : schéma {} à vérifier", nomSchema);
                    return;
                }
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    deprovisionner(nomSchema, acteurEmail);
                    log.info("Compensation : schéma {} supprimé", nomSchema);
                } catch (Exception ex) {
                    log.error("Échec de la compensation du schéma {} : {}", nomSchema, ex.getMessage());
                }
            }
        });
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
