package com.backend.modules.plateforme.entity;

/**
 * Statut d'une demande d'intégration d'un laboratoire sur la plateforme.
 */
public enum StatutIntegration {
    EN_ATTENTE,
    EN_COURS_VALIDATION,
    APPROUVEE,
    REJETEE,
    SUSPENDUE
}
