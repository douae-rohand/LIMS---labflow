package com.backend.modules.essai.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatutEssaiConverterTest {

    private final StatutEssaiConverter converter = new StatutEssaiConverter();

    @Test
    void relitLesAliasHistoriquesVersLeStatutCanonique() {
        assertEquals(StatutEssai.ATTENTE_ECHANTILLON, converter.convertToEntityAttribute("PLANIFIE"));
        assertEquals(StatutEssai.ATTENTE_ECHANTILLON, converter.convertToEntityAttribute("EN_ATTENTE"));
        assertEquals(StatutEssai.ATTENTE_ECHANTILLON, converter.convertToEntityAttribute("REPORTEE"));
        assertEquals(StatutEssai.EN_COURS, converter.convertToEntityAttribute("A_REFAIRE"));
        assertEquals(StatutEssai.TERMINE, converter.convertToEntityAttribute("TERMINEE"));
        assertEquals(StatutEssai.NON_REALISABLE, converter.convertToEntityAttribute("ANNULEE"));
        assertEquals(StatutEssai.EN_COURS, converter.convertToEntityAttribute("EN_COURS"));
    }

    @Test
    void ecritToujoursLeNomCanonique() {
        assertEquals("ATTENTE_ECHANTILLON", converter.convertToDatabaseColumn(StatutEssai.ATTENTE_ECHANTILLON));
        assertEquals("TERMINE", converter.convertToDatabaseColumn(StatutEssai.TERMINE));
        assertEquals("NON_REALISABLE", converter.convertToDatabaseColumn(StatutEssai.NON_REALISABLE));
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void ignoreUneValeurInconnue() {
        assertNull(converter.convertToEntityAttribute("INCONNU"));
        assertNull(converter.convertToEntityAttribute("  "));
    }
}
