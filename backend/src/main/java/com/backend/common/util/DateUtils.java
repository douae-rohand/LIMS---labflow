package com.backend.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utilitaires de manipulation de dates et d'heures.
 * Toutes les méthodes sont statiques (classe non instanciable).
 */
public final class DateUtils {

    /** Format ISO 8601 date (ex. 2025-06-15). */
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Format date-heure lisible (ex. 15/06/2025 14:30). */
    public static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateUtils() {
        // utilitaire statique
    }

    // -------------------------------------------------------------------------
    // Conversions
    // -------------------------------------------------------------------------

    public static LocalDate toLocalDate(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }

    public static LocalDate toLocalDate(Instant instant) {
        return toLocalDate(instant, ZoneOffset.UTC);
    }

    public static Instant toInstant(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC);
    }

    // -------------------------------------------------------------------------
    // Calculs
    // -------------------------------------------------------------------------

    /** Nombre de jours entre deux instants (valeur absolue). */
    public static long joursEntre(Instant debut, Instant fin) {
        return Math.abs(ChronoUnit.DAYS.between(debut, fin));
    }

    /** Indique si l'instant donné est dans le passé. */
    public static boolean estExpire(Instant instant) {
        return instant.isBefore(Instant.now());
    }

    /** Indique si la date d'aujourd'hui est dans la plage [debut, fin]. */
    public static boolean estDansPlage(LocalDate debut, LocalDate fin) {
        LocalDate aujourdhui = LocalDate.now(ZoneOffset.UTC);
        return !aujourdhui.isBefore(debut) && !aujourdhui.isAfter(fin);
    }

    // -------------------------------------------------------------------------
    // Formatage
    // -------------------------------------------------------------------------

    public static String formaterDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    public static String formaterAffichage(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DISPLAY_FORMATTER) : "";
    }
}
