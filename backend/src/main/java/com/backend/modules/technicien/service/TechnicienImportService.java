package com.backend.modules.technicien.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.technicien.dto.*;
import com.backend.modules.validation.repository.ValidationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service d'import CSV de résultats d'essai pour le technicien.
 *
 * <h2>Format CSV</h2>
 * <pre>
 * code_ligne;valeur;commentaire
 * LE-0001;1.23;Commentaire facultatif
 * </pre>
 * <ul>
 *   <li>Séparateur : {@code ;} ou {@code ,} — détecté automatiquement.</li>
 *   <li>Encodage : UTF-8 (BOM toléré).</li>
 *   <li>En-tête : première ligne ignorée si elle commence par {@code code_ligne}.</li>
 * </ul>
 *
 * <h2>Protections</h2>
 * <ul>
 *   <li>Injection de formules : rejet si la valeur commence par {@code = + - @}.</li>
 *   <li>Nom du fichier jamais loggé.</li>
 *   <li>Extension vérifiée ({@code .csv}).</li>
 *   <li>Content-type vérifié ({@code text/csv} ou {@code text/plain}).</li>
 *   <li>Taille max : {@code lims.technicien.import.taille-max-octets} (défaut 1 Mo).</li>
 *   <li>Nombre max de lignes : {@code lims.technicien.import.lignes-max} (défaut 500).</li>
 * </ul>
 *
 * <h2>Règles métier par ligne</h2>
 * Identiques à {@link TechnicienService#saisirResultat} :
 * <ol>
 *   <li>code_ligne correspond à un essai appartenant au technicien courant.</li>
 *   <li>Statut EN_COURS.</li>
 *   <li>Pas de validation Responsable.</li>
 *   <li>Motif obligatoire si résultat déjà saisi.</li>
 * </ol>
 *
 * <h2>Confirmer : tout ou rien</h2>
 * Une seule ligne invalide entraîne le rollback complet. L'audit ne contient
 * que le nombre de lignes importées, jamais les valeurs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicienImportService {

    private static final String COL_CODE = "code_ligne";
    private static final String COL_VALEUR = "valeur";
    private static final String COL_COMMENTAIRE = "commentaire";

    /** Préfixes de formule à rejeter (injection protection). */
    private static final String FORMULA_CHARS = "=+-@";

    private final LigneEssaiRepository ligneEssaiRepository;
    private final ValidationRepository validationRepository;
    private final JournalAuditService journalAuditService;
    private final TechnicienService technicienService; // pour réutiliser les helpers de mapping

    @Value("${lims.technicien.import.taille-max-octets:1048576}")
    private long tailleMaxOctets;

    @Value("${lims.technicien.import.lignes-max:500}")
    private int lignesMax;

    // =========================================================================
    // Aperçu (lecture seule, pas d'écriture)
    // =========================================================================

    /**
     * Parse et valide le CSV sans aucune écriture en base.
     * Retourne un aperçu avec les erreurs et les lignes valides.
     */
    public ImportApercuResponse apercu(MultipartFile fichier) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        Long moi = principal.getId();

        validerFichier(fichier);

        List<LigneCsv> lignesCsv = parseCsv(fichier);
        List<ImportLigneErreurDto> erreurs = new ArrayList<>();
        List<ImportApercuLigneDto> apercu = new ArrayList<>();

        // Index des lignes d'essai du technicien par code (une seule requête)
        Map<String, LigneEssai> lignesParCode = chargerLignesParCode(moi);

        for (LigneCsv csv : lignesCsv) {
            List<ImportLigneErreurDto> erreursLigne = validerLigne(csv, lignesParCode);
            erreurs.addAll(erreursLigne);
            if (erreursLigne.isEmpty()) {
                LigneEssai le = lignesParCode.get(csv.codeLigne());
                String niveauConformite = TechnicienService.calculerNiveauConformite(
                        ligneAvecValeur(le, csv.valeur()));
                apercu.add(ImportApercuLigneDto.builder()
                        .numeroLigne(csv.numeroLigne())
                        .codeLigne(csv.codeLigne())
                        .valeur(csv.valeur())
                        .commentaire(csv.commentaire())
                        .niveauConformite(niveauConformite)
                        .build());
            }
        }

        return ImportApercuResponse.builder()
                .lignesLues(lignesCsv.size())
                .lignesOk(apercu.size())
                .lignesErreur(erreurs.size())
                .pret(erreurs.isEmpty())
                .erreurs(erreurs)
                .apercu(apercu)
                .build();
    }

    // =========================================================================
    // Confirmer (écriture transactionnelle, tout ou rien)
    // =========================================================================

    /**
     * Applique l'import en une transaction : tout ou rien.
     * Une ligne invalide → rollback complet + liste des erreurs en réponse 422.
     */
    @Transactional
    public ImportConfirmerResponse confirmer(MultipartFile fichier) {
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        Long moi = principal.getId();
        String acteur = principal.getEmail();
        Long labId = principal.getLaboratoireId();

        validerFichier(fichier);

        List<LigneCsv> lignesCsv = parseCsv(fichier);
        Map<String, LigneEssai> lignesParCode = chargerLignesParCode(moi);

        // Validation complète avant toute écriture
        List<ImportLigneErreurDto> erreurs = new ArrayList<>();
        for (LigneCsv csv : lignesCsv) {
            erreurs.addAll(validerLigne(csv, lignesParCode));
        }

        if (!erreurs.isEmpty()) {
            // Rollback via BusinessRuleException — la transaction est annulée
            throw new ImportValidationException(erreurs);
        }

        // Toutes les lignes sont valides : écriture
        int count = 0;
        for (LigneCsv csv : lignesCsv) {
            LigneEssai le = lignesParCode.get(csv.codeLigne());
            Boolean conformiteCalc = TechnicienService.calculerConformiteBoolean(
                    csv.valeur(), le.getEssai());

            int updated = ligneEssaiRepository.saisirResultat(
                    le.getId(), moi, csv.valeur(), csv.commentaire(), conformiteCalc, Instant.now());
            if (updated == 0) {
                // Modification concurrente entre validation et écriture
                throw new BusinessRuleException("IMPORT_CONFLICT",
                        "Conflit de mise à jour sur la ligne " + csv.codeLigne()
                                + ". Relancez l'import.");
            }
            count++;
        }

        // Audit global — nombre de lignes seulement, jamais les valeurs
        journalAuditService.enregistrer(
                "IMPORT_RESULTATS", "LigneEssai", null, acteur,
                null, count + " lignes importées", null, labId);

        return ImportConfirmerResponse.builder()
                .lignesImportees(count)
                .erreurs(List.of())
                .build();
    }

    // =========================================================================
    // Validation fichier
    // =========================================================================

    private void validerFichier(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessRuleException("IMPORT_FICHIER_VIDE",
                    "Le fichier CSV est obligatoire et ne peut pas être vide.");
        }

        // Taille max (sans logguer le nom du fichier)
        if (fichier.getSize() > tailleMaxOctets) {
            throw new BusinessRuleException("IMPORT_FICHIER_TROP_GRAND",
                    "Le fichier dépasse la taille maximale autorisée ("
                            + (tailleMaxOctets / 1024) + " Ko).");
        }

        // Extension (sans logguer le nom)
        String originalFilename = fichier.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new BusinessRuleException("IMPORT_FORMAT_INVALIDE",
                    "Seuls les fichiers .csv sont acceptés.");
        }

        // Content-type
        String contentType = fichier.getContentType();
        if (contentType != null
                && !contentType.startsWith("text/csv")
                && !contentType.startsWith("text/plain")
                && !contentType.startsWith("application/csv")
                && !contentType.startsWith("application/octet-stream")) {
            throw new BusinessRuleException("IMPORT_FORMAT_INVALIDE",
                    "Le type de contenu du fichier n'est pas reconnu comme CSV.");
        }
    }

    // =========================================================================
    // Parsing CSV
    // =========================================================================

    private List<LigneCsv> parseCsv(MultipartFile fichier) {
        List<LigneCsv> result = new ArrayList<>();

        // Décodeur strict UTF-8 (rejette les octets invalides)
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(fichier.getInputStream(), decoder))) {

            String premiereLigne = reader.readLine();
            if (premiereLigne == null) {
                throw new BusinessRuleException("IMPORT_FICHIER_VIDE", "Le fichier CSV est vide.");
            }

            // Supprimer BOM UTF-8 éventuel
            if (premiereLigne.startsWith("\uFEFF")) {
                premiereLigne = premiereLigne.substring(1);
            }

            // Détection du séparateur sur la première ligne
            char sep = detecterSeparateur(premiereLigne);

            // Déterminer si la première ligne est un en-tête
            boolean avecEntete = premiereLigne.toLowerCase().contains("code_ligne");
            int numeroLigneCsv = avecEntete ? 1 : 0;

            String ligne = avecEntete ? reader.readLine() : premiereLigne;

            while (ligne != null) {
                numeroLigneCsv++;
                if (result.size() >= lignesMax) {
                    throw new BusinessRuleException("IMPORT_TROP_DE_LIGNES",
                            "Le fichier dépasse le nombre maximal de lignes autorisé ("
                                    + lignesMax + ").");
                }

                String[] cols = splitCsv(ligne, sep);
                if (cols.length < 2) {
                    result.add(LigneCsv.erreur(numeroLigneCsv));
                } else {
                    String codeLigne = cols[0].trim();
                    String valeur = cols[1].trim();
                    String commentaire = cols.length >= 3 ? cols[2].trim() : null;

                    // Protection injection formules
                    if (!valeur.isEmpty() && FORMULA_CHARS.indexOf(valeur.charAt(0)) >= 0) {
                        result.add(LigneCsv.avecFormulaInjection(numeroLigneCsv, codeLigne));
                    } else {
                        result.add(new LigneCsv(numeroLigneCsv, codeLigne, valeur, commentaire, false, false));
                    }
                }
                ligne = reader.readLine();
            }

        } catch (java.nio.charset.MalformedInputException e) {
            throw new BusinessRuleException("IMPORT_ENCODAGE_INVALIDE",
                    "Le fichier n'est pas en UTF-8 valide. Vérifiez l'encodage.");
        } catch (IOException e) {
            throw new BusinessRuleException("IMPORT_LECTURE_IMPOSSIBLE",
                    "Impossible de lire le fichier CSV.");
        }

        if (result.isEmpty()) {
            throw new BusinessRuleException("IMPORT_FICHIER_VIDE",
                    "Le fichier CSV ne contient aucune donnée.");
        }

        return result;
    }

    private char detecterSeparateur(String ligne) {
        long nbPointVirgule = ligne.chars().filter(c -> c == ';').count();
        long nbVirgule = ligne.chars().filter(c -> c == ',').count();
        return nbPointVirgule >= nbVirgule ? ';' : ',';
    }

    /** Split simple sans gestion des guillemets (valeurs CSV simples sans RFC 4180 complet). */
    private String[] splitCsv(String ligne, char sep) {
        return ligne.split(String.valueOf(sep), -1);
    }

    // =========================================================================
    // Validation métier d'une ligne
    // =========================================================================

    private List<ImportLigneErreurDto> validerLigne(LigneCsv csv, Map<String, LigneEssai> lignesParCode) {
        List<ImportLigneErreurDto> erreurs = new ArrayList<>();

        // Ligne mal formée (trop peu de colonnes)
        if (csv.malFormee()) {
            erreurs.add(ImportLigneErreurDto.builder()
                    .numeroLigne(csv.numeroLigne())
                    .colonne("(ligne)")
                    .code("IMPORT_LIGNE_MAL_FORMEE")
                    .message("La ligne doit contenir au moins 2 colonnes : code_ligne et valeur.")
                    .build());
            return erreurs;
        }

        // Injection de formule
        if (csv.formulaInjection()) {
            erreurs.add(ImportLigneErreurDto.builder()
                    .numeroLigne(csv.numeroLigne())
                    .colonne(COL_VALEUR)
                    .code("IMPORT_FORMULE_INTERDITE")
                    .message("La valeur ne peut pas commencer par = + - @ (protection contre l'injection de formules).")
                    .build());
            return erreurs;
        }

        // code_ligne vide
        if (csv.codeLigne() == null || csv.codeLigne().isBlank()) {
            erreurs.add(ImportLigneErreurDto.builder()
                    .numeroLigne(csv.numeroLigne())
                    .colonne(COL_CODE)
                    .code("IMPORT_CODE_VIDE")
                    .message("Le code de ligne est obligatoire.")
                    .build());
            return erreurs;
        }

        // valeur vide
        if (csv.valeur() == null || csv.valeur().isBlank()) {
            erreurs.add(ImportLigneErreurDto.builder()
                    .numeroLigne(csv.numeroLigne())
                    .colonne(COL_VALEUR)
                    .code("IMPORT_VALEUR_VIDE")
                    .message("La valeur est obligatoire.")
                    .build());
        }

        // Appartenance au technicien
        LigneEssai le = lignesParCode.get(csv.codeLigne());
        if (le == null) {
            erreurs.add(ImportLigneErreurDto.builder()
                    .numeroLigne(csv.numeroLigne())
                    .colonne(COL_CODE)
                    .code("IMPORT_LIGNE_INTROUVABLE")
                    .message("Aucun essai en cours ne correspond à ce code pour le technicien courant.")
                    .build());
            return erreurs; // pas besoin de continuer si la ligne n'existe pas
        }

        // Statut EN_COURS requis
        if (le.getStatut() != StatutEssai.EN_COURS) {
            erreurs.add(ImportLigneErreurDto.builder()
                    .numeroLigne(csv.numeroLigne())
                    .colonne(COL_CODE)
                    .code("IMPORT_ESSAI_NON_EN_COURS")
                    .message("L'essai n'est pas EN_COURS (statut actuel : "
                            + (le.getStatut() == null ? "inconnu" : le.getStatut().name()) + ").")
                    .build());
        }

        // Validation Responsable déjà présente
        if (validationRepository.existsValidationResponsableValidee(le.getId())) {
            erreurs.add(ImportLigneErreurDto.builder()
                    .numeroLigne(csv.numeroLigne())
                    .colonne(COL_CODE)
                    .code("IMPORT_DEJA_VALIDE_RESPONSABLE")
                    .message("Le résultat a déjà été validé par le Responsable. Modification impossible.")
                    .build());
        }

        // Motif obligatoire si résultat déjà saisi
        boolean dejaaSaisi = le.getValeur() != null && !le.getValeur().isBlank();
        if (dejaaSaisi && (csv.commentaire() == null || csv.commentaire().isBlank())) {
            erreurs.add(ImportLigneErreurDto.builder()
                    .numeroLigne(csv.numeroLigne())
                    .colonne(COL_COMMENTAIRE)
                    .code("IMPORT_MOTIF_REQUIS")
                    .message("Un commentaire (motif) est obligatoire pour modifier un résultat déjà saisi.")
                    .build());
        }

        return erreurs;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Charge en une seule requête tous les essais EN_COURS du technicien,
     * indexés par code de ligne.
     */
    private Map<String, LigneEssai> chargerLignesParCode(Long technicienId) {
        // findByTechnicienId sans pagination — acceptable car limité aux lignes EN_COURS
        // On utilise la méthode existante puis on filtre (nombre limité pour un technicien)
        Map<String, LigneEssai> map = new HashMap<>();
        // Requête : toutes les lignes du technicien (pas de filtre statut pour permettre la validation)
        // La validation du statut se fait dans validerLigne
        ligneEssaiRepository.findByTechnicienIdAll(technicienId)
                .forEach(le -> map.put(le.getCode(), le));
        return map;
    }

    /**
     * Crée un objet LigneEssai virtuel avec la valeur CSV pour le calcul de conformité
     * (sans toucher l'entité persistée).
     */
    private LigneEssai ligneAvecValeur(LigneEssai original, String valeur) {
        return LigneEssai.builder()
                .id(original.getId())
                .code(original.getCode())
                .statut(original.getStatut())
                .essai(original.getEssai())
                .echantillon(original.getEchantillon())
                .demande(original.getDemande())
                .technicienId(original.getTechnicienId())
                .valeur(valeur)
                .conformite(original.getConformite())
                .montant(original.getMontant())
                .build();
    }

    // =========================================================================
    // Types internes
    // =========================================================================

    /**
     * Exception interne utilisée pour transmettre la liste d'erreurs de validation
     * et déclencher un rollback propre en cas d'import invalide.
     *
     * <p>Capturée dans le contrôleur pour retourner une réponse 422 structurée.</p>
     */
    public static class ImportValidationException extends BusinessRuleException {
        private final List<ImportLigneErreurDto> erreurs;

        public ImportValidationException(List<ImportLigneErreurDto> erreurs) {
            super("IMPORT_VALIDATION_ECHOUEE",
                    erreurs.size() + " erreur(s) détectée(s). Aucune ligne n'a été importée.");
            this.erreurs = List.copyOf(erreurs);
        }

        public List<ImportLigneErreurDto> getErreurs() {
            return erreurs;
        }
    }

    /**
     * Représentation interne d'une ligne parsée du CSV.
     */
    private record LigneCsv(
            int numeroLigne,
            String codeLigne,
            String valeur,
            String commentaire,
            boolean malFormee,
            boolean formulaInjection
    ) {
        static LigneCsv erreur(int num) {
            return new LigneCsv(num, null, null, null, true, false);
        }

        static LigneCsv avecFormulaInjection(int num, String codeLigne) {
            return new LigneCsv(num, codeLigne, null, null, false, true);
        }
    }
}
