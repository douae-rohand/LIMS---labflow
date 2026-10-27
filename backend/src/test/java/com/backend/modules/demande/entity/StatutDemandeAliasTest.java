package com.backend.modules.demande.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatutDemandeAliasTest {

    @Test
    void relitLesNomsHistoriques() {
        assertEquals(StatutDemande.NOUVEAU, StatutDemande.depuis("SOUMISE"));
        assertEquals(StatutDemande.NOUVEAU, StatutDemande.depuis("BROUILLON"));
        assertEquals(StatutDemande.REFUSEE, StatutDemande.depuis("REJETEE"));
        assertEquals(StatutDemande.ACCEPTEE, StatutDemande.depuis("ACCEPTEE"));
        assertTrue(StatutDemande.estNouvelle("SOUMISE"));
    }
}
