package com.backend.integration.llm;

import lombok.Getter;

/**
 * Le modèle de langage n'a pas pu répondre.
 * {@code quotaAtteint} distingue la saturation du fournisseur (HTTP 429) d'une panne (HTTP 503).
 */
@Getter
public class IaIndisponibleException extends RuntimeException {

    private final boolean quotaAtteint;

    public IaIndisponibleException(boolean quotaAtteint, String message) {
        super(message);
        this.quotaAtteint = quotaAtteint;
    }
}
