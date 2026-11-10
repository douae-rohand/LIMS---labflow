package com.backend.common.util;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Exporte un tableau (en-têtes + lignes de texte) en CSV, Excel ou PDF.
 * Les cellules qui commencent par = + - @ sont neutralisées pour éviter
 * l'exécution de formules à l'ouverture dans un tableur.
 */
public final class ExportTableau {

    private static final DateTimeFormatter JOUR =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Africa/Casablanca"));
    private static final DateTimeFormatter JOUR_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Africa/Casablanca"));
    private static final Color MARQUE = new Color(27, 78, 82);

    private ExportTableau() {
    }

    /**
     * @param format     csv, xlsx (ou excel) ou pdf
     * @param nomBase    début du nom de fichier, complété par la date du jour
     * @param titre      titre affiché en tête du PDF et nom de la feuille Excel
     */
    public static FichierGenere generer(String format, String nomBase, String titre,
                                        List<String> colonnes, List<List<String>> lignes) {
        String jour = JOUR.format(Instant.now());
        return switch (normaliser(format)) {
            case "csv" -> new FichierGenere(nomBase + "-" + jour + ".csv", "text/csv; charset=UTF-8",
                    csv(colonnes, lignes));
            case "xlsx" -> new FichierGenere(nomBase + "-" + jour + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    excel(titre, colonnes, lignes));
            case "pdf" -> new FichierGenere(nomBase + "-" + jour + ".pdf", "application/pdf",
                    pdf(titre, colonnes, lignes));
            default -> throw new BusinessRuleException("FORMAT_EXPORT_INVALIDE",
                    "Le format d'exportation doit être PDF, Excel ou CSV");
        };
    }

    static String normaliser(String format) {
        if (format == null) {
            return "";
        }
        return switch (format.trim().toLowerCase(Locale.ROOT)) {
            case "pdf" -> "pdf";
            case "xlsx", "excel" -> "xlsx";
            case "csv" -> "csv";
            default -> "";
        };
    }

    static String proteger(String valeur) {
        String texte = valeur == null ? "" : valeur;
        int index = 0;
        while (index < texte.length() && " \t\r".indexOf(texte.charAt(index)) >= 0) {
            index++;
        }
        if (index < texte.length() && "=+-@".indexOf(texte.charAt(index)) >= 0) {
            return "'" + texte;
        }
        return texte;
    }

    private static byte[] csv(List<String> colonnes, List<List<String>> lignes) {
        StringBuilder sortie = new StringBuilder("﻿");
        ligneCsv(sortie, colonnes);
        for (List<String> ligne : lignes) {
            ligneCsv(sortie, ligne);
        }
        return sortie.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void ligneCsv(StringBuilder sortie, List<String> cellules) {
        for (int i = 0; i < cellules.size(); i++) {
            if (i > 0) {
                sortie.append(';');
            }
            sortie.append('"').append(proteger(cellules.get(i)).replace("\"", "\"\"")).append('"');
        }
        sortie.append("\r\n");
    }

    private static byte[] excel(String titre, List<String> colonnes, List<List<String>> lignes) {
        try (XSSFWorkbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = classeur.createSheet(nomFeuille(titre));
            CellStyle entete = classeur.createCellStyle();
            org.apache.poi.ss.usermodel.Font police = classeur.createFont();
            police.setBold(true);
            police.setColor(IndexedColors.WHITE.getIndex());
            entete.setFont(police);
            entete.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            entete.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row ligneEntete = feuille.createRow(0);
            for (int i = 0; i < colonnes.size(); i++) {
                Cell cellule = ligneEntete.createCell(i);
                cellule.setCellValue(colonnes.get(i));
                cellule.setCellStyle(entete);
            }
            for (int index = 0; index < lignes.size(); index++) {
                Row ligne = feuille.createRow(index + 1);
                List<String> cellules = lignes.get(index);
                for (int i = 0; i < cellules.size(); i++) {
                    ligne.createCell(i).setCellValue(proteger(cellules.get(i)));
                }
            }
            for (int i = 0; i < colonnes.size(); i++) {
                feuille.autoSizeColumn(i);
            }
            feuille.createFreezePane(0, 1);
            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("EXPORT_EXCEL_ECHOUE", "La création du fichier Excel a échoué");
        }
    }

    private static byte[] pdf(String titre, List<String> colonnes, List<List<String>> lignes) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 36, 36);
            PdfWriter.getInstance(document, sortie);
            document.open();
            Font policeTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, MARQUE);
            Font policeSousTitre = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(70, 80, 84));
            Font policeEntete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font policeCellule = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(36, 42, 46));

            document.add(new Paragraph(titre, policeTitre));
            document.add(new Paragraph("Export du " + JOUR_FR.format(Instant.now()) + " · "
                    + lignes.size() + " ligne(s)", policeSousTitre));
            document.add(new Paragraph(" "));

            PdfPTable tableau = new PdfPTable(colonnes.size());
            tableau.setWidthPercentage(100);
            tableau.setHeaderRows(1);
            for (String colonne : colonnes) {
                PdfPCell cellule = new PdfPCell(new Phrase(colonne, policeEntete));
                cellule.setBackgroundColor(MARQUE);
                cellule.setPadding(5);
                cellule.setHorizontalAlignment(Element.ALIGN_LEFT);
                tableau.addCell(cellule);
            }
            for (List<String> ligne : lignes) {
                for (String valeur : ligne) {
                    PdfPCell cellule = new PdfPCell(new Phrase(valeur == null ? "" : valeur, policeCellule));
                    cellule.setPadding(4);
                    tableau.addCell(cellule);
                }
            }
            document.add(tableau);
            document.close();
            return sortie.toByteArray();
        } catch (IOException | DocumentException ex) {
            throw new BusinessRuleException("EXPORT_PDF_ECHOUE", "La création du fichier PDF a échoué");
        }
    }

    private static String nomFeuille(String titre) {
        String nom = titre == null ? "Export" : titre.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (nom.isEmpty()) {
            return "Export";
        }
        return nom.length() > 31 ? nom.substring(0, 31) : nom;
    }
}
