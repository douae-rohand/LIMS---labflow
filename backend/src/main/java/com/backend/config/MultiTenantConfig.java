package com.backend.config;

import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantDataSourceRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * Configuration multi-tenant schema-per-tenant.
 *
 * <p>La clé {@code central} pointe vers {@code lims_central}.
 * Les autres clés sont des {@code nom_schema} ({@code lims_<code>}).
 * Une clé inconnue lève une exception ; elle ne retombe jamais sur le central.
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

    @Bean(name = "centralDataSource")
    public DataSource centralDataSource() {
        return DataSourceBuilder.create()
                .url(centralUrl)
                .username(centralUsername)
                .password(centralPassword)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean
    public TenantDataSourceRegistry tenantDataSourceRegistry(
            @Qualifier("centralDataSource") DataSource centralDataSource) {
        return new TenantDataSourceRegistry(
                centralDataSource, centralUrl, centralUsername, centralPassword, driverClassName);
    }

    @Bean
    @Primary
    public DataSource dataSource(TenantDataSourceRegistry registry) {
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(registry);
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("central", registry.getCentral());
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(registry.getCentral());
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    static class TenantRoutingDataSource extends AbstractRoutingDataSource {

        private final TenantDataSourceRegistry registry;

        TenantRoutingDataSource(TenantDataSourceRegistry registry) {
            this.registry = registry;
        }

        @Override
        protected Object determineCurrentLookupKey() {
            String tenant = TenantContext.getCurrentTenant();
            return (tenant != null && !tenant.isBlank()) ? tenant : "central";
        }

        @Override
        protected DataSource determineTargetDataSource() {
            Object lookupKey = determineCurrentLookupKey();
            return registry.resolve(lookupKey == null ? "central" : lookupKey.toString());
        }
    }
}
