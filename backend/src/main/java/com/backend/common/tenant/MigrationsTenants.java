package com.backend.common.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Migrations Flyway des schémas tenant déjà créés. Un tenant ne reçoit ses migrations qu'à
 * sa création : au démarrage, les migrations ajoutées depuis sont appliquées à chaque schéma
 * existant (désactivable avec {@code app.tenants.migrer-au-demarrage=false}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MigrationsTenants {

    private static final String EMPLACEMENT = "classpath:db/migration/tenant";

    private final TenantDataSourceRegistry registry;

    @Value("${app.tenants.migrer-au-demarrage:true}")
    private boolean migrerAuDemarrage;

    @Order(10)
    @EventListener(ApplicationReadyEvent.class)
    public void migrerTousAuDemarrage() {
        if (!migrerAuDemarrage) {
            return;
        }
        for (String schema : schemasExistants()) {
            try {
                int appliquees = configurer(registry.resolve(schema)).migrate().migrationsExecuted;
                if (appliquees > 0) {
                    log.info("Tenant {} : {} migration(s) appliquée(s) au démarrage", schema, appliquees);
                }
            } catch (RuntimeException ex) {
                log.error("Tenant {} : migration impossible au démarrage : {}", schema, ex.getMessage());
            }
        }
    }

    private static Flyway configurer(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(EMPLACEMENT)
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .outOfOrder(true)
                .ignoreMigrationPatterns("*:missing")
                .load();
    }

    /** Schémas des laboratoires enregistrés qui existent réellement sur le serveur. */
    private List<String> schemasExistants() {
        List<String> schemas = new ArrayList<>();
        String sql = "SELECT l.nom_schema FROM " + com.backend.common.config.SchemaConstants.CENTRAL_SCHEMA + ".laboratoire l "
                + "JOIN INFORMATION_SCHEMA.SCHEMATA s ON s.SCHEMA_NAME = l.nom_schema "
                + "WHERE l.nom_schema REGEXP '^lims_[a-z0-9_]+$'";
        try (Connection connexion = registry.getCentral().getConnection();
             PreparedStatement requete = connexion.prepareStatement(sql);
             ResultSet resultat = requete.executeQuery()) {
            while (resultat.next()) {
                schemas.add(resultat.getString(1));
            }
        } catch (Exception ex) {
            log.error("Liste des schémas tenant illisible : {}", ex.getMessage());
        }
        return schemas;
    }
}
