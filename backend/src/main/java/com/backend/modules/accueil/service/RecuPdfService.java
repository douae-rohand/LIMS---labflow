package com.backend.modules.accueil.service;

import com.backend.common.exception.BusinessRuleException;
import com.lowagie.text.Chunk;
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
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Reçu de paiement remis au client à la caisse (OpenPDF, format A5). */
@Service
public class RecuPdfService {

    private static final ZoneId FUSEAU = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm")
            .withLocale(Locale.FRENCH).withZone(FUSEAU);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final Color MARQUE = new Color(27, 78, 82);
    private static final Color GRIS = new Color(90, 100, 104);

    public record EnTete(String nom, String adresse, String contact, String ice) {
    }

    public record DonneesRecu(
            EnTete laboratoire,
            String numeroRecu,
            Instant datePaiement,
            String client,
            String clientIce,
            String numeroFacture,
            LocalDate dateFacture,
            String demandeReference,
            BigDecimal montantFacture,
            BigDecimal payeAvant,
            BigDecimal montant,
            String mode,
            String referencePaiement,
            BigDecimal reste,
            String encaisseur) {
    }

    public byte[] generer(DonneesRecu d) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5, 32, 32, 32, 32);
            PdfWriter.getInstance(document, sortie);
            document.open();
            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, MARQUE);
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, MARQUE);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font gras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font discret = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
            Font montant = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, MARQUE);

            EnTete labo = d.laboratoire();
            document.add(new Paragraph(texte(labo.nom()), titre));
            if (labo.adresse() != null) document.add(new Paragraph(labo.adresse(), discret));
            if (labo.contact() != null) document.add(new Paragraph(labo.contact(), discret));
            if (labo.ice() != null) document.add(new Paragraph("ICE : " + labo.ice(), discret));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Reçu de paiement n° " + d.numeroRecu(), sousTitre));
            document.add(new Paragraph("Émis le " + HORODATAGE.format(d.datePaiement()), discret));
            document.add(Chunk.NEWLINE);

            PdfPTable infos = new PdfPTable(new float[]{1.3f, 2.7f});
            infos.setWidthPercentage(100);
            ligne(infos, "Client", texte(d.client()), gras, normal);
            if (d.clientIce() != null) ligne(infos, "ICE client", d.clientIce(), gras, normal);
            ligne(infos, "Facture", d.numeroFacture()
                    + (d.dateFacture() == null ? "" : " du " + JOUR.format(d.dateFacture())), gras, normal);
            ligne(infos, "Demande", texte(d.demandeReference()), gras, normal);
            ligne(infos, "Mode", libelleMode(d.mode())
                    + (d.referencePaiement() == null ? "" : " — réf. " + d.referencePaiement()), gras, normal);
            document.add(infos);
            document.add(Chunk.NEWLINE);

            PdfPTable montants = new PdfPTable(new float[]{2.6f, 1.4f});
            montants.setWidthPercentage(100);
            montant(montants, "Montant de la facture", d.montantFacture(), normal);
            montant(montants, "Déjà réglé", d.payeAvant(), normal);
            PdfPCell libelle = new PdfPCell(new Phrase("Montant reçu", gras));
            libelle.setBackgroundColor(new Color(232, 241, 240));
            libelle.setPadding(6);
            montants.addCell(libelle);
            PdfPCell valeur = new PdfPCell(new Phrase(mad(d.montant()), montant));
            valeur.setBackgroundColor(new Color(232, 241, 240));
            valeur.setPadding(6);
            valeur.setHorizontalAlignment(Element.ALIGN_RIGHT);
            montants.addCell(valeur);
            montant(montants, "Reste à payer", d.reste(), gras);
            document.add(montants);
            document.add(Chunk.NEWLINE);

            String solde = d.reste().signum() == 0 ? "Facture intégralement réglée." : "Règlement partiel.";
            document.add(new Paragraph(solde, gras));
            document.add(new Paragraph("Encaissé par " + texte(d.encaisseur()) + ".", discret));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Ce reçu atteste du paiement indiqué ci-dessus. Conservez-le avec votre facture.", discret));
            document.close();
            return sortie.toByteArray();
        } catch (IOException | DocumentException ex) {
            throw new BusinessRuleException("RECU_PDF_ECHOUE", "Le reçu n'a pas pu être généré.");
        }
    }

    /** Journal de caisse d'un jour (A4 paysage) : écritures, totaux par mode, total net. */
    public byte[] journal(EnTete labo, com.backend.modules.accueil.dto.CaisseDtos.JournalCaisse j, String editePar) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(document, sortie);
            document.open();
            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, MARQUE);
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, MARQUE);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Font gras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
            Font rouge = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(170, 40, 40));
            Font discret = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);

            document.add(new Paragraph(texte(labo.nom()), titre));
            if (labo.adresse() != null) document.add(new Paragraph(labo.adresse(), discret));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Journal de caisse du " + JOUR.format(j.jour()), sousTitre));
            document.add(new Paragraph("Édité le " + HORODATAGE.format(Instant.now()) + " par " + texte(editePar), discret));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{0.7f, 1.3f, 1.3f, 2.2f, 1f, 1.1f, 1.5f, 2.2f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String entete : new String[]{"Heure", "Reçu", "Facture", "Client", "Mode", "Montant", "Encaissé par", "Observation"}) {
                PdfPCell cellule = new PdfPCell(new Phrase(entete, gras));
                cellule.setBackgroundColor(new Color(232, 241, 240));
                cellule.setPadding(4);
                table.addCell(cellule);
            }
            DateTimeFormatter heure = DateTimeFormatter.ofPattern("HH:mm").withZone(FUSEAU);
            for (var l : j.lignes()) {
                Font police = l.contrePassation() ? rouge : normal;
                cellule(table, heure.format(l.date()), police, Element.ALIGN_LEFT);
                cellule(table, l.code(), police, Element.ALIGN_LEFT);
                cellule(table, texte(l.facture()), police, Element.ALIGN_LEFT);
                cellule(table, texte(l.client()), police, Element.ALIGN_LEFT);
                cellule(table, libelleMode(l.mode()), police, Element.ALIGN_LEFT);
                cellule(table, mad(l.montant()), police, Element.ALIGN_RIGHT);
                cellule(table, texte(l.encaisseur()), police, Element.ALIGN_LEFT);
                cellule(table, l.contrePassation() ? texte(l.reference()) : (l.reference() == null ? "" : "Réf. " + l.reference()),
                        police, Element.ALIGN_LEFT);
            }
            if (j.lignes().isEmpty()) {
                PdfPCell vide = new PdfPCell(new Phrase("Aucun encaissement ce jour.", discret));
                vide.setColspan(8);
                vide.setPadding(6);
                table.addCell(vide);
            }
            document.add(table);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Totaux par mode de paiement", sousTitre));
            document.add(Chunk.NEWLINE);
            PdfPTable totaux = new PdfPTable(new float[]{2f, 1f, 1.4f});
            totaux.setWidthPercentage(45);
            totaux.setHorizontalAlignment(Element.ALIGN_LEFT);
            for (String entete : new String[]{"Mode", "Opérations", "Montant net"}) {
                PdfPCell cellule = new PdfPCell(new Phrase(entete, gras));
                cellule.setBackgroundColor(new Color(232, 241, 240));
                cellule.setPadding(4);
                totaux.addCell(cellule);
            }
            for (var t : j.totaux()) {
                cellule(totaux, libelleMode(t.mode()), normal, Element.ALIGN_LEFT);
                cellule(totaux, String.valueOf(t.operations()), normal, Element.ALIGN_RIGHT);
                cellule(totaux, mad(t.montant()), normal, Element.ALIGN_RIGHT);
            }
            cellule(totaux, "Total encaissé", gras, Element.ALIGN_LEFT);
            cellule(totaux, "", gras, Element.ALIGN_RIGHT);
            cellule(totaux, mad(j.total()), gras, Element.ALIGN_RIGHT);
            document.add(totaux);
            if (j.annulations() > 0) {
                document.add(new Paragraph(j.annulations() + " annulation(s) d'encaissement déduite(s) des totaux.", discret));
            }
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Signature de l'agent d'accueil :", discret));
            document.close();
            return sortie.toByteArray();
        } catch (IOException | DocumentException ex) {
            throw new BusinessRuleException("JOURNAL_PDF_ECHOUE", "Le journal de caisse n'a pas pu être généré.");
        }
    }

    private static void cellule(PdfPTable table, String valeur, Font police, int alignement) {
        PdfPCell cellule = new PdfPCell(new Phrase(valeur == null ? "" : valeur, police));
        cellule.setPadding(4);
        cellule.setHorizontalAlignment(alignement);
        table.addCell(cellule);
    }

    static String mad(BigDecimal valeur) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(valeur == null ? BigDecimal.ZERO : valeur) + " MAD";
    }

    static String libelleMode(String mode) {
        return switch (mode == null ? "" : mode) {
            case "ESPECES" -> "Espèces";
            case "CHEQUE" -> "Chèque";
            case "CARTE" -> "Carte bancaire";
            case "VIREMENT" -> "Virement";
            default -> "—";
        };
    }

    private static void ligne(PdfPTable table, String libelle, String valeur, Font gras, Font normal) {
        PdfPCell gauche = new PdfPCell(new Phrase(libelle, gras));
        gauche.setBorder(PdfPCell.NO_BORDER);
        gauche.setPadding(3);
        table.addCell(gauche);
        PdfPCell droite = new PdfPCell(new Phrase(valeur, normal));
        droite.setBorder(PdfPCell.NO_BORDER);
        droite.setPadding(3);
        table.addCell(droite);
    }

    private static void montant(PdfPTable table, String libelle, BigDecimal valeur, Font police) {
        PdfPCell gauche = new PdfPCell(new Phrase(libelle, police));
        gauche.setPadding(6);
        table.addCell(gauche);
        PdfPCell droite = new PdfPCell(new Phrase(mad(valeur), police));
        droite.setPadding(6);
        droite.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(droite);
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? "—" : valeur;
    }
}
