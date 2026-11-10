package com.backend.modules.rapport.signature;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.integration.minio.StockageFichierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Image de la signature manuscrite d'un responsable, imprimée dans le cadre de signature
 * des rapports. Un fichier PNG par utilisateur, dans {@code <stockage>/signatures/<id>.png} :
 * aucune table n'est nécessaire.
 *
 * <p>À l'import, l'image est réduite (600 × 200 au plus) et son fond clair devient
 * transparent, pour qu'une signature scannée ou photographiée s'imprime proprement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureManuscriteService {

    private static final long TAILLE_MAX = 2L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/png", "image/jpeg", "image/jpg");
    private static final int LARGEUR_MAX = 600;
    private static final int HAUTEUR_MAX = 200;
    /** Au-dessus de ce niveau (0-255) sur les trois canaux, un pixel est considéré comme fond. */
    private static final int SEUIL_FOND = 225;

    private final StockageFichierService stockageFichierService;
    private final JournalAuditService journalAuditService;

    public record Statut(boolean presente, Instant miseAJour) {
    }

    public Statut statut(Long utilisateurId) {
        Path fichier = chemin(utilisateurId);
        if (!Files.exists(fichier)) {
            return new Statut(false, null);
        }
        try {
            return new Statut(true, Files.getLastModifiedTime(fichier).toInstant());
        } catch (IOException ex) {
            return new Statut(true, null);
        }
    }

    /** Image PNG de la signature, si elle a été importée. */
    public Optional<byte[]> lire(Long utilisateurId) {
        if (utilisateurId == null) {
            return Optional.empty();
        }
        Path fichier = chemin(utilisateurId);
        if (!Files.exists(fichier)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(fichier));
        } catch (IOException ex) {
            log.warn("Signature manuscrite illisible pour userId={} : {}", utilisateurId, ex.getMessage());
            return Optional.empty();
        }
    }

    public Statut enregistrer(Long utilisateurId, String acteurEmail, Long laboratoireId, MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessRuleException("SIGNATURE_ABSENTE", "Choisissez l'image de votre signature.");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new BusinessRuleException("SIGNATURE_TROP_LOURDE", "L'image ne doit pas dépasser 2 Mo.");
        }
        String type = fichier.getContentType() == null ? "" : fichier.getContentType().toLowerCase(Locale.ROOT);
        if (!TYPES.contains(type)) {
            throw new BusinessRuleException("SIGNATURE_FORMAT", "Formats acceptés : PNG ou JPG.");
        }
        BufferedImage image;
        try (InputStream entree = fichier.getInputStream()) {
            image = ImageIO.read(entree);
        } catch (IOException ex) {
            image = null;
        }
        if (image == null) {
            throw new BusinessRuleException("SIGNATURE_ILLISIBLE", "Ce fichier n'est pas une image lisible.");
        }
        byte[] png = preparer(image);
        try (InputStream contenu = new ByteArrayInputStream(png)) {
            stockageFichierService.televerser(cle(utilisateurId), contenu, png.length, "image/png");
        } catch (IOException ex) {
            throw new BusinessRuleException("SIGNATURE_STOCKAGE", "La signature n'a pas pu être enregistrée.");
        }
        journalAuditService.enregistrer("IMPORTER_SIGNATURE_MANUSCRITE", "Utilisateur", utilisateurId, acteurEmail,
                null, null, null, laboratoireId);
        return statut(utilisateurId);
    }

    public Statut supprimer(Long utilisateurId, String acteurEmail, Long laboratoireId) {
        stockageFichierService.supprimer(cle(utilisateurId));
        journalAuditService.enregistrer("SUPPRIMER_SIGNATURE_MANUSCRITE", "Utilisateur", utilisateurId, acteurEmail,
                null, null, null, laboratoireId);
        return new Statut(false, null);
    }

    /** Réduit l'image et rend son fond clair transparent. */
    static byte[] preparer(BufferedImage source) {
        double echelle = Math.min(1.0, Math.min((double) LARGEUR_MAX / source.getWidth(),
                (double) HAUTEUR_MAX / source.getHeight()));
        int largeur = Math.max(1, (int) Math.round(source.getWidth() * echelle));
        int hauteur = Math.max(1, (int) Math.round(source.getHeight() * echelle));
        BufferedImage resultat = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resultat.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source, 0, 0, largeur, hauteur, null);
        g.dispose();
        for (int x = 0; x < largeur; x++) {
            for (int y = 0; y < hauteur; y++) {
                int argb = resultat.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int v = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                if (r > SEUIL_FOND && v > SEUIL_FOND && b > SEUIL_FOND) {
                    resultat.setRGB(x, y, 0x00FFFFFF);
                }
            }
        }
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            ImageIO.write(resultat, "png", sortie);
            return sortie.toByteArray();
        } catch (IOException ex) {
            throw new BusinessRuleException("SIGNATURE_CONVERSION", "L'image n'a pas pu être convertie.");
        }
    }

    private static String cle(Long utilisateurId) {
        return "signatures/" + utilisateurId + ".png";
    }

    private Path chemin(Long utilisateurId) {
        return stockageFichierService.cheminAbsolu(cle(utilisateurId));
    }
}
