package com.backend.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque une méthode de service comme devant être journalisée automatiquement
 * dans la table {@code journal_audit} du schéma tenant.
 *
 * <p>Un aspect AOP (à implémenter) interceptera les méthodes annotées et
 * appellera {@link JournalAuditService} pour persister l'entrée.
 *
 * <p>Exemple d'usage :
 * <pre>
 *   {@literal @}Auditable(action = "CREATION_DEMANDE", entite = "Demande")
 *   public Demande creerDemande(DemandeDto dto) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Code de l'action métier (ex. CREATION_DEMANDE, VALIDATION_ESSAI). */
    String action();

    /** Nom de l'entité concernée (ex. Demande, Echantillon). */
    String entite() default "";

    /** Description lisible par un humain. */
    String description() default "";
}
