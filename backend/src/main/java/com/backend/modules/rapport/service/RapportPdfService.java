package com.backend.modules.rapport.service;

import com.backend.common.exception.BusinessRuleException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Mise en page du rapport d'analyse (OpenPDF). */
@Service
public class RapportPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")
            .withLocale(Locale.FRENCH).withZone(ZoneId.of("Africa/Casablanca"));
    private static final Color MARQUE = new Color(27, 78, 82);
    private static final Color GRIS = new Color(90, 100, 104);

    public record Laboratoire(String nom, String adresse, String contact, String ice) {
    }

    public record LigneRapport(String essai, String methode, String resultat, String unite,
                               String reference, String conformite, String validePar) {
    }

    public record DonneesRapport(Laboratoire laboratoire, String numero, int version, String demandeReference,
                                 String demandeTitre, String client, Instant dateEmission,
                                 List<LigneRapport> lignes, String signataire, Instant dateSignature,
                                 boolean projet, byte[] imageSignature,
                                 List<AnalyseNonRealisee> nonRealisees) {
    }

    /** Analyse demandée mais non réalisée : le rapport est alors partiel (CDC M07). */
    public record AnalyseNonRealisee(String essai, String motif) {
    }

    public byte[] generer(DonneesRapport donnees) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, sortie);
            document.open();

            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, MARQUE);
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, MARQUE);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font discret = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
            Font gras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font projet = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(176, 58, 46));

            Laboratoire labo = donnees.laboratoire();
            document.add(new Paragraph(texte(labo.nom()), titre));
            if (labo.adresse() != null) document.add(new Paragraph(labo.adresse(), discret));
            if (labo.contact() != null) document.add(new Paragraph(labo.contact(), discret));
            if (labo.ice() != null) document.add(new Paragraph("ICE : " + labo.ice(), discret));
            document.add(Chunk.NEWLINE);

            Paragraph entete = new Paragraph("Rapport d'analyse n° " + donnees.numero()
                    + (donnees.version() > 1 ? " (version " + donnees.version() + ")" : ""), sousTitre);
            document.add(entete);
            if (donnees.projet()) {
                document.add(new Paragraph("PROJET — document non signé, sans valeur officielle", projet));
            }
            boolean partiel = donnees.nonRealisees() != null && !donnees.nonRealisees().isEmpty();
            if (partiel) {
                document.add(new Paragraph("RAPPORT PARTIEL — " + donnees.nonRealisees().size()
                        + " analyse(s) demandée(s) n'ont pas pu être réalisées (voir la liste ci-dessous).", projet));
            }
            document.add(Chunk.NEWLINE);

            PdfPTable infos = new PdfPTable(new float[]{1.2f, 3f});
            infos.setWidthPercentage(100);
            ligneInfo(infos, "Demande", donnees.demandeReference() + " — " + texte(donnees.demandeTitre()), gras, normal);
            ligneInfo(infos, "Client", texte(donnees.client()), gras, normal);
            ligneInfo(infos, "Date d'émission", donnees.dateEmission() == null ? "—" : DATE.format(donnees.dateEmission()), gras, normal);
            document.add(infos);
            document.add(Chunk.NEWLINE);

            PdfPTable tableau = new PdfPTable(new float[]{2.4f, 1.6f, 1.3f, 0.8f, 1.5f, 1.1f, 1.6f});
            tableau.setWidthPercentage(100);
            tableau.setHeaderRows(1);
            Font entetes = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            for (String colonne : List.of("Analyse", "Méthode", "Résultat", "Unité", "Valeurs de référence", "Conformité", "Validé par")) {
                PdfPCell cellule = new PdfPCell(new Phrase(colonne, entetes));
                cellule.setBackgroundColor(MARQUE);
                cellule.setPadding(5);
                tableau.addCell(cellule);
            }
            Font cellules = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            for (LigneRapport ligne : donnees.lignes()) {
                for (String valeur : List.of(texte(ligne.essai()), texte(ligne.methode()), texte(ligne.resultat()),
                        texte(ligne.unite()), texte(ligne.reference()), texte(ligne.conformite()), texte(ligne.validePar()))) {
                    PdfPCell cellule = new PdfPCell(new Phrase(valeur, cellules));
                    cellule.setPadding(4);
                    tableau.addCell(cellule);
                }
            }
            document.add(tableau);
            document.add(Chunk.NEWLINE);

            if (partiel) {
                document.add(new Paragraph("Analyses non réalisées", sousTitre));
                PdfPTable absentes = new PdfPTable(new float[]{2f, 4f});
                absentes.setWidthPercentage(100);
                for (String colonne : List.of("Analyse", "Motif")) {
                    PdfPCell cellule = new PdfPCell(new Phrase(colonne, entetes));
                    cellule.setBackgroundColor(MARQUE);
                    cellule.setPadding(5);
                    absentes.addCell(cellule);
                }
                for (AnalyseNonRealisee absente : donnees.nonRealisees()) {
                    for (String valeur : List.of(texte(absente.essai()), texte(absente.motif()))) {
                        PdfPCell cellule = new PdfPCell(new Phrase(valeur, cellules));
                        cellule.setPadding(4);
                        absentes.addCell(cellule);
                    }
                }
                document.add(absentes);
                document.add(Chunk.NEWLINE);
            }

            if (donnees.projet()) {
                document.add(new Paragraph("Signature : en attente de signature par le responsable du laboratoire.", discret));
            } else {
                document.add(cadreSignature(donnees, labo, gras, normal, discret));
            }
            document.close();
            return sortie.toByteArray();
        } catch (IOException | DocumentException ex) {
            throw new BusinessRuleException("RAPPORT_PDF_ECHOUE", "Le PDF du rapport n'a pas pu être généré.");
        }
    }

    /** Cadre de signature lisible par tous, aligné à droite en bas du rapport. */
    private static PdfPTable cadreSignature(DonneesRapport donnees, Laboratoire labo, Font gras, Font normal, Font discret) {
        PdfPTable cadre = new PdfPTable(1);
        cadre.setWidthPercentage(55);
        cadre.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph texte = new Paragraph();
        texte.add(new Phrase("Signé électroniquement par\n", discret));
        texte.add(new Phrase(texte(donnees.signataire()) + "\n", gras));
        texte.add(new Phrase("Responsable du laboratoire\n", normal));
        texte.add(new Phrase(texte(labo.nom()) + "\n", normal));
        texte.add(new Phrase("Le " + DATE.format(donnees.dateSignature()), normal));
        PdfPCell cellule = new PdfPCell();
        cellule.addElement(texte);
        if (donnees.imageSignature() != null) {
            try {
                Image signature = Image.getInstance(donnees.imageSignature());
                signature.scaleToFit(160, 60);
                signature.setSpacingBefore(6);
                cellule.addElement(signature);
            } catch (Exception ex) {
                // Image illisible : le cadre reste valable sans elle.
            }
        }
        cellule.setBorderColor(MARQUE);
        cellule.setBorderWidth(1.2f);
        cellule.setPadding(10);
        cadre.addCell(cellule);
        return cadre;
    }

    private static void ligneInfo(PdfPTable table, String libelle, String valeur, Font gras, Font normal) {
        PdfPCell gauche = new PdfPCell(new Phrase(libelle, gras));
        PdfPCell droite = new PdfPCell(new Phrase(valeur, normal));
        gauche.setBorder(0);
        droite.setBorder(0);
        gauche.setPadding(3);
        droite.setPadding(3);
        table.addCell(gauche);
        table.addCell(droite);
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? "—" : valeur;
    }
}
