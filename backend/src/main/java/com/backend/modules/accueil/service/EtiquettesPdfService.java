package com.backend.modules.accueil.service;

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
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Étiquettes d'échantillons, format 70 × 37 mm, référence en code-barres Code 128 lisible par
 * une douchette. Deux mises en page : planche A4 (3 × 8, repères de découpe) ou une étiquette
 * par page (imprimante thermique).
 */
@Service
public class EtiquettesPdfService {

    public enum Format { PLANCHE, UNITAIRE }

    private static final ZoneId FUSEAU = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withLocale(Locale.FRENCH).withZone(FUSEAU);
    private static final float MM = 72f / 25.4f;
    private static final float LARGEUR = 70 * MM;
    private static final float HAUTEUR = 37 * MM;
    private static final int COLONNES = 3;
    private static final int LIGNES = 8;
    private static final Color MARQUE = new Color(27, 78, 82);
    private static final Color MENTHE = new Color(232, 241, 240);
    private static final Color ROUGE = new Color(170, 40, 40);
    private static final Color GRIS = new Color(95, 105, 110);

    public record Etiquette(String reference, String demande, String demandeur, String nature,
                            Instant dateReception, boolean conforme, String analyses, String conservation) {
    }

    public byte[] generer(String laboratoire, List<Etiquette> etiquettes, Format format) {
        return format == Format.UNITAIRE ? unitaires(laboratoire, etiquettes) : planche(laboratoire, etiquettes);
    }

    /** Code-barres seul, en PNG net (agrandi sans lissage), pour l'aperçu à l'écran. */
    public byte[] codeBarresPng(String reference) {
        Barcode128 code = codeBarres(reference, 1f, 40);
        java.awt.Image brut = code.createAwtImage(Color.BLACK, Color.WHITE);
        int facteur = 3;
        int marge = 10 * facteur;
        BufferedImage image = new BufferedImage(brut.getWidth(null) * facteur + 2 * marge,
                brut.getHeight(null) * facteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(brut, marge, 0, brut.getWidth(null) * facteur, brut.getHeight(null) * facteur, null);
        g.dispose();
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("CODE_BARRES_ECHOUE", "Le code-barres n'a pas pu être généré.");
        }
    }

    // ------------------------------------------------------------------ mises en page

    private byte[] planche(String laboratoire, List<Etiquette> etiquettes) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            // 3 × 70 mm = 210 mm ; 8 × 37 mm = 296 mm : marges verticales d'un demi-millimètre.
            Document document = new Document(PageSize.A4, 0, 0, 0.5f * MM, 0.5f * MM);
            PdfWriter writer = PdfWriter.getInstance(document, sortie);
            document.open();
            PdfContentByte contenu = writer.getDirectContent();
            PdfPTable planche = new PdfPTable(COLONNES);
            planche.setTotalWidth(COLONNES * LARGEUR);
            planche.setLockedWidth(true);
            for (Etiquette e : etiquettes) {
                PdfPCell cellule = etiquette(laboratoire, e, contenu);
                // Repère de découpe discret.
                cellule.setBorder(Rectangle.BOX);
                cellule.setBorderColor(new Color(225, 228, 228));
                cellule.setBorderWidth(0.3f);
                planche.addCell(cellule);
            }
            int reste = etiquettes.size() % COLONNES;
            for (int i = 0; reste != 0 && i < COLONNES - reste; i++) {
                PdfPCell vide = new PdfPCell();
                vide.setFixedHeight(HAUTEUR);
                vide.setBorder(Rectangle.NO_BORDER);
                planche.addCell(vide);
            }
            planche.setSplitLate(false);
            planche.setHeaderRows(0);
            document.add(planche);
            document.close();
            return sortie.toByteArray();
        } catch (IOException | DocumentException ex) {
            throw new BusinessRuleException("ETIQUETTES_PDF_ECHOUE", "Les étiquettes n'ont pas pu être générées.");
        }
    }

    private byte[] unitaires(String laboratoire, List<Etiquette> etiquettes) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(new Rectangle(LARGEUR, HAUTEUR), 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(document, sortie);
            document.open();
            PdfContentByte contenu = writer.getDirectContent();
            boolean premiere = true;
            for (Etiquette e : etiquettes) {
                if (!premiere) {
                    document.newPage();
                }
                premiere = false;
                PdfPTable table = new PdfPTable(1);
                table.setTotalWidth(LARGEUR);
                table.setLockedWidth(true);
                PdfPCell cellule = etiquette(laboratoire, e, contenu);
                cellule.setBorder(Rectangle.NO_BORDER);
                cellule.setFixedHeight(HAUTEUR - 0.5f);
                table.addCell(cellule);
                document.add(table);
            }
            document.close();
            return sortie.toByteArray();
        } catch (IOException | DocumentException ex) {
            throw new BusinessRuleException("ETIQUETTES_PDF_ECHOUE", "Les étiquettes n'ont pas pu être générées.");
        }
    }

    // ------------------------------------------------------------------ une étiquette

    /**
     * Bandeau (laboratoire, demande), code-barres, référence en gros caractères, demandeur,
     * nature et date de réception, analyses, puis conformité.
     */
    private PdfPCell etiquette(String laboratoire, Etiquette e, PdfContentByte contenu) {
        Font bandeau = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.WHITE);
        Font bandeauDroite = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, Color.WHITE);
        Font reference = FontFactory.getFont(FontFactory.COURIER_BOLD, 9.5f, Color.BLACK);
        Font nom = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.BLACK);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 6.2f, GRIS);
        Font etat = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6f, e.conforme() ? MARQUE : Color.WHITE);

        PdfPTable interieur = new PdfPTable(1);
        interieur.setWidthPercentage(100);

        // Bandeau : laboratoire à gauche, référence de la demande à droite.
        PdfPTable entete = new PdfPTable(new float[]{1.4f, 1f});
        entete.setWidthPercentage(100);
        PdfPCell gauche = new PdfPCell(new Phrase(tronquer(texte(laboratoire), 28), bandeau));
        PdfPCell droite = new PdfPCell(new Phrase("Dem. " + texte(e.demande()), bandeauDroite));
        for (PdfPCell c : new PdfPCell[]{gauche, droite}) {
            c.setBackgroundColor(MARQUE);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPaddingTop(1.2f);
            c.setPaddingBottom(2.4f);
            c.setPaddingLeft(4);
            c.setPaddingRight(4);
        }
        droite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        entete.addCell(gauche);
        entete.addCell(droite);
        interieur.addCell(sansBord(new PdfPCell(entete), 0));

        // Code-barres (sans texte : la référence est imprimée juste dessous, en gras).
        Image image = codeBarres(e.reference(), 0.9f, 9.5f * MM).createImageWithBarcode(contenu, Color.BLACK, Color.BLACK);
        image.scaleToFit(LARGEUR - 10 * MM, 9.5f * MM);
        PdfPCell code = new PdfPCell(image, false);
        code.setHorizontalAlignment(Element.ALIGN_CENTER);
        code.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sansBord(code, 0);
        code.setPaddingTop(2.2f * MM);
        interieur.addCell(code);
        PdfPCell ref = sansBord(new PdfPCell(new Phrase(e.reference(), reference)), 0);
        ref.setHorizontalAlignment(Element.ALIGN_CENTER);
        ref.setPaddingTop(1);
        interieur.addCell(ref);

        // Demandeur, nature et date, analyses.
        PdfPCell infos = sansBord(new PdfPCell(), 0);
        infos.setPaddingLeft(3 * MM);
        infos.setPaddingRight(3 * MM);
        Paragraph ligneNom = new Paragraph(tronquer(texte(e.demandeur()), 34), nom);
        ligneNom.setLeading(8.5f);
        infos.addElement(ligneNom);
        Paragraph ligneNature = new Paragraph(texte(e.nature())
                + (e.dateReception() == null ? "" : "  ·  " + DATE.format(e.dateReception()))
                + (e.conservation() == null || e.conservation().isBlank() ? "" : "  ·  " + e.conservation()), normal);
        ligneNature.setLeading(7.5f);
        infos.addElement(ligneNature);
        if (e.analyses() != null && !e.analyses().isBlank()) {
            Paragraph analyses = new Paragraph("Analyses : " + tronquer(e.analyses(), 48), normal);
            analyses.setLeading(7.5f);
            infos.addElement(analyses);
        }
        interieur.addCell(infos);

        // Pastille de conformité, alignée à droite.
        Chunk pastille = new Chunk(e.conforme() ? " CONFORME " : " NON CONFORME ", etat);
        pastille.setBackground(e.conforme() ? MENTHE : ROUGE, 1, 1, 1, 1);
        PdfPCell statut = sansBord(new PdfPCell(new Phrase(pastille)), 0);
        statut.setHorizontalAlignment(Element.ALIGN_RIGHT);
        statut.setPaddingRight(3 * MM);
        statut.setPaddingTop(1.5f);
        interieur.addCell(statut);

        PdfPCell cellule = new PdfPCell(interieur);
        cellule.setFixedHeight(HAUTEUR);
        cellule.setPadding(0);
        return cellule;
    }

    private static Barcode128 codeBarres(String reference, float module, float hauteur) {
        Barcode128 code = new Barcode128();
        code.setCodeType(Barcode128.CODE128);
        code.setCode(reference);
        code.setX(module);
        code.setBarHeight(hauteur);
        code.setFont(null);
        return code;
    }

    private static PdfPCell sansBord(PdfPCell cellule, float padding) {
        cellule.setBorder(Rectangle.NO_BORDER);
        cellule.setPadding(padding);
        return cellule;
    }

    private static String tronquer(String valeur, int max) {
        return valeur.length() > max ? valeur.substring(0, max - 1) + "…" : valeur;
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? "—" : valeur;
    }
}
