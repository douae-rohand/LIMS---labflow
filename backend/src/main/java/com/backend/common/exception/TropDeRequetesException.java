package com.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lors d'un dépassement de quota ou d'un délai d'attente imposé (rate limit).
 * Retourne un code HTTP 429 Too Many Requests.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TropDeRequetesException extends RuntimeException {

    public TropDeRequetesException(String message) {
        super(message);
    }
}
