package com.backend.common.util;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.dto.FichierGenere;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportTableauTest {

    private static final List<String> COLONNES = List.of("Nom", "Valeur");
    private static final List<List<String>> LIGNES = List.of(List.of("Atlas", "=SOMME(A1:A2)"));

    @Test
    void csvNeutraliseLesFormules() {
        FichierGenere fichier = ExportTableau.generer("csv", "essai", "Essai", COLONNES, LIGNES);
        String contenu = new String(fichier.contenu(), StandardCharsets.UTF_8);
        assertTrue(fichier.nom().endsWith(".csv"));
        assertTrue(contenu.contains("\"'=SOMME(A1:A2)\""));
    }

    @Test
    void excelEtPdfSontGeneres() {
        FichierGenere excel = ExportTableau.generer("excel", "essai", "Essai", COLONNES, LIGNES);
        FichierGenere pdf = ExportTableau.generer("PDF", "essai", "Essai", COLONNES, LIGNES);
        assertTrue(excel.nom().endsWith(".xlsx"));
        assertEquals("application/pdf", pdf.typeMime());
        assertTrue(new String(pdf.contenu(), 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));
    }

    @Test
    void formatInconnuRefuse() {
        assertThrows(BusinessRuleException.class,
                () -> ExportTableau.generer("docx", "essai", "Essai", COLONNES, LIGNES));
    }
}
