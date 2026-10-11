package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.dto.AdministrateurLaboratoireDto;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.dto.LaboratoireDto;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaboratoireExportService {

    static final List<String> COLONNES = List.of(
            "Nom du laboratoire",
            "Identifiant du laboratoire",
            "Ville",
            "Adresse e-mail",
            "Téléphone",
            "Administrateur du laboratoire",
            "Date d'intégration",
            "Statut du laboratoire"
    );

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Africa/Casablanca"));

    private final PlateformeService plateformeService;

    @Transactional(readOnly = true)
    public FichierGenere exporter(String format, String recherche, String statut, String ville, boolean tous) {
        List<LaboratoireDto> laboratoires = plateformeService.listerPourExport(recherche, statut, ville, tous);
        String jour = DATE.format(Instant.now());
        return switch (normaliserFormat(format)) {
            case "pdf" -> new FichierGenere(
                    "laboratoires-" + jour + ".pdf", "application/pdf", pdf(laboratoires, jour));
            case "xlsx" -> new FichierGenere(
                    "laboratoires-" + jour + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    excel(laboratoires));
            case "csv" -> new FichierGenere(
                    "laboratoires-" + jour + ".csv", "text/csv; charset=UTF-8", csv(laboratoires));
            default -> throw new BusinessRuleException("FORMAT_EXPORT_INVALIDE",
                    "Le format d'exportation doit être PDF, Excel ou CSV");
        };
    }

    private static String normaliserFormat(String format) {
        if (format == null) {
            return "";
        }
        return switch (format.trim().toLowerCase()) {
            case "pdf" -> "pdf";
            case "xlsx", "excel" -> "xlsx";
            case "csv" -> "csv";
            default -> "";
        };
    }

    private byte[] csv(List<LaboratoireDto> laboratoires) {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append(String.join(";", COLONNES)).append("\r\n");
        for (LaboratoireDto laboratoire : laboratoires) {
            List<String> cellules = valeurs(laboratoire);
            for (int i = 0; i < cellules.size(); i++) {
                if (i > 0) {
                    builder.append(';');
                }
                builder.append(echapperCsv(cellules.get(i)));
            }
            builder.append("\r\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] excel(List<LaboratoireDto> laboratoires) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = workbook.createSheet("Laboratoires");
            CellStyle entete = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font police = workbook.createFont();
            police.setBold(true);
            police.setColor(IndexedColors.WHITE.getIndex());
            entete.setFont(police);
            entete.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            entete.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row ligneEntete = feuille.createRow(0);
            for (int i = 0; i < COLONNES.size(); i++) {
                Cell cellule = ligneEntete.createCell(i);
                cellule.setCellValue(COLONNES.get(i));
                cellule.setCellStyle(entete);
            }
            int index = 1;
            for (LaboratoireDto laboratoire : laboratoires) {
                Row ligne = feuille.createRow(index++);
                List<String> cellules = valeurs(laboratoire);
                for (int i = 0; i < cellules.size(); i++) {
                    ligne.createCell(i).setCellValue(cellules.get(i));
                }
            }
            for (int i = 0; i < COLONNES.size(); i++) {
                feuille.autoSizeColumn(i);
            }
            feuille.createFreezePane(0, 1);
            workbook.write(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("EXPORT_EXCEL_ECHOUE", "La création du fichier Excel a échoué");
        }
    }

    private byte[] pdf(List<LaboratoireDto> laboratoires, String jour) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 36, 36);
            PdfWriter.getInstance(document, sortie);
            document.open();

            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(27, 78, 82));
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(70, 80, 84));
            Font entete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font cellule = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(36, 42, 46));

            document.add(new Paragraph("Laboratoires LabFlow", titre));
            document.add(new Paragraph(
                    "Export du " + jour + " · " + laboratoires.size() + " laboratoire(s)", sousTitre));
            document.add(new Paragraph(" "));

            PdfPTable tableau = new PdfPTable(COLONNES.size());
            tableau.setWidthPercentage(100);
            tableau.setHeaderRows(1);
            tableau.setWidths(new float[]{2.2f, 1.5f, 1.3f, 2.2f, 1.4f, 2f, 1.3f, 1.2f});
            for (String colonne : COLONNES) {
                PdfPCell cell = new PdfPCell(new Phrase(colonne, entete));
                cell.setBackgroundColor(new Color(27, 78, 82));
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                tableau.addCell(cell);
            }
            for (LaboratoireDto laboratoire : laboratoires) {
                for (String valeur : valeurs(laboratoire)) {
                    PdfPCell cell = new PdfPCell(new Phrase(valeur == null ? "" : valeur, cellule));
                    cell.setPadding(4);
                    tableau.addCell(cell);
                }
            }
            document.add(tableau);
            document.close();
            return sortie.toByteArray();
        } catch (IOException | com.lowagie.text.DocumentException ex) {
            throw new BusinessRuleException("EXPORT_PDF_ECHOUE", "La création du fichier PDF a échoué");
        }
    }

    private static List<String> valeurs(LaboratoireDto laboratoire) {
        AdministrateurLaboratoireDto admin = laboratoire.getAdministrateur();
        return List.of(
                texte(laboratoire.getNom()),
                texte(laboratoire.getCode()),
                texte(laboratoire.getVille()),
                texte(laboratoire.getEmailContact()),
                texte(laboratoire.getTelephone()),
                admin == null ? "" : texte(admin.getNomComplet()),
                laboratoire.getDateCreation() == null ? "" : DATE.format(laboratoire.getDateCreation()),
                laboratoire.isActif() ? "Actif" : "Désactivé"
        );
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private static String echapperCsv(String valeur) {
        String brut = valeur == null ? "" : valeur;
        if (brut.contains(";") || brut.contains("\"") || brut.contains("\n") || brut.contains("\r")) {
            return "\"" + brut.replace("\"", "\"\"") + "\"";
        }
        return brut;
    }
}
