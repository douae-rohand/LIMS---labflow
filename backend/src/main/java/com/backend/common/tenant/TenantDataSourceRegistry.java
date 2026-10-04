package com.backend.common.tenant;

import com.backend.common.exception.UnauthorizedTenantException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Résout et met en cache les DataSources tenant. Une clé inconnue
 * lève une exception au lieu de retomber sur le schéma central.
 */
@Slf4j
public class TenantDataSourceRegistry {

    private static final String CENTRAL_KEY = "central";

    private final DataSource centralDataSource;
    private final String centralUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final ConcurrentHashMap<String, DataSource> cache = new ConcurrentHashMap<>();

    public TenantDataSourceRegistry(
            DataSource centralDataSource,
            String centralUrl,
            String username,
            String password,
            String driverClassName) {
        this.centralDataSource = centralDataSource;
        this.centralUrl = centralUrl;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        cache.put(CENTRAL_KEY, centralDataSource);
    }

    public DataSource getCentral() {
        return centralDataSource;
    }

    public DataSource resolve(String lookupKey) {
        if (lookupKey == null || lookupKey.isBlank() || CENTRAL_KEY.equals(lookupKey)) {
            return centralDataSource;
        }
        if (!lookupKey.matches("lims_[a-z0-9_]+")) {
            throw new UnauthorizedTenantException("Identifiant de tenant invalide : " + lookupKey);
        }
        if (!laboratoireExiste(lookupKey)) {
            throw new UnauthorizedTenantException("Tenant inconnu : " + lookupKey);
        }
        return cache.computeIfAbsent(lookupKey, this::creerDataSource);
    }

    public DataSource enregistrer(String nomSchema) {
        DataSource dataSource = creerDataSource(nomSchema);
        cache.put(nomSchema, dataSource);
        return dataSource;
    }

    public String urlPourSchema(String nomSchema) {
        return remplacerBase(centralUrl, nomSchema);
    }

    private boolean laboratoireExiste(String nomSchema) {
        Integer count = new JdbcTemplate(centralDataSource).queryForObject(
                "SELECT COUNT(*) FROM laboratoire WHERE nom_schema = ?",
                Integer.class,
                nomSchema);
        return count != null && count > 0;
    }

    private DataSource creerDataSource(String nomSchema) {
        log.info("Ouverture de la DataSource tenant {}", nomSchema);
        return DataSourceBuilder.create()
                .url(urlPourSchema(nomSchema))
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    static String remplacerBase(String jdbcUrl, String nomSchema) {
        int schemeEnd = jdbcUrl.indexOf("://");
        int pathStart = jdbcUrl.indexOf('/', schemeEnd + 3);
        if (pathStart < 0) {
            throw new IllegalArgumentException("URL JDBC sans nom de base : " + jdbcUrl);
        }
        int queryStart = jdbcUrl.indexOf('?', pathStart);
        String prefix = jdbcUrl.substring(0, pathStart + 1);
        String suffix = queryStart >= 0 ? jdbcUrl.substring(queryStart) : "";
        return prefix + nomSchema + suffix;
    }
}
