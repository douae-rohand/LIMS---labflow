package com.backend.common.tenant;

import com.backend.common.exception.UnauthorizedTenantException;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TenantDataSourceRegistryTest {

    @Test
    void remplaceLeNomDeBaseDansLUrlJdbc() {
        String url = "jdbc:mysql://localhost:3306/lims_central?useSSL=false";
        assertEquals(
                "jdbc:mysql://localhost:3306/lims_labo_a?useSSL=false",
                TenantDataSourceRegistry.remplacerBase(url, "lims_labo_a"));
    }

    @Test
    void refuseUneCleTenantInvalide() {
        TenantDataSourceRegistry registry = new TenantDataSourceRegistry(
                mock(DataSource.class),
                "jdbc:mysql://localhost:3306/lims_central",
                "root",
                "",
                "com.mysql.cj.jdbc.Driver");
        assertThrows(UnauthorizedTenantException.class, () -> registry.resolve("autre_schema"));
    }
}
