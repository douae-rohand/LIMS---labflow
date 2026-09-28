package com.backend.common.tenant;

/**
 * Stocke l'identifiant du tenant courant dans un ThreadLocal.
 * Doit être nettoyé (clear) après chaque requête HTTP (cf. TenantInterceptor).
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // utilitaire statique – pas d'instanciation
    }

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
