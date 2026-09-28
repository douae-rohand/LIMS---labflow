package com.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand un utilisateur tente d'accéder aux données
 * d'un tenant auquel il n'appartient pas.
 * Produit un HTTP 403 Forbidden.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedTenantException extends RuntimeException {

    public UnauthorizedTenantException(String message) {
        super(message);
    }

    public UnauthorizedTenantException(String requestedTenant, String userTenant) {
        super(String.format(
                "Accès refusé : tenant demandé '%s' ne correspond pas au tenant de l'utilisateur '%s'",
                requestedTenant, userTenant));
    }
}
