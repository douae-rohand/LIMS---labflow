package com.backend.modules.facturation.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.tenant.TenantContext;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.FactureLigne;
import com.backend.modules.facturation.entity.Paiement;
import com.backend.modules.facturation.entity.StatutFacture;
import com.backend.modules.facturation.repository.FactureLigneRepository;
import com.backend.modules.facturation.repository.FactureRepository;
import com.backend.modules.facturation.repository.PaiementRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.lowagie.text.Chunk;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Facture PDF et avoir PDF d'une facture du laboratoire courant (schéma tenant de la requête).
 * Mentions : identité et ICE du laboratoire, client ou patient (ICE du client professionnel),
 * numéro, dates, prestations, total, montant en toutes lettres, paiements et reste dû.
 */
@Service
@RequiredArgsConstructor
public class FacturePdfService {

    private static final Color MARQUE = new Color(27, 78, 82);
    private static final Color GRIS = new Color(90, 100, 104);
    private static final Color ROUGE = new Color(176, 58, 46);
    private static final ZoneId FUSEAU = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final FactureRepository factureRepository;
    private final FactureLigneRepository factureLigneRepository;
    private final PaiementRepository paiementRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantExecutor tenantExecutor;

    public record FichierPdf(String nom, byte[] contenu) {
    }

    private record Emetteur(String nom, String adresse, String contact, String ice) {
    }

    private record Destinataire(String nom, String ice, String adresse) {
    }

    @Transactional(readOnly = true)
    public FichierPdf facture(Long factureId) {
        Facture facture = charger(factureId);
        List<FactureLigne> lignes = factureLigneRepository.findByFactureId(factureId);
        List<Paiement> paiements = paiementRepository.findByFactureId(factureId);
        BigDecimal paye = paiements.stream().map(Paiement::getMontant).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean annulee = StatutFacture.ANNULEE.name().equals(facture.getStatut());
        return new FichierPdf("Facture-" + facture.getNumero() + ".pdf",
                generer(facture, lignes, paiements, paye, annulee, false));
    }

    @Transactional(readOnly = true)
    public FichierPdf avoir(Long factureId) {
        Facture facture = charger(factureId);
        if (!StatutFacture.ANNULEE.name().equals(facture.getStatut()) || facture.getNumeroAvoir() == null) {
            throw new BusinessRuleException("AVOIR_INEXISTANT", "Cette facture n'a pas été annulée : aucun avoir.");
        }
        List<FactureLigne> lignes = factureLigneRepository.findByFactureId(factureId);
        return new FichierPdf("Avoir-" + facture.getNumeroAvoir() + ".pdf",
                generer(facture, lignes, List.of(), BigDecimal.ZERO, true, true));
    }

    private Facture charger(Long factureId) {
        return factureRepository.findById(factureId)
                .orElseThrow(() -> new ResourceNotFoundException("Facture", "id", factureId));
    }

    private byte[] generer(Facture facture, List<FactureLigne> lignes, List<Paiement> paiements, BigDecimal paye,
                           boolean annulee, boolean avoir) {
        Emetteur emetteur = emetteur();
        Destinataire destinataire = destinataire(facture.getDemande());
        BigDecimal total = facture.getMontantHt() == null ? BigDecimal.ZERO : facture.getMontantHt();
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, sortie);
            document.open();
            Font titre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, MARQUE);
            Font sousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, MARQUE);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font gras = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font discret = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS);
            Font alerte = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, ROUGE);
            Font entete = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

            // Émetteur
            document.add(new Paragraph(emetteur.nom(), titre));
            if (emetteur.adresse() != null) document.add(new Paragraph(emetteur.adresse(), discret));
            if (emetteur.contact() != null) document.add(new Paragraph(emetteur.contact(), discret));
            if (emetteur.ice() != null) document.add(new Paragraph("ICE : " + emetteur.ice(), discret));
            document.add(Chunk.NEWLINE);

            String intitule = avoir ? "AVOIR n° " + facture.getNumeroAvoir() : "FACTURE n° " + facture.getNumero();
            document.add(new Paragraph(intitule, sousTitre));
            if (avoir) {
                document.add(new Paragraph("Annule la facture n° " + facture.getNumero(), gras));
            } else if (annulee) {
                document.add(new Paragraph("FACTURE ANNULÉE par l'avoir n° " + facture.getNumeroAvoir(), alerte));
            }
            document.add(Chunk.NEWLINE);

            PdfPTable infos = new PdfPTable(new float[]{1.3f, 3f});
            infos.setWidthPercentage(100);
            ligne(infos, avoir ? "Date de l'avoir" : "Date de facture",
                    avoir && facture.getDateAnnulation() != null
                            ? JOUR.format(facture.getDateAnnulation().atZone(FUSEAU))
                            : facture.getDateFacture() == null ? "—" : JOUR.format(facture.getDateFacture()), gras, normal);
            if (!avoir && facture.getDateEcheance() != null) {
                ligne(infos, "Échéance", JOUR.format(facture.getDateEcheance()), gras, normal);
            }
            Demande demande = facture.getDemande();
            if (demande != null) {
                ligne(infos, "Demande", demande.getNumero() + (demande.getTitre() == null ? "" : " — " + demande.getTitre()),
                        gras, normal);
            }
            ligne(infos, "Facturé à", destinataire.nom(), gras, normal);
            if (destinataire.ice() != null) ligne(infos, "ICE client", destinataire.ice(), gras, normal);
            if (destinataire.adresse() != null) ligne(infos, "Adresse", destinataire.adresse(), gras, normal);
            if (avoir && facture.getMotifAnnulation() != null) {
                ligne(infos, "Motif", facture.getMotifAnnulation(), gras, normal);
            }
            document.add(infos);
            document.add(Chunk.NEWLINE);

            PdfPTable tableau = new PdfPTable(new float[]{0.5f, 4f, 1.4f});
            tableau.setWidthPercentage(100);
            tableau.setHeaderRows(1);
            for (String colonne : List.of("N°", "Prestation", "Montant (MAD)")) {
                PdfPCell cellule = new PdfPCell(new Phrase(colonne, entete));
                cellule.setBackgroundColor(MARQUE);
                cellule.setPadding(5);
                tableau.addCell(cellule);
            }
            int numero = 1;
            for (FactureLigne l : lignes) {
                tableau.addCell(cellule(String.valueOf(numero++), normal, Element.ALIGN_LEFT));
                tableau.addCell(cellule(l.getDesignation() == null ? "—" : l.getDesignation(), normal, Element.ALIGN_LEFT));
                BigDecimal montant = l.getMontant() == null ? BigDecimal.ZERO : l.getMontant();
                tableau.addCell(cellule(mad(avoir ? montant.negate() : montant), normal, Element.ALIGN_RIGHT));
            }
            PdfPCell libelleTotal = cellule(avoir ? "TOTAL DE L'AVOIR" : "TOTAL", gras, Element.ALIGN_RIGHT);
            libelleTotal.setColspan(2);
            tableau.addCell(libelleTotal);
            tableau.addCell(cellule(mad(avoir ? total.negate() : total), gras, Element.ALIGN_RIGHT));
            document.add(tableau);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph((avoir ? "Arrêté le présent avoir à la somme de : " : "Arrêtée la présente facture à la somme de : ")
                    + MontantEnLettres.dirhams(total) + ".", gras));
            document.add(Chunk.NEWLINE);

            if (!avoir) {
                PdfPTable reglement = new PdfPTable(new float[]{2f, 1.4f});
                reglement.setWidthPercentage(50);
                reglement.setHorizontalAlignment(Element.ALIGN_RIGHT);
                ligneMontant(reglement, "Déjà payé", mad(paye), normal);
                BigDecimal reste = annulee ? BigDecimal.ZERO : total.subtract(paye).max(BigDecimal.ZERO);
                ligneMontant(reglement, annulee ? "Reste dû (facture annulée)" : "Reste dû", mad(reste), gras);
                document.add(reglement);
                if (!paiements.isEmpty()) {
                    document.add(Chunk.NEWLINE);
                    document.add(new Paragraph("Paiements reçus", gras));
                    for (Paiement p : paiements) {
                        document.add(new Paragraph("• " + (p.getDatePaiement() == null ? "" : JOUR.format(p.getDatePaiement().atZone(FUSEAU)))
                                + " — " + mad(p.getMontant()) + " MAD (" + mode(p.getMode()) + ", reçu " + p.getCode() + ")", normal));
                    }
                }
            }
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Document généré par LabFlow LIMS.", discret));
            document.close();
            return sortie.toByteArray();
        } catch (Exception ex) {
            throw new BusinessRuleException("PDF_FACTURE_ECHOUE", "Le document PDF n'a pas pu être généré.");
        }
    }

    /** Laboratoire émetteur : celui du schéma tenant de la requête. */
    private Emetteur emetteur() {
        String schema = TenantContext.getCurrentTenant();
        Laboratoire labo = schema == null ? null
                : tenantExecutor.inCentral(() -> laboratoireRepository.findByNomSchema(schema).orElse(null));
        if (labo == null) {
            return new Emetteur("Laboratoire", null, null, null);
        }
        return new Emetteur(labo.getRaisonSociale(),
                joindre(", ", labo.getAdresse(), joindre(" ", labo.getCodePostal(), labo.getVille())),
                joindre(" · ", labo.getTelephone(), labo.getEmail()), labo.getIce());
    }

    private Destinataire destinataire(Demande demande) {
        if (demande == null) {
            return new Destinataire("—", null, null);
        }
        if (demande.getClient() != null) {
            var client = demande.getClient();
            String nom = StringUtils.hasText(client.getRaisonSociale()) ? client.getRaisonSociale() : null;
            if (nom == null && client.getUtilisateurId() != null) {
                nom = tenantExecutor.inCentral(() -> utilisateurRepository.findById(client.getUtilisateurId())
                        .map(Utilisateur::getNomComplet).orElse(null));
            }
            return new Destinataire(nom == null ? "Client" : nom, client.getIce(), client.getAdresse());
        }
        if (demande.getPatient() != null) {
            var patient = demande.getPatient();
            return new Destinataire(((patient.getPrenom() == null ? "" : patient.getPrenom() + " ") + patient.getNom()).trim()
                    + " (patient " + patient.getCode() + ")", null, patient.getAdresse());
        }
        return new Destinataire("—", null, null);
    }

    private static void ligne(PdfPTable table, String libelle, String valeur, Font gras, Font normal) {
        PdfPCell a = new PdfPCell(new Phrase(libelle, gras));
        PdfPCell b = new PdfPCell(new Phrase(valeur == null ? "—" : valeur, normal));
        a.setBorder(PdfPCell.NO_BORDER);
        b.setBorder(PdfPCell.NO_BORDER);
        a.setPadding(2);
        b.setPadding(2);
        table.addCell(a);
        table.addCell(b);
    }

    private static void ligneMontant(PdfPTable table, String libelle, String valeur, Font police) {
        table.addCell(cellule(libelle, police, Element.ALIGN_LEFT));
        table.addCell(cellule(valeur, police, Element.ALIGN_RIGHT));
    }

    private static PdfPCell cellule(String texte, Font police, int alignement) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, police));
        cellule.setPadding(4);
        cellule.setHorizontalAlignment(alignement);
        return cellule;
    }

    private static String mad(BigDecimal montant) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.FRANCE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format((montant == null ? BigDecimal.ZERO : montant).setScale(2, RoundingMode.HALF_UP));
    }

    private static String mode(String mode) {
        if (mode == null) return "—";
        return switch (mode) {
            case "ESPECES" -> "espèces";
            case "CHEQUE" -> "chèque";
            case "CARTE" -> "carte bancaire";
            case "VIREMENT" -> "virement";
            default -> mode.toLowerCase(Locale.ROOT);
        };
    }

    private static String joindre(String separateur, String... parties) {
        String resultat = java.util.Arrays.stream(parties).filter(StringUtils::hasText).reduce((a, b) -> a + separateur + b)
                .orElse(null);
        return StringUtils.hasText(resultat) ? resultat : null;
    }
}
