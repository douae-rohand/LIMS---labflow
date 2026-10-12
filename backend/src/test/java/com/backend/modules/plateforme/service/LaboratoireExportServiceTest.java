package com.backend.modules.plateforme.service;

import com.backend.modules.plateforme.dto.AdministrateurLaboratoireDto;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.dto.LaboratoireDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaboratoireExportServiceTest {

    @Mock
    private PlateformeService plateformeService;

    @InjectMocks
    private LaboratoireExportService exportService;

    @Test
    void csvContientLesLaboratoiresReelsSansDonneesSensibles() {
        when(plateformeService.listerPourExport(null, null, null, true)).thenReturn(List.of(exemple()));

        FichierGenere fichier = exportService.exporter("csv", null, null, null, true);
        String texte = new String(fichier.contenu(), StandardCharsets.UTF_8);

        assertTrue(texte.startsWith("\uFEFF"));
        assertTrue(texte.contains("Nom du laboratoire;Identifiant du laboratoire"));
        assertTrue(texte.contains("Laboratoire Atlas"));
        assertTrue(texte.contains("atlas"));
        assertTrue(texte.contains("Sara Bennani"));
        assertTrue(texte.contains("Actif"));
        assertFalse(texte.contains("lims_atlas"));
        assertFalse(texte.contains("AB123456"));
    }

    @Test
    void pdfEtExcelSontGeneres() {
        when(plateformeService.listerPourExport("casa", "ACTIF", null, false)).thenReturn(List.of(exemple()));

        FichierGenere pdf = exportService.exporter("pdf", "casa", "ACTIF", null, false);
        FichierGenere excel = exportService.exporter("xlsx", "casa", "ACTIF", null, false);

        assertTrue(new String(pdf.contenu(), StandardCharsets.ISO_8859_1).startsWith("%PDF"));
        assertTrue(pdf.nom().endsWith(".pdf"));
        assertTrue(excel.contenu()[0] == 'P' && excel.contenu()[1] == 'K');
        assertTrue(excel.nom().endsWith(".xlsx"));
    }

    private static LaboratoireDto exemple() {
        return LaboratoireDto.builder()
                .nom("Laboratoire Atlas")
                .code("atlas")
                .ville("Casablanca")
                .emailContact("contact@atlas.ma")
                .telephone("0522000000")
                .schemaName("lims_atlas")
                .actif(true)
                .dateCreation(Instant.parse("2026-01-15T10:00:00Z"))
                .administrateur(AdministrateurLaboratoireDto.builder()
                        .nomComplet("Sara Bennani")
                        .cin("AB123456")
                        .build())
                .build();
    }
}
