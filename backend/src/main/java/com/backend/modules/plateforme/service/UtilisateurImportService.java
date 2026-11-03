package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.common.util.ApresCommit;
import com.backend.modules.plateforme.service.AdministrateurLaboratoireService.InvitationPreparee;
import com.backend.modules.plateforme.dto.FichierGenere;
import com.backend.modules.plateforme.dto.LigneImportUtilisateurDto;
import com.backend.modules.plateforme.dto.RapportImportUtilisateurDto;
import com.backend.modules.plateforme.dto.ResultatLigneUtilisateurDto;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.entity.StatutLabo;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilisateurImportService {

    static final int MAX_LIGNES = 500;
    private static final int MAX_ANALYSES_PAR_ACTEUR = 3;
    static final long MAX_OCTETS = 2L * 1024 * 1024;
    static final List<String> ENTETES = List.of(
            "Nom", "Prénom", "E-mail", "Rôle", "Laboratoire", "Téléphone", "Fonction");
    private static final List<String> EXEMPLE = List.of(
            "Bennani", "Amina", "amina.bennani@exemple.ma", "TECHNICIEN",
            "Laboratoire exemple", "0612345678", "Technicienne");
    private static final Set<RoleUtilisateur> ROLES = Set.of(
            RoleUtilisateur.ADMINISTRATEUR,
            RoleUtilisateur.RESPONSABLE,
            RoleUtilisateur.TECHNICIEN,
            RoleUtilisateur.ACCUEIL);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ConcurrentHashMap<String, AnalyseMemorisee> analyses = new ConcurrentHashMap<>();

    private final LaboratoireRepository laboratoireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AdministrateurLaboratoireService administrateurLaboratoireService;

    public FichierGenere modele(String format) {
        return switch (format == null ? "" : format.trim().toLowerCase(Locale.ROOT)) {
            case "xlsx", "excel" -> new FichierGenere(
                    "modele-importation-utilisateurs.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    modeleExcel());
            case "csv" -> new FichierGenere(
                    "modele-importation-utilisateurs.csv",
                    "text/csv; charset=UTF-8",
                    modeleCsv());
            default -> throw new BusinessRuleException("FORMAT_MODELE_INVALIDE",
                    "Le modèle doit être téléchargé au format Excel ou CSV");
        };
    }

    @Transactional(readOnly = true)
    public RapportImportUtilisateurDto analyser(MultipartFile fichier, String acteurEmail) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessRuleException("FICHIER_MANQUANT", "Choisissez un fichier Excel ou CSV à importer");
        }
        if (fichier.getSize() > MAX_OCTETS) {
            throw new BusinessRuleException("FICHIER_TROP_VOLUMINEUX", "Le fichier ne doit pas dépasser 2 Mo");
        }
        String nom = fichier.getOriginalFilename() == null ? "" : fichier.getOriginalFilename().toLowerCase(Locale.ROOT);
        try (InputStream flux = fichier.getInputStream()) {
            List<LigneImportUtilisateurDto> lignes = nom.endsWith(".csv")
                    ? lireCsv(flux)
                    : nom.endsWith(".xlsx") ? lireExcel(flux)
                    : throwFormat();
            RapportImportUtilisateurDto rapport = evaluer(lignes, true);
            String jeton = memoriser(lignes, acteurEmail);
            rapport.setJetonAnalyse(jeton);
            return rapport;
        } catch (IOException ex) {
            throw new BusinessRuleException("FICHIER_ILLISIBLE", "Le fichier n'a pas pu être lu");
        }
    }

    @Transactional
    public RapportImportUtilisateurDto confirmer(String jeton, String acteurEmail) {
        AnalyseMemorisee analyse = retirer(jeton, acteurEmail);
        List<LigneImportUtilisateurDto> lignes = analyse.lignes();
        RapportImportUtilisateurDto rapport = evaluer(lignes, false);
        List<InvitationPreparee> preparees = new ArrayList<>();
        for (int i = 0; i < lignes.size(); i++) {
            ResultatLigneUtilisateurDto resultat = rapport.getLignes().get(i);
            if (!resultat.isValide()) {
                continue;
            }
            LigneImportUtilisateurDto ligne = lignes.get(i);
            Laboratoire laboratoire = laboratoireUnique(ligne.getLaboratoire());
            preparees.add(administrateurLaboratoireService.preparerInvite(
                    RoleUtilisateur.valueOf(ligne.getRole().trim().toUpperCase(Locale.ROOT)),
                    laboratoire.getId(),
                    ligne.getNom(),
                    ligne.getPrenom(),
                    ligne.getEmail(),
                    ligne.getTelephone(),
                    ligne.getFonction(),
                    acteurEmail,
                    ligne.getNumeroLigne()));
        }
        int enregistres = preparees.size();
        rapport.setEnregistres(enregistres);
        rapport.setApercu(false);
        rapport.setJetonAnalyse(null);
        rapport.setInvitationsNonEnvoyees(0);
        rapport.setInvitationsEchouees(List.of());
        rapport.setMessage(enregistres + " compte" + (enregistres > 1 ? "s" : "") + " créé"
                + (enregistres > 1 ? "s" : "")
                + ". Les invitations partent ensuite. Si un e-mail n'arrive pas, utilisez « Renvoyer l'invitation » sur la ligne du compte.");
        int invalides = rapport.getInvalides();
        ApresCommit.executer(() -> administrateurLaboratoireService.envoyerInvitationsApresImport(
                preparees, acteurEmail, enregistres, invalides));
        return rapport;
    }

    private String memoriser(List<LigneImportUtilisateurDto> lignes, String acteurEmail) {
        purgerExpirees();
        List<String> siennes = analyses.entrySet().stream()
                .filter(entree -> acteurEmail.equalsIgnoreCase(entree.getValue().acteurEmail()))
                .sorted(Comparator.comparing(entree -> entree.getValue().expiration()))
                .map(java.util.Map.Entry::getKey)
                .toList();
        int surplus = siennes.size() - (MAX_ANALYSES_PAR_ACTEUR - 1);
        for (int i = 0; i < surplus; i++) {
            analyses.remove(siennes.get(i));
        }
        String jeton = UUID.randomUUID().toString();
        analyses.put(jeton, new AnalyseMemorisee(acteurEmail, lignes, Instant.now().plus(30, ChronoUnit.MINUTES)));
        return jeton;
    }

    private void purgerExpirees() {
        Instant maintenant = Instant.now();
        analyses.entrySet().removeIf(entree -> entree.getValue().expiration().isBefore(maintenant));
    }

    private AnalyseMemorisee retirer(String jeton, String acteurEmail) {
        purgerExpirees();
        if (jeton == null || jeton.isBlank() || acteurEmail == null) {
            throw new BusinessRuleException("ANALYSE_INTROUVABLE", "Relancez l'analyse du fichier avant de confirmer");
        }
        AnalyseMemorisee analyse = analyses.remove(jeton.trim());
        if (analyse == null || analyse.expiration().isBefore(Instant.now())
                || !acteurEmail.equalsIgnoreCase(analyse.acteurEmail())) {
            throw new BusinessRuleException("ANALYSE_EXPIREE",
                    "Cette analyse n'est plus disponible. Téléversez à nouveau le fichier.");
        }
        return analyse;
    }

    private record AnalyseMemorisee(String acteurEmail, List<LigneImportUtilisateurDto> lignes, Instant expiration) {}

    private RapportImportUtilisateurDto evaluer(List<LigneImportUtilisateurDto> lignes, boolean apercu) {
        if (lignes.size() > MAX_LIGNES) {
            throw new BusinessRuleException("IMPORT_TROP_GRAND", "500 lignes au maximum par importation");
        }
        Set<String> emailsFichier = new HashSet<>();
        Set<Long> administrateursFichier = new HashSet<>();
        List<ResultatLigneUtilisateurDto> resultats = new ArrayList<>();
        int valides = 0;
        for (LigneImportUtilisateurDto ligne : lignes) {
            List<String> erreurs = new ArrayList<>();
            String email = normaliserEmail(ligne.getEmail(), erreurs);
            RoleUtilisateur role = role(ligne.getRole(), erreurs);
            Laboratoire laboratoire = laboratoire(ligne.getLaboratoire(), erreurs);
            if (!StringUtils.hasText(ligne.getNom())) {
                erreurs.add("Le nom est obligatoire");
            } else if (ligne.getNom().trim().length() > 100) {
                erreurs.add("Le nom ne peut pas dépasser 100 caractères");
            }
            if (StringUtils.hasText(ligne.getPrenom()) && ligne.getPrenom().trim().length() > 100) {
                erreurs.add("Le prénom ne peut pas dépasser 100 caractères");
            }
            if (StringUtils.hasText(ligne.getTelephone()) && ligne.getTelephone().trim().length() > 50) {
                erreurs.add("Le téléphone ne peut pas dépasser 50 caractères");
            }
            if (StringUtils.hasText(ligne.getFonction()) && ligne.getFonction().trim().length() > 100) {
                erreurs.add("La fonction ne peut pas dépasser 100 caractères");
            }
            if (email != null) {
                if (!emailsFichier.add(email)) {
                    erreurs.add("Cet e-mail est en doublon dans le fichier");
                } else if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
                    erreurs.add("Cet e-mail est déjà associé à un compte");
                }
            }
            if (role == RoleUtilisateur.ADMINISTRATEUR && laboratoire != null && erreurs.isEmpty()) {
                boolean dejaDansFichier = !administrateursFichier.add(laboratoire.getId());
                if (dejaDansFichier || utilisateurRepository.compterAdministrateursActifs(laboratoire.getId()) > 0) {
                    erreurs.add("Ce laboratoire a déjà un administrateur actif");
                }
            }
            boolean valide = erreurs.isEmpty();
            if (valide) {
                valides++;
            }
            resultats.add(ResultatLigneUtilisateurDto.builder()
                    .numeroLigne(ligne.getNumeroLigne())
                    .email(email)
                    .valide(valide)
                    .erreurs(erreurs)
                    .build());
        }
        int invalides = lignes.size() - valides;
        return RapportImportUtilisateurDto.builder()
                .total(lignes.size())
                .valides(valides)
                .invalides(invalides)
                .enregistres(0)
                .apercu(apercu)
                .message(apercu
                        ? "Analyse terminée. Aucune ligne n'a été enregistrée."
                        : null)
                .lignes(resultats)
                .build();
    }

    private Laboratoire laboratoireUnique(String nom) {
        List<Laboratoire> trouves = laboratoireRepository.findAllByRaisonSocialeIgnoreCase(nom.trim());
        if (trouves.size() != 1 || !StatutLabo.ACTIF.name().equals(trouves.getFirst().getStatut())) {
            throw new BusinessRuleException("IMPORT_LIGNE_INVALIDE",
                    "Une ligne valide à l'analyse ne l'est plus. Relancez l'analyse.");
        }
        return trouves.getFirst();
    }

    private static String normaliserEmail(String brut, List<String> erreurs) {
        if (!StringUtils.hasText(brut)) {
            erreurs.add("L'e-mail est obligatoire");
            return null;
        }
        String email = brut.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 255 || !EMAIL.matcher(email).matches()) {
            erreurs.add("L'e-mail est invalide");
            return null;
        }
        return email;
    }

    private static RoleUtilisateur role(String brut, List<String> erreurs) {
        if (!StringUtils.hasText(brut)) {
            erreurs.add("Le rôle est obligatoire");
            return null;
        }
        try {
            RoleUtilisateur role = RoleUtilisateur.valueOf(brut.trim().toUpperCase(Locale.ROOT));
            if (!ROLES.contains(role)) {
                erreurs.add("Rôle non autorisé. Valeurs acceptées : ADMINISTRATEUR, RESPONSABLE, TECHNICIEN, ACCUEIL");
                return null;
            }
            return role;
        } catch (IllegalArgumentException ex) {
            erreurs.add("Rôle non autorisé. Valeurs acceptées : ADMINISTRATEUR, RESPONSABLE, TECHNICIEN, ACCUEIL");
            return null;
        }
    }

    private Laboratoire laboratoire(String nom, List<String> erreurs) {
        if (!StringUtils.hasText(nom)) {
            erreurs.add("Le laboratoire est obligatoire");
            return null;
        }
        List<Laboratoire> trouves = laboratoireRepository.findAllByRaisonSocialeIgnoreCase(nom.trim());
        if (trouves.isEmpty()) {
            erreurs.add("Laboratoire introuvable");
            return null;
        }
        if (trouves.size() > 1) {
            erreurs.add("Plusieurs laboratoires portent ce nom");
            return null;
        }
        Laboratoire laboratoire = trouves.getFirst();
        if (!StatutLabo.ACTIF.name().equals(laboratoire.getStatut())) {
            erreurs.add("Le laboratoire n'est pas actif");
            return null;
        }
        return laboratoire;
    }

    private List<LigneImportUtilisateurDto> lireExcel(InputStream flux) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(flux)) {
            Sheet feuille = workbook.getSheet("Utilisateurs");
            if (feuille == null) {
                feuille = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            }
            if (feuille == null) {
                throw new BusinessRuleException("IMPORT_VIDE", "Le fichier Excel ne contient aucune feuille");
            }
            DataFormatter formatter = new DataFormatter();
            int premiere = feuille.getFirstRowNum();
            verifierEntetes(lireLigneExcel(feuille.getRow(premiere), formatter));
            List<LigneImportUtilisateurDto> lignes = new ArrayList<>();
            for (int i = premiere + 1; i <= feuille.getLastRowNum(); i++) {
                List<String> cellules = lireLigneExcel(feuille.getRow(i), formatter);
                if (cellules.stream().allMatch(c -> !StringUtils.hasText(c))) {
                    continue;
                }
                lignes.add(versLigne(i + 1, cellules));
            }
            if (lignes.isEmpty()) {
                throw new BusinessRuleException("IMPORT_VIDE", "Le fichier ne contient aucune ligne à importer");
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

    private List<LigneImportUtilisateurDto> lireCsv(InputStream flux) throws IOException {
        byte[] octets = flux.readAllBytes();
        if (octets.length > MAX_OCTETS) {
            throw new BusinessRuleException("FICHIER_TROP_VOLUMINEUX", "Le fichier ne doit pas dépasser 2 Mo");
        }
        String contenu = new String(octets, StandardCharsets.UTF_8);
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
        List<LigneImportUtilisateurDto> lignes = new ArrayList<>();
        for (int i = 1; i < brutes.size(); i++) {
            List<String> cellules = decouperCsv(brutes.get(i), separateur);
            if (cellules.stream().allMatch(c -> !StringUtils.hasText(c))) {
                continue;
            }
            lignes.add(versLigne(i + 1, cellules));
        }
        if (lignes.isEmpty()) {
            throw new BusinessRuleException("IMPORT_VIDE", "Le fichier ne contient aucune ligne à importer");
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

    private static LigneImportUtilisateurDto versLigne(int numero, List<String> cellules) {
        return LigneImportUtilisateurDto.builder()
                .numeroLigne(numero)
                .nom(cellule(cellules, 0))
                .prenom(cellule(cellules, 1))
                .email(cellule(cellules, 2))
                .role(cellule(cellules, 3))
                .laboratoire(cellule(cellules, 4))
                .telephone(cellule(cellules, 5))
                .fonction(cellule(cellules, 6))
                .build();
    }

    private static String cellule(List<String> cellules, int index) {
        if (index >= cellules.size() || cellules.get(index) == null || cellules.get(index).isBlank()) {
            return null;
        }
        return cellules.get(index).trim();
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

    private static List<LigneImportUtilisateurDto> throwFormat() {
        throw new BusinessRuleException("FORMAT_FICHIER_INVALIDE",
                "Le fichier doit être un classeur Excel (.xlsx) ou un CSV");
    }

    private byte[] modeleCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append(String.join(";", ENTETES)).append("\r\n");
        builder.append(String.join(";", EXEMPLE)).append("\r\n");
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] modeleExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = workbook.createSheet("Utilisateurs");
            Row entete = feuille.createRow(0);
            for (int i = 0; i < ENTETES.size(); i++) {
                entete.createCell(i).setCellValue(ENTETES.get(i));
            }
            Row exemple = feuille.createRow(1);
            for (int i = 0; i < EXEMPLE.size(); i++) {
                exemple.createCell(i).setCellValue(EXEMPLE.get(i));
            }
            workbook.write(sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("MODELE_ECHOUE", "Le modèle n'a pas pu être généré");
        }
    }
}
