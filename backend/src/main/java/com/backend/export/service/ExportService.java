package com.backend.export.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.audit.dto.AuditEntryDto;
import com.backend.common.audit.service.AuditAdminService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.demande.dto.DemandesStatsDto;
import com.backend.modules.demande.service.DemandesStatsService;
import com.backend.modules.facturation.dto.FactureAdminDto;
import com.backend.modules.facturation.service.FacturationAdminService;
import com.backend.modules.produit.dto.ProduitDto;
import com.backend.modules.produit.service.StockAdminService;
import com.backend.modules.satisfaction.dto.SatisfactionStatsDto;
import com.backend.modules.satisfaction.service.SatisfactionAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final StockAdminService stockAdminService;
    private final FacturationAdminService facturationAdminService;
    private final DemandesStatsService demandesStatsService;
    private final SatisfactionAdminService satisfactionAdminService;
    private final AuditAdminService auditAdminService;
    private final JournalAuditService journalAuditService;

    private static final int MAX_LIGNES = 50000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public byte[] exporter(String jeu, String format) {
        String email = SecurityUtils.principalCourant().getEmail();

        if (format == null) {
            format = "csv";
        }

        if (!format.equals("csv") && !format.equals("xlsx") && !format.equals("pdf")) {
            throw new BusinessRuleException("FORMAT_INVALIDE", "Format d'export non supporté : " + format);
        }

        ExportResult result;
        switch (jeu) {
            case "demandes":
                result = exporterDemandes(format);
                break;
            case "factures":
                result = exporterFactures(format);
                break;
            case "produits":
                result = exporterProduits(format);
                break;
            case "enquetes":
                result = exporterEnquetes(format);
                break;
            case "audit":
                result = exporterAudit(format);
                break;
            case "employes":
                result = exporterEmployes(format);
                break;
            default:
                throw new BusinessRuleException("JEU_INVALIDE", "Jeu d'export non supporté : " + jeu);
        }

        if (result.lignes > MAX_LIGNES) {
            throw new BusinessRuleException("EXPORT_TROP_VOLUMINEUX",
                    "L'export contient " + result.lignes + " lignes (maximum " + MAX_LIGNES + ")");
        }

        journalAuditService.enregistrer("EXPORT_DONNEES", "Export", null,
                email, null, "{\"jeu\": \"" + jeu + "\", \"format\": \"" + format + "\", \"lignes\": " + result.lignes + "}", null);

        return result.donnees;
    }

    private ExportResult exporterDemandes(String format) {
        List<String> entetes = List.of("ID", "Numéro", "Titre", "Statut", "Date soumission", "Client");
        List<List<String>> lignes = new ArrayList<>();

        Page<Object[]> page = demandesStatsService.toutesDemandes(PageRequest.of(0, MAX_LIGNES));
        for (Object[] row : page.getContent()) {
            List<String> ligne = new ArrayList<>();
            ligne.add(String.valueOf(row[0]));
            ligne.add(protegerCellule(String.valueOf(row[1])));
            ligne.add(protegerCellule(String.valueOf(row[2])));
            ligne.add(protegerCellule(String.valueOf(row[3])));
            ligne.add(row[4] != null ? row[4].toString() : "");
            ligne.add(protegerCellule(String.valueOf(row[5])));
            lignes.add(ligne);
        }

        return new ExportResult((int) page.getTotalElements(), genererDonnees(format, entetes, lignes));
    }

    private ExportResult exporterFactures(String format) {
        List<String> entetes = List.of("ID", "Numéro", "Date facture", "Date échéance", "Montant HT", "Statut", "Demande", "Client");
        List<List<String>> lignes = new ArrayList<>();

        Page<FactureAdminDto> page = facturationAdminService.toutesFactures(PageRequest.of(0, MAX_LIGNES));
        for (FactureAdminDto facture : page.getContent()) {
            List<String> ligne = new ArrayList<>();
            ligne.add(String.valueOf(facture.getId()));
            ligne.add(protegerCellule(facture.getNumero()));
            ligne.add(facture.getDateFacture() != null ? facture.getDateFacture().toString() : "");
            ligne.add(facture.getDateEcheance() != null ? facture.getDateEcheance().toString() : "");
            ligne.add(facture.getMontantHt() != null ? facture.getMontantHt().toString() : "");
            ligne.add(protegerCellule(facture.getStatut()));
            ligne.add(protegerCellule(facture.getDemandeReference()));
            ligne.add(protegerCellule(facture.getClientNom()));
            lignes.add(ligne);
        }

        return new ExportResult((int) page.getTotalElements(), genererDonnees(format, entetes, lignes));
    }

    private ExportResult exporterProduits(String format) {
        List<String> entetes = List.of("ID", "Référence", "Nom", "Unité", "Seuil minimal", "Conditions stockage", "Actif", "Stock courant");
        List<List<String>> lignes = new ArrayList<>();

        Page<ProduitDto> page = stockAdminService.tousProduits(PageRequest.of(0, MAX_LIGNES));
        for (ProduitDto produit : page.getContent()) {
            List<String> ligne = new ArrayList<>();
            ligne.add(String.valueOf(produit.getId()));
            ligne.add(protegerCellule(produit.getReference()));
            ligne.add(protegerCellule(produit.getNom()));
            ligne.add(protegerCellule(produit.getUnite()));
            ligne.add(produit.getSeuilMinimal() != null ? produit.getSeuilMinimal().toString() : "");
            ligne.add(protegerCellule(produit.getConditionsStockage()));
            ligne.add(produit.getActif() != null ? produit.getActif().toString() : "");
            ligne.add(produit.getStockCourant() != null ? produit.getStockCourant().toString() : "");
            lignes.add(ligne);
        }

        return new ExportResult((int) page.getTotalElements(), genererDonnees(format, entetes, lignes));
    }

    private ExportResult exporterEnquetes(String format) {
        List<String> entetes = List.of("ID", "Code", "Date envoi", "Date réponse", "Note globale", "Note délai", "Note clarté", "Note relation", "Commentaire");
        List<List<String>> lignes = new ArrayList<>();

        Page<Object[]> page = satisfactionAdminService.toutesEnquetesRepondues(PageRequest.of(0, MAX_LIGNES));
        for (Object[] row : page.getContent()) {
            List<String> ligne = new ArrayList<>();
            ligne.add(String.valueOf(row[0]));
            ligne.add(protegerCellule(String.valueOf(row[1])));
            ligne.add(row[2] != null ? row[2].toString() : "");
            ligne.add(row[3] != null ? row[3].toString() : "");
            ligne.add(row[4] != null ? row[4].toString() : "");
            ligne.add(row[5] != null ? row[5].toString() : "");
            ligne.add(row[6] != null ? row[6].toString() : "");
            ligne.add(row[7] != null ? row[7].toString() : "");
            ligne.add(protegerCellule(String.valueOf(row[8])));
            lignes.add(ligne);
        }

        return new ExportResult((int) page.getTotalElements(), genererDonnees(format, entetes, lignes));
    }

    private ExportResult exporterAudit(String format) {
        List<String> entetes = List.of("Code", "Date action", "Action", "Objet", "Valeur avant", "Valeur après", "Motif", "Acteur", "Rôle");
        List<List<String>> lignes = new ArrayList<>();

        Page<AuditEntryDto> page = auditAdminService.toutesEntries(PageRequest.of(0, MAX_LIGNES));
        for (AuditEntryDto entry : page.getContent()) {
            List<String> ligne = new ArrayList<>();
            ligne.add(protegerCellule(entry.getCode()));
            ligne.add(entry.getDateAction() != null ? entry.getDateAction().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().format(DATE_FORMATTER) : "");
            ligne.add(protegerCellule(entry.getAction()));
            ligne.add(protegerCellule(entry.getObjet()));
            ligne.add(protegerCellule(entry.getValeurAvant()));
            ligne.add(protegerCellule(entry.getValeurApres()));
            ligne.add(protegerCellule(entry.getMotif()));
            ligne.add(protegerCellule(entry.getActeurNom()));
            ligne.add(protegerCellule(entry.getActeurRole()));
            lignes.add(ligne);
        }

        return new ExportResult((int) page.getTotalElements(), genererDonnees(format, entetes, lignes));
    }

    private ExportResult exporterEmployes(String format) {
        List<String> entetes = List.of("ID", "Nom", "Prénom", "Email", "Rôle", "Laboratoire");
        List<List<String>> lignes = new ArrayList<>();

        Page<Object[]> page = stockAdminService.tousEmployes(PageRequest.of(0, MAX_LIGNES));
        for (Object[] row : page.getContent()) {
            List<String> ligne = new ArrayList<>();
            ligne.add(String.valueOf(row[0]));
            ligne.add(protegerCellule(String.valueOf(row[1])));
            ligne.add(protegerCellule(String.valueOf(row[2])));
            ligne.add(protegerCellule(String.valueOf(row[3])));
            ligne.add(protegerCellule(String.valueOf(row[4])));
            ligne.add(protegerCellule(String.valueOf(row[5])));
            lignes.add(ligne);
        }

        return new ExportResult((int) page.getTotalElements(), genererDonnees(format, entetes, lignes));
    }

    private byte[] genererDonnees(String format, List<String> entetes, List<List<String>> lignes) {
        try {
            switch (format.toLowerCase()) {
                case "xlsx":
                    return creerXlsx(entetes, lignes);
                case "pdf":
                    return creerPdf(entetes, lignes);
                default:
                    return creerCsv(entetes, lignes).getBytes(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération de l'export", e);
        }
    }

    private String protegerCellule(String valeur) {
        if (valeur == null) return "";
        if (valeur.startsWith("=") || valeur.startsWith("+") || valeur.startsWith("-")
                || valeur.startsWith("@") || valeur.startsWith("\t") || valeur.startsWith("\r")) {
            return "'" + valeur;
        }
        return valeur;
    }

    private String creerCsv(List<String> entetes, List<List<String>> lignes) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append(String.join(";", entetes)).append('\n');
        for (List<String> ligne : lignes) {
            csv.append(ligne.stream()
                    .map(this::protegerCellule)
                    .map(this::echapperCsv)
                    .reduce((a, b) -> a + ";" + b)
                    .orElse(""))
                    .append('\n');
        }
        return csv.toString();
    }

    private String echapperCsv(String valeur) {
        if (valeur.contains(";") || valeur.contains("\"") || valeur.contains("\n")) {
            return "\"" + valeur.replace("\"", "\"\"") + "\"";
        }
        return valeur;
    }

    private byte[] creerXlsx(List<String> entetes, List<List<String>> lignes) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            org.apache.poi.xssf.streaming.SXSSFSheet sheet = workbook.createSheet("Export");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < entetes.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(entetes.get(i));
            }

            for (int rowNum = 0; rowNum < lignes.size(); rowNum++) {
                Row row = sheet.createRow(rowNum + 1);
                List<String> ligne = lignes.get(rowNum);
                for (int colNum = 0; colNum < ligne.size(); colNum++) {
                    Cell cell = row.createCell(colNum);
                    String valeur = protegerCellule(ligne.get(colNum));
                    cell.setCellValue(valeur);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] creerPdf(List<String> entetes, List<List<String>> lignes) throws IOException {
        try (com.lowagie.text.Document document = new com.lowagie.text.Document();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(entetes.size());
            for (String entete : entetes) {
                table.addCell(new com.lowagie.text.Phrase(entete));
            }
            for (List<String> ligne : lignes) {
                for (String cell : ligne) {
                    table.addCell(new com.lowagie.text.Phrase(cell));
                }
            }
            document.add(table);
            document.close();

            return out.toByteArray();
        }
    }

    private static class ExportResult {
        final int lignes;
        final byte[] donnees;

        ExportResult(int lignes, byte[] donnees) {
            this.lignes = lignes;
            this.donnees = donnees;
        }
    }
}
