package com.backend.modules.plateforme.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DocumentIntegrationJsonTest {

    @Test
    void laRelationInverseNeBouclePasEnJson() {
        DemandeIntegration demande = DemandeIntegration.builder()
                .id(1L)
                .numero("INT-1")
                .statut(StatutIntegration.EN_ATTENTE.name())
                .raisonSociale("Laboratoire Oasis")
                .contactNom("Kadiri")
                .contactEmail("noura@oasis.ma")
                .build();
        DocumentIntegration document = DocumentIntegration.builder()
                .id(2L)
                .demande(demande)
                .typeDocument(TypeDocumentIntegration.CIN_BIOLOGISTE_RESPONSABLE)
                .nomFichier("cin.pdf")
                .typeMime("application/pdf")
                .taille(1200L)
                .cheminStockage("integration/cin.pdf")
                .build();
        demande.setDocuments(new ArrayList<>(List.of(document)));

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(demande));
        assertFalse(json.contains("\"demande\""));
    }
}
