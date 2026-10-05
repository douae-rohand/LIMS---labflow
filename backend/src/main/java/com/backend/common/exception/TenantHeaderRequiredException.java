package com.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand une route exige l'en-tête {@code X-Tenant-ID}
 * et que celui-ci est absent de la requête.
 * Produit un HTTP 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TenantHeaderRequiredException extends RuntimeException {

    public TenantHeaderRequiredException(String message) {
        super(message);
    }
}
