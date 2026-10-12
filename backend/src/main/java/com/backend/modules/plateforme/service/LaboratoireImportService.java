package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.dto.LigneImportLaboratoireDto;
import com.backend.modules.plateforme.dto.RapportImportLaboratoireDto;
import com.backend.modules.plateforme.dto.ResultatLigneImportDto;
import com.backend.modules.plateforme.entity.DemandeIntegration;
import com.backend.modules.plateforme.entity.StatutIntegration;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Importe des laboratoires sous forme de demandes d'intégration incomplètes.
 * Aucun schéma tenant n'est créé : les documents justificatifs restent obligatoires
 * avant l'acceptation.
 */
@Service
@RequiredArgsConstructor
public class LaboratoireImportService {

    static final int MAX_LIGNES = 500;
    static final String MESSAGE_DOSSIER_INCOMPLET =
            "Demande créée par importation. Les six documents justificatifs sont encore requis avant l'acceptation. "
                    + "Aucun espace laboratoire n'a été créé.";

    static final List<String> ENTETES = List.of(
            "Nom du laboratoire",
            "Raison sociale",
            "Types de laboratoire",
            "Identifiant ICE",
            "Ville",
            "Adresse",
            "Région",
            "Code postal",
            "Pays",
            "Téléphone du laboratoire",
            "Adresse e-mail du laboratoire",
            "Site web",
            "Nom de l'administrateur",
            "Prénom de l'administrateur",
            "Adresse e-mail de l'administrateur",
            "Téléphone de l'administrateur",
            "Fonction de l'administrateur",
            "CIN de l'administrateur",
            "Latitude",
            "Longitude"
    );

    private static final List<String> TYPES = List.of(
            "Médical", "Industriel", "Environnemental", "Pharmaceutique et cosmétique");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern ICE = Pattern.compile("^\\d{15}$");

    private final DemandeIntegrationRepository demandeIntegrationRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final UtilisateurRepository utilisateurRepository;

    public FichierGenere modele(String format) {
        return switch (format == null ? "" : format.trim().toLowerCase(Locale.ROOT)) {
            case "xlsx", "excel" -> new FichierGenere(
                    "modele-importation-laboratoires.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    modeleExcel());
            case "csv" -> new FichierGenere(
                    "modele-importation-laboratoires.csv",
                    "text/csv; charset=UTF-8",
                    modeleCsv());
            default -> throw new BusinessRuleException("FORMAT_MODELE_INVALIDE",
                    "Le modèle doit être téléchargé au format Excel ou CSV");
        };
    }

    @Transactional(readOnly = true)
    public RapportImportLaboratoireDto analyser(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessRuleException("FICHIER_MANQUANT", "Choisissez un fichier Excel ou CSV à importer");
        }
        String nom = fichier.getOriginalFilename() == null ? "" : fichier.getOriginalFilename().toLowerCase(Locale.ROOT);
        try (InputStream flux = fichier.getInputStream()) {
            List<LigneImportLaboratoireDto> lignes = nom.endsWith(".csv")
                    ? lireCsv(flux)
                    : nom.endsWith(".xlsx") ? lireExcel(flux)
                    : throwFormat();
            return evaluer(lignes, true);
        } catch (IOException ex) {
            throw new BusinessRuleException("FICHIER_ILLISIBLE", "Le fichier n'a pas pu être lu");
        }
    }

    @Transactional(readOnly = true)
    public RapportImportLaboratoireDto verifier(List<LigneImportLaboratoireDto> lignes) {
        return evaluer(lignes, true);
    }

    @Transactional
    public RapportImportLaboratoireDto confirmer(List<LigneImportLaboratoireDto> lignes) {
        RapportImportLaboratoireDto rapport = evaluer(lignes, false);
        int enregistrees = 0;
        for (ResultatLigneImportDto ligne : rapport.getLignes()) {
            if (!ligne.isValide()) {
                continue;
            }
            DemandeIntegration demande = enregistrerDemande(ligne.getDonnees());
            ligne.setDemandeId(demande.getId());
            ligne.setNumeroDemande(demande.getNumero());
            enregistrees++;
        }
        rapport.setApercu(false);
        rapport.setEnregistrees(enregistrees);
        rapport.setMessage(enregistrees + " demande(s) d'intégration créée(s), "
                + rapport.getInvalides() + " ligne(s) non importée(s). "
                + "Les documents justificatifs restent obligatoires : aucun espace laboratoire n'a été créé.");
        return rapport;
    }

    private List<LigneImportLaboratoireDto> throwFormat() {
        throw new BusinessRuleException("FORMAT_FICHIER_INVALIDE",
                "Utilisez le modèle Excel (.xlsx) ou le modèle CSV");
    }

    private RapportImportLaboratoireDto evaluer(List<LigneImportLaboratoireDto> lignes, boolean apercu) {
        if (lignes == null || lignes.isEmpty()) {
            throw new BusinessRuleException("IMPORT_VIDE", "Le fichier ne contient aucune ligne à importer");
        }
        if (lignes.size() > MAX_LIGNES) {
            throw new BusinessRuleException("IMPORT_TROP_VOLUMINEUX",
                    "Le fichier dépasse 500 lignes. Divisez-le en plusieurs importations.");
        }

        Set<String> emailsFichier = new HashSet<>();
        Set<String> icesFichier = new HashSet<>();
        List<ResultatLigneImportDto> resultats = new ArrayList<>();
        int valides = 0;
        for (int i = 0; i < lignes.size(); i++) {
            LigneImportLaboratoireDto ligne = lignes.get(i);
            int numero = ligne.getNumeroLigne() != null && ligne.getNumeroLigne() > 0
                    ? ligne.getNumeroLigne()
                    : i + 2;
            List<String> erreurs = validerLigne(ligne, emailsFichier, icesFichier);
            boolean valide = erreurs.isEmpty();
            if (valide) {
                valides++;
            }
            resultats.add(ResultatLigneImportDto.builder()
                    .numeroLigne(numero)
                    .nomLaboratoire(texte(ligne.getNomLaboratoire()))
                    .adminEmail(texte(ligne.getAdminEmail()) == null
                            ? null
                            : texte(ligne.getAdminEmail()).toLowerCase(Locale.ROOT))
                    .valide(valide)
                    .erreurs(erreurs)
                    .donnees(ligne)
                    .build());
        }
        int invalides = resultats.size() - valides;
        return RapportImportLaboratoireDto.builder()
                .total(resultats.size())
                .valides(valides)
                .invalides(invalides)
                .enregistrees(0)
                .apercu(apercu)
                .message(apercu
                        ? valides + " ligne(s) prête(s), " + invalides
                        + " ligne(s) à corriger. Aucune demande n'a encore été enregistrée."
                        : "")
                .lignes(resultats)
                .build();
    }

    private List<String> validerLigne(LigneImportLaboratoireDto ligne,
                                      Set<String> emailsFichier,
                                      Set<String> icesFichier) {
        List<String> erreurs = new ArrayList<>();
        exiger(ligne.getNomLaboratoire(), "Le nom du laboratoire est obligatoire", erreurs);
        exiger(ligne.getRaisonSociale(), "La raison sociale est obligatoire", erreurs);
        exiger(ligne.getVille(), "La ville est obligatoire", erreurs);
        exiger(ligne.getAdresse(), "L'adresse est obligatoire", erreurs);
        exiger(ligne.getAdminNom(), "Le nom de l'administrateur est obligatoire", erreurs);
        exiger(ligne.getAdminPrenom(), "Le prénom de l'administrateur est obligatoire", erreurs);
        exiger(ligne.getAdminTelephone(), "Le téléphone de l'administrateur est obligatoire", erreurs);
        exiger(ligne.getAdminFonction(), "La fonction de l'administrateur est obligatoire", erreurs);
        exiger(ligne.getAdminCin(), "Le CIN de l'administrateur est obligatoire", erreurs);

        String types = normaliserTypes(ligne.getTypesLaboratoire(), erreurs);
        ligne.setTypesLaboratoire(types);

        String emailAdmin = texte(ligne.getAdminEmail());
        if (emailAdmin == null) {
            erreurs.add("L'adresse e-mail de l'administrateur est obligatoire");
        } else {
            emailAdmin = emailAdmin.toLowerCase(Locale.ROOT);
            ligne.setAdminEmail(emailAdmin);
            if (!EMAIL.matcher(emailAdmin).matches()) {
                erreurs.add("L'adresse e-mail de l'administrateur est invalide");
            } else if (!emailsFichier.add(emailAdmin)) {
                erreurs.add("Cette adresse e-mail d'administrateur est déjà présente dans le fichier");
            } else if (utilisateurRepository.existsByEmail(emailAdmin)
                    || demandeIntegrationRepository.existsByContactEmailAndStatut(
                            emailAdmin, StatutIntegration.EN_ATTENTE.name())) {
                erreurs.add("Cette adresse e-mail est déjà utilisée par un compte ou une demande en attente");
            }
        }

        String emailLabo = texte(ligne.getEmailLaboratoire());
        if (emailLabo != null && !EMAIL.matcher(emailLabo).matches()) {
            erreurs.add("L'adresse e-mail du laboratoire est invalide");
        }

        String ice = texte(ligne.getIce());
        if (ice != null) {
            if (!ICE.matcher(ice).matches()) {
                erreurs.add("L'identifiant ICE doit contenir 15 chiffres");
            } else if (!icesFichier.add(ice)) {
                erreurs.add("Cet identifiant ICE est déjà présent dans le fichier");
            } else if (laboratoireRepository.existsByIce(ice)
                    || demandeIntegrationRepository.existsByIceAndStatut(ice, StatutIntegration.EN_ATTENTE.name())) {
                erreurs.add("Cet identifiant ICE est déjà associé à un laboratoire ou une demande en attente");
            }
        }

        Double latitude = nombre(ligne.getLatitude(), "La latitude est invalide", erreurs);
        Double longitude = nombre(ligne.getLongitude(), "La longitude est invalide", erreurs);
        if ((latitude == null) != (longitude == null) && erreurs.stream().noneMatch(e -> e.contains("latitude") || e.contains("longitude"))) {
            erreurs.add("La latitude et la longitude doivent être renseignées ensemble");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            erreurs.add("La latitude doit être comprise entre -90 et 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            erreurs.add("La longitude doit être comprise entre -180 et 180");
        }

        limiter(ligne.getNomLaboratoire(), 255, "Le nom du laboratoire est trop long", erreurs);
        limiter(ligne.getRaisonSociale(), 255, "La raison sociale est trop longue", erreurs);
        limiter(ligne.getVille(), 100, "La ville est trop longue", erreurs);
        limiter(ligne.getAdminCin(), 20, "Le CIN est trop long", erreurs);
        return erreurs;
    }

    private DemandeIntegration enregistrerDemande(LigneImportLaboratoireDto ligne) {
        DemandeIntegration demande = DemandeIntegration.builder()
                .numero("INT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                .dateDemande(Instant.now())
                .statut(StatutIntegration.EN_ATTENTE.name())
                .nomLaboratoire(texte(ligne.getNomLaboratoire()))
                .raisonSociale(texte(ligne.getRaisonSociale()))
                .typeLaboratoire(texte(ligne.getTypesLaboratoire()))
                .ice(texte(ligne.getIce()))
                .telephoneLaboratoire(texte(ligne.getTelephoneLaboratoire()))
                .emailLaboratoire(texte(ligne.getEmailLaboratoire()))
                .siteWeb(texte(ligne.getSiteWeb()))
                .message(MESSAGE_DOSSIER_INCOMPLET)
                .adresse(texte(ligne.getAdresse()))
                .ville(texte(ligne.getVille()))
                .region(texte(ligne.getRegion()))
                .pays(StringUtils.hasText(ligne.getPays()) ? ligne.getPays().trim() : "Maroc")
                .codePostal(texte(ligne.getCodePostal()))
                .latitude(parseSilencieux(ligne.getLatitude()))
                .longitude(parseSilencieux(ligne.getLongitude()))
                .contactNom(texte(ligne.getAdminNom()))
                .contactPrenom(texte(ligne.getAdminPrenom()))
                .contactEmail(texte(ligne.getAdminEmail()).toLowerCase(Locale.ROOT))
                .contactTelephone(texte(ligne.getAdminTelephone()))
                .contactFonction(texte(ligne.getAdminFonction()))
                .contactCin(texte(ligne.getAdminCin()))
                .build();
        return demandeIntegrationRepository.saveAndFlush(demande);
    }

    private byte[] modeleExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet donnees = workbook.createSheet("Laboratoires");
            Row entete = donnees.createRow(0);
            for (int i = 0; i < ENTETES.size(); i++) {
                entete.createCell(i).setCellValue(ENTETES.get(i));
                donnees.setColumnWidth(i, 22 * 256);
            }
            Sheet instructions = workbook.createSheet("Instructions");
            int ligne = 0;
            instructions.createRow(ligne++).createCell(0).setCellValue(
                    "Remplissez la feuille Laboratoires. Ne modifiez pas la ligne d'en-têtes.");
            instructions.createRow(ligne++).createCell(0).setCellValue(
                    "Champs obligatoires : Nom du laboratoire, Raison sociale, Types de laboratoire, Ville, Adresse, "
                            + "Nom, prénom, adresse e-mail, téléphone, fonction et CIN de l'administrateur.");
            instructions.createRow(ligne++).createCell(0).setCellValue(
                    "Types acceptés, séparés par une virgule : " + String.join(", ", TYPES) + ".");
            instructions.createRow(ligne++).createCell(0).setCellValue(
                    "L'identifiant ICE, s'il est renseigné, doit contenir 15 chiffres.");
            instructions.createRow(ligne++).createCell(0).setCellValue(
                    "Cette importation crée une demande d'intégration incomplète. "
                            + "Les six documents justificatifs restent obligatoires. Aucun espace laboratoire n'est créé.");
            instructions.setColumnWidth(0, 140 * 256);
            workbook.setActiveSheet(0);
            workbook.write(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("MODELE_EXCEL_ECHOUE", "La création du modèle Excel a échoué");
        }
    }

    private byte[] modeleCsv() {
        String contenu = "\uFEFF"
                + "# Modèle d'importation des laboratoires LabFlow. Les lignes commençant par # sont ignorées.\r\n"
                + "# Champs obligatoires : Nom du laboratoire, Raison sociale, Types de laboratoire, Ville, Adresse, "
                + "Nom de l'administrateur, Prénom de l'administrateur, Adresse e-mail de l'administrateur, "
                + "Téléphone de l'administrateur, Fonction de l'administrateur, CIN de l'administrateur.\r\n"
                + "# Types acceptés, séparés par une virgule : " + String.join(", ", TYPES) + ".\r\n"
                + "# L'importation crée une demande d'intégration incomplète. Les documents justificatifs restent obligatoires. "
                + "Aucun espace laboratoire n'est créé.\r\n"
                + String.join(";", ENTETES) + "\r\n";
        return contenu.getBytes(StandardCharsets.UTF_8);
    }

    private List<LigneImportLaboratoireDto> lireExcel(InputStream flux) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(flux)) {
            Sheet feuille = workbook.getSheet("Laboratoires");
            if (feuille == null) {
                feuille = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (feuille == null) {
                throw new BusinessRuleException("IMPORT_VIDE", "Le fichier Excel ne contient aucune feuille");
            }
            DataFormatter formatter = new DataFormatter();
            int premiere = feuille.getFirstRowNum();
            Row entete = feuille.getRow(premiere);
            verifierEntetes(lireLigneExcel(entete, formatter));
            List<LigneImportLaboratoireDto> lignes = new ArrayList<>();
            for (int i = premiere + 1; i <= feuille.getLastRowNum(); i++) {
                List<String> cellules = lireLigneExcel(feuille.getRow(i), formatter);
                if (cellules.stream().allMatch(c -> !StringUtils.hasText(c))) {
                    continue;
                }
                lignes.add(versLigne(i + 1, cellules));
            }
            return lignes;
        }
    }

    private List<String> lireLigneExcel(Row row, DataFormatter formatter) {
        List<String> cellules = new ArrayList<>();
        if (row == null) {
            return cellules;
        }
        for (int i = 0; i < ENTETES.size(); i++) {
            Cell cell = row.getCell(i);
            cellules.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
        }
        return cellules;
    }

    private List<LigneImportLaboratoireDto> lireCsv(InputStream flux) throws IOException {
        String contenu = new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        if (contenu.startsWith("\uFEFF")) {
            contenu = contenu.substring(1);
        }
        List<String> brutes = contenu.lines()
                .map(String::trim)
                .filter(ligne -> !ligne.isEmpty() && !ligne.startsWith("#"))
                .toList();
        if (brutes.isEmpty()) {
            throw new BusinessRuleException("IMPORT_VIDE", "Le fichier CSV ne contient aucune ligne à importer");
        }
        char separateur = brutes.getFirst().chars().filter(c -> c == ';').count()
                >= brutes.getFirst().chars().filter(c -> c == ',').count() ? ';' : ',';
        verifierEntetes(decouperCsv(brutes.getFirst(), separateur));
        List<LigneImportLaboratoireDto> lignes = new ArrayList<>();
        for (int i = 1; i < brutes.size(); i++) {
            lignes.add(versLigne(i + 1, decouperCsv(brutes.get(i), separateur)));
        }
        return lignes;
    }

    private void verifierEntetes(List<String> entetes) {
        if (entetes.size() < ENTETES.size()) {
            throw new BusinessRuleException("IMPORT_ENTETES_INVALIDES",
                    "Les colonnes ne correspondent pas au modèle. Téléchargez le modèle et conservez la ligne d'en-têtes.");
        }
        for (int i = 0; i < ENTETES.size(); i++) {
            if (!ENTETES.get(i).equals(entetes.get(i).trim())) {
                throw new BusinessRuleException("IMPORT_ENTETES_INVALIDES",
                        "Colonne attendue : « " + ENTETES.get(i) + " ». Téléchargez le modèle fourni.");
            }
        }
    }

    private LigneImportLaboratoireDto versLigne(int numero, List<String> cellules) {
        return LigneImportLaboratoireDto.builder()
                .numeroLigne(numero)
                .nomLaboratoire(cellule(cellules, 0))
                .raisonSociale(cellule(cellules, 1))
                .typesLaboratoire(cellule(cellules, 2))
                .ice(cellule(cellules, 3))
                .ville(cellule(cellules, 4))
                .adresse(cellule(cellules, 5))
                .region(cellule(cellules, 6))
                .codePostal(cellule(cellules, 7))
                .pays(cellule(cellules, 8))
                .telephoneLaboratoire(cellule(cellules, 9))
                .emailLaboratoire(cellule(cellules, 10))
                .siteWeb(cellule(cellules, 11))
                .adminNom(cellule(cellules, 12))
                .adminPrenom(cellule(cellules, 13))
                .adminEmail(cellule(cellules, 14))
                .adminTelephone(cellule(cellules, 15))
                .adminFonction(cellule(cellules, 16))
                .adminCin(cellule(cellules, 17))
                .latitude(cellule(cellules, 18))
                .longitude(cellule(cellules, 19))
                .build();
    }

    private static String cellule(List<String> cellules, int index) {
        if (index >= cellules.size()) {
            return null;
        }
        String valeur = cellules.get(index);
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    private static List<String> decouperCsv(String ligne, char separateur) {
        List<String> cellules = new ArrayList<>();
        StringBuilder courant = new StringBuilder();
        boolean guillemets = false;
        for (int i = 0; i < ligne.length(); i++) {
            char caractere = ligne.charAt(i);
            if (caractere == '"') {
                if (guillemets && i + 1 < ligne.length() && ligne.charAt(i + 1) == '"') {
                    courant.append('"');
                    i++;
                } else {
                    guillemets = !guillemets;
                }
            } else if (caractere == separateur && !guillemets) {
                cellules.add(courant.toString().trim());
                courant.setLength(0);
            } else {
                courant.append(caractere);
            }
        }
        cellules.add(courant.toString().trim());
        return cellules;
    }

    private String normaliserTypes(String brut, List<String> erreurs) {
        if (!StringUtils.hasText(brut)) {
            erreurs.add("Au moins un type de laboratoire est obligatoire");
            return null;
        }
        LinkedHashSet<String> retenus = new LinkedHashSet<>();
        for (String morceau : brut.split("[,;]")) {
            String canonique = TYPES.stream()
                    .filter(type -> type.toLowerCase(Locale.FRENCH).equals(morceau.trim().toLowerCase(Locale.FRENCH)))
                    .findFirst()
                    .orElse(null);
            if (canonique == null && StringUtils.hasText(morceau)) {
                erreurs.add("Type de laboratoire inconnu : " + morceau.trim()
                        + ". Valeurs acceptées : " + String.join(", ", TYPES));
            } else if (canonique != null) {
                retenus.add(canonique);
            }
        }
        if (retenus.isEmpty() && erreurs.stream().noneMatch(e -> e.startsWith("Type de laboratoire"))) {
            erreurs.add("Au moins un type de laboratoire est obligatoire");
        }
        return retenus.isEmpty() ? texte(brut) : String.join(", ", retenus);
    }

    private static void exiger(String valeur, String message, List<String> erreurs) {
        if (!StringUtils.hasText(valeur)) {
            erreurs.add(message);
        }
    }

    private static void limiter(String valeur, int max, String message, List<String> erreurs) {
        if (valeur != null && valeur.trim().length() > max) {
            erreurs.add(message);
        }
    }

    private static Double nombre(String valeur, String message, List<String> erreurs) {
        if (!StringUtils.hasText(valeur)) {
            return null;
        }
        try {
            return Double.valueOf(valeur.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            erreurs.add(message);
            return null;
        }
    }

    private static Double parseSilencieux(String valeur) {
        if (!StringUtils.hasText(valeur)) {
            return null;
        }
        return Double.valueOf(valeur.trim().replace(',', '.'));
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }
}
