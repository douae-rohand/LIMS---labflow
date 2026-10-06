package com.backend.integration.minio;

import com.backend.common.exception.BusinessRuleException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Stockage des fichiers d'intégration et documents métier.
 * Implémentation locale (disque) utilisable immédiatement ; MinIO reste
 * configurable via {@code minio.*} pour une bascule ultérieure.
 */
@Slf4j
@Service
public class StockageFichierService {

    @Value("${app.storage.path:${user.home}/lims-documents}")
    private String storagePath;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.bucket:lims-documents}")
    private String defaultBucket;

    private Path racine;

    @PostConstruct
    void initialiser() {
        racine = Path.of(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(racine);
            log.info("Stockage fichiers initialisé : {}", racine);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'initialiser le répertoire de stockage : " + racine, ex);
        }
    }

    public String televerser(String objectName, InputStream contenu, long taille, String contentType) {
        Path destination = resoudre(objectName);
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(contenu, destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier stocké : object={}, taille={}, type={}", objectName, taille, contentType);
            return objectName;
        } catch (IOException ex) {
            throw new BusinessRuleException("STOCKAGE_ECHOUE",
                    "Impossible d'enregistrer le fichier : " + ex.getMessage());
        }
    }

    public InputStream telecharger(String objectName) {
        Path source = resoudre(objectName);
        if (!Files.exists(source)) {
            throw new BusinessRuleException("FICHIER_INTROUVABLE",
                    "Le fichier demandé n'existe plus sur le serveur");
        }
        try {
            return Files.newInputStream(source);
        } catch (IOException ex) {
            throw new BusinessRuleException("LECTURE_FICHIER_ECHOUEE",
                    "Impossible de lire le fichier : " + ex.getMessage());
        }
    }

    public Path cheminAbsolu(String objectName) {
        return resoudre(objectName);
    }

    public void supprimer(String objectName) {
        Path cible = resoudre(objectName);
        try {
            Files.deleteIfExists(cible);
        } catch (IOException ex) {
            log.warn("Suppression fichier échouée : object={}, erreur={}", objectName, ex.getMessage());
        }
    }

    public String genererUrlPresignee(String objectName, int expirationMinutes) {
        return endpoint + "/" + defaultBucket + "/" + objectName + "?ttl=" + expirationMinutes;
    }

    private Path resoudre(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new BusinessRuleException("CHEMIN_FICHIER_INVALIDE", "Nom d'objet vide");
        }
        Path cible = racine.resolve(objectName).normalize();
        if (!cible.startsWith(racine)) {
            throw new BusinessRuleException("CHEMIN_FICHIER_INVALIDE", "Chemin de fichier hors zone de stockage");
        }
        return cible;
    }
}
