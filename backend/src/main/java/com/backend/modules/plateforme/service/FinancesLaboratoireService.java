package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantDataSourceRegistry;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Supervision financière d'un laboratoire par le Super Administrateur : la commission que la
 * plateforme prélève sur chaque facture de demande (commission = montant × taux / 100), les factures
 * annulées par avoir étant exclues,
 * sur une période, regroupée par jour, semaine ou mois. Lecture seule, agrégée.
 */
@Service
@RequiredArgsConstructor
public class FinancesLaboratoireService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Casablanca");
    /** Nombre maximal de lignes (jours, semaines ou mois) dans le détail. */
    private static final int MAX_PERIODES = 120;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private final LaboratoireRepository laboratoireRepository;
    private final TenantDataSourceRegistry registry;
    private final ParametrePlateformeService parametrePlateformeService;

    public enum Regroupement { JOUR, SEMAINE, MOIS }

    /** Une ligne du détail : {@code debut} est le premier jour du jour, de la semaine (lundi) ou du mois. */
    public record PeriodeFinance(LocalDate debut, String libelle, long demandesFacturees,
                                 BigDecimal facture, BigDecimal commission, BigDecimal commissionEncaissee) {
    }

    public record FinancesLaboratoire(
            Long laboratoireId,
            String laboratoire,
            LocalDate du,
            LocalDate au,
            Regroupement regroupement,
            BigDecimal tauxCommission,
            long demandesFacturees,
            /** Total TTC des factures de la période (base de calcul). */
            BigDecimal facture,
            /** Commission de la plateforme sur les factures de la période. */
            BigDecimal commission,
            /** Commission sur les paiements reçus pendant la période. */
            BigDecimal commissionEncaissee,
            /** Commission restant à percevoir sur les factures non soldées, à ce jour. */
            BigDecimal commissionAPercevoir,
            /** dont factures dont l'échéance est dépassée, à ce jour. */
            BigDecimal commissionEnRetard,
            List<PeriodeFinance> details) {
    }

    /**
     * @param du           premier jour inclus (défaut : 1er jour du mois courant)
     * @param au           dernier jour inclus (défaut : aujourd'hui)
     * @param regroupement jour, semaine ou mois ; choisi selon la durée s'il est absent
     */
    public FinancesLaboratoire synthese(Long laboratoireId, LocalDate du, LocalDate au, String regroupement) {
        Laboratoire laboratoire = laboratoireRepository.findById(laboratoireId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratoire", "id", laboratoireId));
        String schema = laboratoire.getNomSchema();
        if (schema == null || !schema.matches("lims_[a-z0-9_]+")) {
            throw new BusinessRuleException("SCHEMA_INVALIDE", "Le schéma du laboratoire est invalide.");
        }
        LocalDate aujourdHui = LocalDate.now(ZONE);
        LocalDate fin = au != null ? au : aujourdHui;
        LocalDate debut = du != null ? du : fin.withDayOfMonth(1);
        if (debut.isAfter(fin)) {
            throw new BusinessRuleException("PERIODE_INVALIDE", "La date de début doit précéder la date de fin.");
        }
        Regroupement groupe = regroupement(regroupement, debut, fin);
        List<LocalDate> cles = cles(debut, fin, groupe);
        if (cles.size() > MAX_PERIODES) {
            throw new BusinessRuleException("PERIODE_TROP_LONGUE", "Période trop longue pour un regroupement par "
                    + groupe.name().toLowerCase(Locale.ROOT) + " : choisissez un regroupement plus large.");
        }

        BigDecimal tauxCourant = parametrePlateformeService.tauxCommission();
        String f = "`" + schema + "`.facture";
        String p = "`" + schema + "`.paiement";
        LocalDate lendemain = fin.plusDays(1);

        try (Connection connexion = registry.getCentral().getConnection()) {
            Map<String, Object> periode = ligne(connexion, "SELECT COUNT(*) nb, COALESCE(SUM(montant_ht), 0) facture,"
                    + " COALESCE(SUM(" + commission("montant_ht", "taux_commission") + "), 0) commission"
                    + " FROM " + f + " WHERE statut <> 'ANNULEE' AND date_facture >= ? AND date_facture < ?",
                    tauxCourant, debut, lendemain);
            BigDecimal commissionEncaissee = decimal(ligne(connexion, "SELECT COALESCE(SUM(" + commission("pa.montant", "fa.taux_commission")
                    + "), 0) v FROM " + p + " pa JOIN " + f + " fa ON fa.id_facture = pa.facture_id"
                    + " WHERE pa.date_paiement >= ? AND pa.date_paiement < ?",
                    tauxCourant, debut.atStartOfDay(), lendemain.atStartOfDay()).get("v"));

            // État à ce jour, indépendant de la période : commission sur les montants non encore payés.
            String reste = "fa.montant_ht - COALESCE(pa.paye, 0)";
            String nonSoldees = " FROM " + f + " fa LEFT JOIN (SELECT facture_id, SUM(montant) paye FROM " + p
                    + " GROUP BY facture_id) pa ON pa.facture_id = fa.id_facture WHERE fa.statut NOT IN ('PAYEE', 'ANNULEE')";
            BigDecimal commissionAPercevoir = decimal(ligne(connexion, "SELECT COALESCE(SUM(" + commission(reste, "fa.taux_commission")
                    + "), 0) v" + nonSoldees, tauxCourant).get("v"));
            BigDecimal commissionEnRetard = decimal(ligne(connexion, "SELECT COALESCE(SUM(" + commission(reste, "fa.taux_commission")
                    + "), 0) v" + nonSoldees + " AND fa.date_echeance < CURDATE()", tauxCourant).get("v"));

            return new FinancesLaboratoire(
                    laboratoire.getId(),
                    laboratoire.getRaisonSociale(),
                    debut,
                    fin,
                    groupe,
                    tauxCourant,
                    ((Number) periode.get("nb")).longValue(),
                    arrondi(decimal(periode.get("facture"))),
                    arrondi(decimal(periode.get("commission"))),
                    arrondi(commissionEncaissee),
                    arrondi(commissionAPercevoir.max(BigDecimal.ZERO)),
                    arrondi(commissionEnRetard.max(BigDecimal.ZERO)),
                    details(connexion, f, p, cles, groupe, debut, lendemain, tauxCourant));
        } catch (SQLException ex) {
            throw new BusinessRuleException("FINANCES_INDISPONIBLES",
                    "Les données financières du laboratoire sont indisponibles.");
        }
    }

    /** Regroupement demandé, sinon choisi selon la durée : jour (≤ 31 j), semaine (≤ 6 mois), mois. */
    private static Regroupement regroupement(String demande, LocalDate debut, LocalDate fin) {
        if (demande != null && !demande.isBlank()) {
            try {
                return Regroupement.valueOf(demande.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException("REGROUPEMENT_INVALIDE", "Regroupement attendu : jour, semaine ou mois.");
            }
        }
        long jours = ChronoUnit.DAYS.between(debut, fin) + 1;
        return jours <= 31 ? Regroupement.JOUR : jours <= 186 ? Regroupement.SEMAINE : Regroupement.MOIS;
    }

    private static LocalDate cle(LocalDate date, Regroupement groupe) {
        return switch (groupe) {
            case JOUR -> date;
            case SEMAINE -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MOIS -> date.withDayOfMonth(1);
        };
    }

    private static List<LocalDate> cles(LocalDate debut, LocalDate fin, Regroupement groupe) {
        List<LocalDate> cles = new ArrayList<>();
        LocalDate courant = cle(debut, groupe);
        while (!courant.isAfter(fin) && cles.size() <= MAX_PERIODES) {
            cles.add(courant);
            courant = switch (groupe) {
                case JOUR -> courant.plusDays(1);
                case SEMAINE -> courant.plusWeeks(1);
                case MOIS -> courant.plusMonths(1);
            };
        }
        return cles;
    }

    /** Commission sur un montant : le taux figé sur la facture prime, sinon le taux courant (un paramètre « ? »). */
    private static String commission(String montant, String colonneTaux) {
        return "(" + montant + ") * COALESCE(" + colonneTaux + ", ?) / 100";
    }

    private static String expressionCle(String colonne, Regroupement groupe) {
        return switch (groupe) {
            case JOUR -> "DATE(" + colonne + ")";
            case SEMAINE -> "DATE_SUB(DATE(" + colonne + "), INTERVAL WEEKDAY(" + colonne + ") DAY)";
            case MOIS -> "DATE_FORMAT(" + colonne + ", '%Y-%m-01')";
        };
    }

    private List<PeriodeFinance> details(Connection connexion, String f, String p, List<LocalDate> cles,
                                         Regroupement groupe, LocalDate debut, LocalDate lendemain,
                                         BigDecimal tauxCourant) throws SQLException {
        Map<LocalDate, long[]> nombres = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal[]> montants = new LinkedHashMap<>();
        for (LocalDate cle : cles) {
            nombres.put(cle, new long[1]);
            montants.put(cle, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }
        try (PreparedStatement requete = connexion.prepareStatement("SELECT " + expressionCle("date_facture", groupe)
                + " k, COUNT(*) nb, SUM(montant_ht) total, SUM(" + commission("montant_ht", "taux_commission") + ") commission FROM " + f
                + " WHERE statut <> 'ANNULEE' AND date_facture >= ? AND date_facture < ? GROUP BY k")) {
            requete.setObject(1, tauxCourant);
            requete.setObject(2, debut);
            requete.setObject(3, lendemain);
            try (ResultSet rs = requete.executeQuery()) {
                while (rs.next()) {
                    LocalDate cle = LocalDate.parse(rs.getString("k").substring(0, 10));
                    if (nombres.containsKey(cle)) {
                        nombres.get(cle)[0] = rs.getLong("nb");
                        montants.get(cle)[0] = decimal(rs.getBigDecimal("total"));
                        montants.get(cle)[1] = decimal(rs.getBigDecimal("commission"));
                    }
                }
            }
        }
        try (PreparedStatement requete = connexion.prepareStatement("SELECT " + expressionCle("pa.date_paiement", groupe)
                + " k, SUM(" + commission("pa.montant", "fa.taux_commission") + ") commission FROM " + p + " pa JOIN " + f
                + " fa ON fa.id_facture = pa.facture_id WHERE pa.date_paiement >= ? AND pa.date_paiement < ? GROUP BY k")) {
            requete.setObject(1, tauxCourant);
            requete.setObject(2, debut.atStartOfDay());
            requete.setObject(3, lendemain.atStartOfDay());
            try (ResultSet rs = requete.executeQuery()) {
                while (rs.next()) {
                    LocalDate cle = LocalDate.parse(rs.getString("k").substring(0, 10));
                    if (montants.containsKey(cle)) {
                        montants.get(cle)[2] = decimal(rs.getBigDecimal("commission"));
                    }
                }
            }
        }
        List<PeriodeFinance> resultat = new ArrayList<>();
        nombres.forEach((cle, nb) -> resultat.add(new PeriodeFinance(cle, libelle(cle, groupe), nb[0],
                arrondi(montants.get(cle)[0]), arrondi(montants.get(cle)[1]), arrondi(montants.get(cle)[2]))));
        return resultat;
    }

    private static String libelle(LocalDate cle, Regroupement groupe) {
        return switch (groupe) {
            case JOUR -> cle.format(JOUR);
            case SEMAINE -> "Semaine du " + cle.format(JOUR);
            case MOIS -> {
                String mois = YearMonth.from(cle).format(MOIS);
                yield Character.toUpperCase(mois.charAt(0)) + mois.substring(1);
            }
        };
    }

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    public FichierGenere exporter(Long laboratoireId, LocalDate du, LocalDate au, String regroupement, String format) {
        FinancesLaboratoire finances = synthese(laboratoireId, du, au, regroupement);
        String base = "finances-" + laboratoireRepository.findById(laboratoireId).map(Laboratoire::getCode).orElse("labo")
                + "-" + finances.du() + "_" + finances.au();
        String choix = format == null ? "" : format.trim().toLowerCase(Locale.ROOT);
        return switch (choix) {
            case "csv" -> new FichierGenere(base + ".csv", "text/csv; charset=UTF-8", csv(finances));
            case "xlsx", "excel" -> new FichierGenere(base + ".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel(finances));
            case "pdf" -> new FichierGenere(base + ".pdf", "application/pdf", pdf(finances));
            default -> throw new BusinessRuleException("FORMAT_EXPORT_INVALIDE",
                    "Le format d'exportation doit être PDF, Excel ou CSV");
        };
    }

    private static final List<String> COLONNES = List.of(
            "Période", "Demandes facturées", "Facturé (MAD)", "Commission (MAD)", "Commission encaissée (MAD)");

    private static String intitulePeriode(FinancesLaboratoire finances) {
        return "Du " + finances.du().format(JOUR) + " au " + finances.au().format(JOUR);
    }

    private static List<List<String>> lignes(FinancesLaboratoire finances) {
        List<List<String>> lignes = new ArrayList<>();
        for (PeriodeFinance d : finances.details()) {
            lignes.add(List.of(d.libelle(), String.valueOf(d.demandesFacturees()),
                    d.facture().toPlainString(), d.commission().toPlainString(), d.commissionEncaissee().toPlainString()));
        }
        lignes.add(List.of("Total", String.valueOf(finances.demandesFacturees()),
                finances.facture().toPlainString(), finances.commission().toPlainString(),
                finances.commissionEncaissee().toPlainString()));
        return lignes;
    }

    private static List<List<String>> resume(FinancesLaboratoire finances) {
        return List.of(
                List.of("Commission sur la période", finances.commission().toPlainString()),
                List.of("Commission encaissée sur la période", finances.commissionEncaissee().toPlainString()),
                List.of("Montant facturé (base)", finances.facture().toPlainString()),
                List.of("Commission à percevoir (à ce jour)", finances.commissionAPercevoir().toPlainString()),
                List.of("dont en retard (à ce jour)", finances.commissionEnRetard().toPlainString()));
    }

    private static byte[] csv(FinancesLaboratoire finances) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("Laboratoire;").append(finances.laboratoire().replace(";", ",")).append("\r\n");
        sb.append("Période;").append(intitulePeriode(finances)).append("\r\n\r\n");
        for (List<String> l : resume(finances)) {
            sb.append(String.join(";", l)).append("\r\n");
        }
        sb.append("\r\n").append(String.join(";", COLONNES)).append("\r\n");
        for (List<String> l : lignes(finances)) {
            sb.append(String.join(";", l)).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] excel(FinancesLaboratoire finances) {
        try (XSSFWorkbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = classeur.createSheet("Finances");
            int index = 0;
            feuille.createRow(index++).createCell(0).setCellValue("Laboratoire : " + finances.laboratoire());
            feuille.createRow(index++).createCell(0).setCellValue(intitulePeriode(finances));
            index++;
            for (List<String> l : resume(finances)) {
                Row ligne = feuille.createRow(index++);
                ligne.createCell(0).setCellValue(l.get(0));
                ligne.createCell(1).setCellValue(Double.parseDouble(l.get(1)));
            }
            index++;
            Row entete = feuille.createRow(index++);
            for (int i = 0; i < COLONNES.size(); i++) {
                entete.createCell(i).setCellValue(COLONNES.get(i));
            }
            for (List<String> l : lignes(finances)) {
                Row ligne = feuille.createRow(index++);
                ligne.createCell(0).setCellValue(l.get(0));
                for (int i = 1; i < l.size(); i++) {
                    ligne.createCell(i).setCellValue(Double.parseDouble(l.get(i)));
                }
            }
            for (int i = 0; i < COLONNES.size(); i++) {
                feuille.autoSizeColumn(i);
            }
            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("EXPORT_EXCEL_ECHOUE", "La création du fichier Excel a échoué");
        }
    }

    private static byte[] pdf(FinancesLaboratoire finances) {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, sortie);
            document.open();
            Color marque = new Color(27, 78, 82);
            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, marque);
            Font texte = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(36, 42, 46));
            Font gras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(36, 42, 46));
            Font entete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            document.add(new Paragraph("Commission plateforme — " + finances.laboratoire(), titre));
            document.add(new Paragraph(intitulePeriode(finances) + " · montants en MAD · taux actuel "
                    + finances.tauxCommission().stripTrailingZeros().toPlainString() + " %", texte));
            document.add(new Paragraph(" "));

            PdfPTable resume = new PdfPTable(2);
            resume.setWidthPercentage(60);
            resume.setHorizontalAlignment(Element.ALIGN_LEFT);
            for (List<String> l : resume(finances)) {
                PdfPCell libelle = new PdfPCell(new Phrase(l.get(0), texte));
                libelle.setPadding(4);
                PdfPCell valeur = new PdfPCell(new Phrase(l.get(1), gras));
                valeur.setPadding(4);
                valeur.setHorizontalAlignment(Element.ALIGN_RIGHT);
                resume.addCell(libelle);
                resume.addCell(valeur);
            }
            document.add(resume);
            document.add(new Paragraph(" "));

            PdfPTable tableau = new PdfPTable(COLONNES.size());
            tableau.setWidthPercentage(100);
            tableau.setHeaderRows(1);
            for (String colonne : COLONNES) {
                PdfPCell cell = new PdfPCell(new Phrase(colonne, entete));
                cell.setBackgroundColor(marque);
                cell.setPadding(5);
                tableau.addCell(cell);
            }
            List<List<String>> lignes = lignes(finances);
            for (int n = 0; n < lignes.size(); n++) {
                List<String> l = lignes.get(n);
                boolean total = n == lignes.size() - 1;
                for (int i = 0; i < l.size(); i++) {
                    PdfPCell cell = new PdfPCell(new Phrase(l.get(i), total ? gras : texte));
                    cell.setPadding(4);
                    if (i > 0) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
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

    // -------------------------------------------------------------------------

    private static Map<String, Object> ligne(Connection connexion, String sql, Object... parametres) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            for (int i = 0; i < parametres.length; i++) {
                requete.setObject(i + 1, parametres[i]);
            }
            try (ResultSet rs = requete.executeQuery()) {
                Map<String, Object> valeurs = new LinkedHashMap<>();
                if (rs.next()) {
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        valeurs.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                    }
                }
                return valeurs;
            }
        }
    }

    private static BigDecimal decimal(Object valeur) {
        if (valeur instanceof BigDecimal d) {
            return d;
        }
        if (valeur instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal arrondi(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }
}
