package com.backend.modules.essai.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatutEssaiConverterTest {

    private final StatutEssaiConverter converter = new StatutEssaiConverter();

    @Test
    void relitLesAliasHistoriquesVersLeStatutCanonique() {
        assertEquals(StatutEssai.PLANIFIEE, converter.convertToEntityAttribute("PLANIFIE"));
        assertEquals(StatutEssai.TERMINE, converter.convertToEntityAttribute("TERMINEE"));
        assertEquals(StatutEssai.ANNULE, converter.convertToEntityAttribute("ANNULEE"));
        assertEquals(StatutEssai.EN_COURS, converter.convertToEntityAttribute("EN_COURS"));
    }

    @Test
    void ecritToujoursLeNomCanonique() {
        assertEquals("PLANIFIEE", converter.convertToDatabaseColumn(StatutEssai.PLANIFIEE));
        assertEquals("TERMINE", converter.convertToDatabaseColumn(StatutEssai.TERMINE));
        assertEquals("ANNULE", converter.convertToDatabaseColumn(StatutEssai.ANNULE));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void ignoreUneValeurInconnue() {
        assertNull(converter.convertToEntityAttribute("INCONNU"));
        assertNull(converter.convertToEntityAttribute("  "));
    }
}
