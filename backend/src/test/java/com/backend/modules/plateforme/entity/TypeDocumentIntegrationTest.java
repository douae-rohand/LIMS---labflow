package com.backend.modules.plateforme.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeDocumentIntegrationTest {

    @Test
    void sixDocumentsOfficielsSontDefinits() {
        assertEquals(6, TypeDocumentIntegration.values().length);
        assertEquals("Autorisation d'ouverture et d'exploitation du laboratoire",
                TypeDocumentIntegration.AUTORISATION_OUVERTURE_EXPLOITATION.getLibelle());
        assertTrue(TypeDocumentIntegration.JUSTIFICATIF_ADRESSE.getRaison().contains("adresse"));
    }
}
