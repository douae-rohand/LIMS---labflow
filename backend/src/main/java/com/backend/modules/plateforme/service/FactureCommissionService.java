package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantDataSourceRegistry;
import com.backend.modules.facturation.service.MontantEnLettres;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.Identite;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Facture mensuelle de commission émise par la plateforme à un laboratoire : une ligne par facture
 * de demande émise dans le mois (hors factures annulées par avoir), au taux figé sur chaque facture.
 * Générée à la demande (numéro déterministe COM-AAAAMM-CODE), jamais stockée.
 */
@Service
@RequiredArgsConstructor
public class FactureCommissionService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final Color MARQUE = new Color(27, 78, 82);
    private static final Color GRIS = new Color(90, 100, 104);
    private static final Color FOND = new Color(232, 241, 240);

    private final LaboratoireRepository laboratoireRepository;
    private final TenantDataSourceRegistry registry;
    private final ParametrePlateformeService parametrePlateformeService;

    public record LigneCommission(String facture, LocalDate date, String statut, BigDecimal montant,
                                  BigDecimal taux, BigDecimal commission) {
    }

    public record FactureCommission(String numero, Long laboratoireId, String laboratoire, YearMonth mois,
                                    LocalDate dateEmission, LocalDate dateEcheance, int factures,
                                    BigDecimal base, BigDecimal commission, List<LigneCommission> lignes) {
    }

    /** @param mois mois facturé (défaut : le mois précédent) */
    public FactureCommission calculer(Long laboratoireId, YearMonth mois) {
        Laboratoire labo = laboratoire(laboratoireId);
        YearMonth periode = mois == null ? YearMonth.now(ZONE).minusMonths(1) : mois;
        if (periode.isAfter(YearMonth.now(ZONE))) {
            throw new BusinessRuleException("MOIS_FUTUR", "Choisissez un mois écoulé ou le mois en cours.");
        }
        BigDecimal tauxCourant = parametrePlateformeService.tauxCommission();
        String sql = "SELECT numero, date_facture, statut, montant_ht, COALESCE(taux_commission, ?) taux FROM `"
                + labo.getNomSchema() + "`.facture WHERE statut <> 'ANNULEE' AND date_facture >= ? AND date_facture < ?"
                + " ORDER BY date_facture, numero";
        List<LigneCommission> lignes = new ArrayList<>();
        try (Connection connexion = registry.getCentral().getConnection();
             PreparedStatement requete = connexion.prepareStatement(sql)) {
            requete.setBigDecimal(1, tauxCourant);
            requete.setObject(2, periode.atDay(1));
            requete.setObject(3, periode.plusMonths(1).atDay(1));
            try (ResultSet rs = requete.executeQuery()) {
                while (rs.next()) {
                    BigDecimal montant = rs.getBigDecimal("montant_ht") == null ? BigDecimal.ZERO : rs.getBigDecimal("montant_ht");
                    BigDecimal taux = rs.getBigDecimal("taux");
                    java.sql.Date date = rs.getDate("date_facture");
                    lignes.add(new LigneCommission(rs.getString("numero"), date == null ? null : date.toLocalDate(),
                            rs.getString("statut"), montant, taux, ParametrePlateformeService.commissionSur(montant, taux)));
                }
            }
        } catch (SQLException ex) {
            throw new BusinessRuleException("COMMISSION_ILLISIBLE", "Les factures du laboratoire sont illisibles.");
        }
        BigDecimal base = lignes.stream().map(LigneCommission::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = lignes.stream().map(LigneCommission::commission).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate emission = periode.plusMonths(1).atDay(1);
        String code = labo.getCode() == null ? String.valueOf(labo.getId()) : labo.getCode().toUpperCase(Locale.ROOT);
        return new FactureCommission("COM-" + periode.format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + code,
                labo.getId(), labo.getRaisonSociale(), periode, emission, emission.plusDays(30), lignes.size(),
                base, commission, lignes);
    }

    public FichierGenere pdf(Long laboratoireId, YearMonth mois) {
        FactureCommission f = calculer(laboratoireId, mois);
        Laboratoire labo = laboratoire(laboratoireId);
        Identite plateforme = parametrePlateformeService.identite();
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, sortie);
            document.open();
            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, MARQUE);
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, MARQUE);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font gras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font discret = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);

            PdfPTable entete = new PdfPTable(2);
            entete.setWidthPercentage(100);
            PdfPCell emetteur = new PdfPCell();
            emetteur.setBorder(Rectangle.NO_BORDER);
            emetteur.addElement(new Paragraph(texte(plateforme.nom()), titre));
            if (rempli(plateforme.adresse())) emetteur.addElement(new Paragraph(plateforme.adresse(), discret));
            String contact = String.join(" · ", Stream.of(plateforme.telephone(), plateforme.emailContact())
                    .filter(FactureCommissionService::rempli).toList());
            if (!contact.isBlank()) emetteur.addElement(new Paragraph(contact, discret));
            entete.addCell(emetteur);
            PdfPCell destinataire = new PdfPCell();
            destinataire.setBorder(Rectangle.BOX);
            destinataire.setBorderColor(new Color(210, 220, 220));
            destinataire.setPadding(8);
            destinataire.addElement(new Paragraph("Facturé à", discret));
            destinataire.addElement(new Paragraph(texte(labo.getRaisonSociale()), gras));
            if (rempli(labo.getAdresse())) destinataire.addElement(new Paragraph(labo.getAdresse()
                    + (rempli(labo.getVille()) ? ", " + labo.getVille() : ""), normal));
            if (rempli(labo.getIce())) destinataire.addElement(new Paragraph("ICE : " + labo.getIce(), normal));
            entete.addCell(destinataire);
            document.add(entete);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Facture de commission n° " + f.numero(), sousTitre));
            document.add(new Paragraph("Période : " + MOIS.format(f.mois()) + " · émise le " + JOUR.format(f.dateEmission())
                    + " · échéance " + JOUR.format(f.dateEcheance()), discret));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{1.6f, 1f, 1f, 1.3f, 0.7f, 1.3f});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String colonne : new String[]{"Facture de demande", "Date", "Statut", "Montant", "Taux", "Commission"}) {
                PdfPCell c = new PdfPCell(new Phrase(colonne, gras));
                c.setBackgroundColor(FOND);
                c.setPadding(5);
                table.addCell(c);
            }
            for (LigneCommission l : f.lignes()) {
                cellule(table, l.facture(), normal, Element.ALIGN_LEFT);
                cellule(table, l.date() == null ? "—" : JOUR.format(l.date()), normal, Element.ALIGN_LEFT);
                cellule(table, statut(l.statut()), normal, Element.ALIGN_LEFT);
                cellule(table, mad(l.montant()), normal, Element.ALIGN_RIGHT);
                cellule(table, l.taux().stripTrailingZeros().toPlainString() + " %", normal, Element.ALIGN_RIGHT);
                cellule(table, mad(l.commission()), normal, Element.ALIGN_RIGHT);
            }
            if (f.lignes().isEmpty()) {
                PdfPCell c = new PdfPCell(new Phrase("Aucune facture émise par le laboratoire ce mois-ci.", discret));
                c.setColspan(6);
                c.setPadding(6);
                table.addCell(c);
            }
            document.add(table);
            document.add(Chunk.NEWLINE);

            PdfPTable totaux = new PdfPTable(new float[]{2f, 1.3f});
            totaux.setWidthPercentage(45);
            totaux.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellule(totaux, "Montant facturé par le laboratoire", normal, Element.ALIGN_LEFT);
            cellule(totaux, mad(f.base()), normal, Element.ALIGN_RIGHT);
            PdfPCell libelle = new PdfPCell(new Phrase("Commission due", gras));
            libelle.setBackgroundColor(FOND);
            libelle.setPadding(6);
            totaux.addCell(libelle);
            PdfPCell valeur = new PdfPCell(new Phrase(mad(f.commission()), gras));
            valeur.setBackgroundColor(FOND);
            valeur.setPadding(6);
            valeur.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totaux.addCell(valeur);
            document.add(totaux);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Arrêtée la présente facture à la somme de : "
                    + MontantEnLettres.dirhams(f.commission()) + ".", normal));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Commission calculée au taux figé sur chaque facture de demande ; "
                    + "les factures annulées par avoir sont exclues.", discret));
            document.close();
            return new FichierGenere(f.numero() + ".pdf", "application/pdf", sortie.toByteArray());
        } catch (IOException | DocumentException ex) {
            throw new BusinessRuleException("COMMISSION_PDF_ECHOUE", "La facture de commission n'a pas pu être générée.");
        }
    }

    private Laboratoire laboratoire(Long id) {
        Laboratoire labo = laboratoireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", id));
        if (labo.getNomSchema() == null || !labo.getNomSchema().matches("lims_[a-z0-9_]+")) {
            throw new BusinessRuleException("SCHEMA_INVALIDE", "Le schéma du laboratoire est invalide.");
        }
        return labo;
    }

    private static void cellule(PdfPTable table, String valeur, Font police, int alignement) {
        PdfPCell c = new PdfPCell(new Phrase(valeur == null ? "" : valeur, police));
        c.setPadding(5);
        c.setHorizontalAlignment(alignement);
        table.addCell(c);
    }

    private static String statut(String statut) {
        return switch (statut == null ? "" : statut) {
            case "PAYEE" -> "Payée";
            case "EN_RETARD" -> "En retard";
            case "IMPAYEE" -> "Impayée";
            case "EMISE" -> "Émise";
            default -> statut == null ? "—" : statut;
        };
    }

    private static String mad(BigDecimal valeur) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(valeur == null ? BigDecimal.ZERO : valeur) + " MAD";
    }

    private static boolean rempli(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    private static String texte(String valeur) {
        return rempli(valeur) ? valeur : "—";
    }
}
