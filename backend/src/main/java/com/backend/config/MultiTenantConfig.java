package com.backend.config;

import com.backend.common.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration multi-tenant « schema-per-tenant ».
 *
 * <p>Architecture :
 * <ul>
 *   <li>Un DataSource <em>central</em> (schéma {@code lims_central}) contient les tables
 *       {@code laboratoire} et {@code utilisateur_super_admin}.</li>
 *   <li>Après authentification, le tenant est résolu depuis le JWT et stocké dans
 *       {@link TenantContext}. Le {@link TenantRoutingDataSource} route alors toutes
 *       les requêtes JPA vers le schéma {@code lims_<tenantId>}.</li>
 * </ul>
 *
 * <p>TODO : alimenter dynamiquement la map des DataSources à partir de la table
 * {@code laboratoire} (lazy creation ou cache Caffeine).
 */
@Configuration
public class MultiTenantConfig {

    @Value("${spring.datasource.url}")
    private String centralUrl;

    @Value("${spring.datasource.username}")
    private String centralUsername;

    @Value("${spring.datasource.password}")
    private String centralPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClassName;

    // -------------------------------------------------------------------------
    // DataSource central (authentification, table laboratoire)
    // -------------------------------------------------------------------------

    @Bean(name = "centralDataSource")
    public DataSource centralDataSource() {
        return DataSourceBuilder.create()
                .url(centralUrl)
                .username(centralUsername)
                .password(centralPassword)
                .driverClassName(driverClassName)
                .build();
    }

    // -------------------------------------------------------------------------
    // DataSource de routage principal (utilisé par Spring Data JPA)
    // -------------------------------------------------------------------------

    @Bean
    @Primary
    public DataSource dataSource() {
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();

        // La clé null (absent de contexte) → DataSource central
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("central", centralDataSource());

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(centralDataSource());
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }

    // -------------------------------------------------------------------------
    // AbstractRoutingDataSource interne
    // -------------------------------------------------------------------------

    /**
     * Route vers le schéma du tenant courant (lu depuis {@link TenantContext}).
     * Si aucun tenant n'est défini (ex. pendant l'authentification), utilise « central ».
     *
     * <p>TODO : créer dynamiquement le DataSource du tenant s'il n'existe pas encore
     * dans la map (connexion à {@code lims_<tenantId>}).
     */
    static class TenantRoutingDataSource extends AbstractRoutingDataSource {

        @Override
        protected Object determineCurrentLookupKey() {
            String tenant = TenantContext.getCurrentTenant();
            return (tenant != null && !tenant.isBlank()) ? tenant : "central";
        }
    }
}
