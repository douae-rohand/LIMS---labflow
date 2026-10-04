package com.backend.common.tenant;

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

    private final TenantDataSourceRegistry registry;

    public void provisionner(Laboratoire laboratoire) {
        String nomSchema = laboratoire.getNomSchema();
        if (nomSchema == null || !nomSchema.matches("lims_[a-z0-9_]+")) {
            throw new BusinessRuleException("SCHEMA_TENANT_INVALIDE",
                    "Nom de schéma tenant invalide : " + nomSchema);
        }
        creerBase(nomSchema);
        DataSource tenantDataSource = registry.enregistrer(nomSchema);
        Flyway.configure()
                .dataSource(tenantDataSource)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load()
                .migrate();
        log.info("Tenant provisionné : schema={}", nomSchema);
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
}
