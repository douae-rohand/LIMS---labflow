package com.backend.common.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Résolveur d'identifiant de tenant utilisé par la couche JPA / Hibernate.
 *
 * <p>Retourne le tenant courant depuis {@link TenantContext}.
 * Si aucun tenant n'est défini (ex. démarrage, tâches planifiées),
 * retourne la valeur par défaut {@code "central"}.
 *
 * <p>Ce composant peut être intégré à un {@code CurrentTenantIdentifierResolver}
 * Hibernate si une approche Hibernate multi-tenancy est choisie à la place
 * du {@code AbstractRoutingDataSource} de Spring.
 */
@Slf4j
@Component
public class TenantIdentifierResolver {

    private static final String DEFAULT_TENANT = "central";

    /**
     * Retourne l'identifiant du tenant courant, ou {@code "central"} par défaut.
     */
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank()) {
            log.trace("Aucun tenant dans le contexte — utilisation du tenant par défaut '{}'", DEFAULT_TENANT);
            return DEFAULT_TENANT;
        }
        return tenant;
    }

    /**
     * Indique si le tenant courant peut être utilisé sans qu'une transaction
     * soit déjà ouverte.
     */
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
