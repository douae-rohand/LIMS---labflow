package com.backend.integration.minio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Service de stockage de fichiers via MinIO (compatible S3).
 *
 * <p>Utilisé pour : rapports PDF, photos d'échantillons, documents qualité.
 *
 * <p>TODO: ajouter la dépendance MinIO SDK ({@code io.minio:minio}) dans le pom.xml
 * et implémenter les appels réels. Cette classe est un stub compilable.
 *
 * <pre>
 * // Dépendance à ajouter :
 * // &lt;dependency&gt;
 * //   &lt;groupId&gt;io.minio&lt;/groupId&gt;
 * //   &lt;artifactId&gt;minio&lt;/artifactId&gt;
 * //   &lt;version&gt;8.5.12&lt;/version&gt;
 * // &lt;/dependency&gt;
 * </pre>
 */
@Slf4j
@Service
public class StockageFichierService {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.bucket:lims-documents}")
    private String defaultBucket;

    // TODO: injecter le client MinIO après ajout de la dépendance
    // private MinioClient minioClient;

    /**
     * Téléverse un fichier et retourne son URL publique ou présignée.
     *
     * @param objectName nom de l'objet dans le bucket (ex. "rapports/RAP-0001.pdf")
     * @param contenu    flux du fichier
     * @param taille     taille en octets (-1 si inconnue)
     * @param contentType type MIME (ex. "application/pdf")
     * @return URL d'accès au fichier
     */
    public String televerser(String objectName, InputStream contenu, long taille, String contentType) {
        log.info("Téléversement MinIO : bucket={}, object={}", defaultBucket, objectName);
        // TODO: implémenter avec MinioClient
        // minioClient.putObject(PutObjectArgs.builder()
        //     .bucket(defaultBucket).object(objectName).stream(contenu, taille, -1)
        //     .contentType(contentType).build());
        return endpoint + "/" + defaultBucket + "/" + objectName;
    }

    /**
     * Télécharge un fichier depuis MinIO.
     *
     * @param objectName nom de l'objet dans le bucket
     * @return flux du fichier
     */
    public InputStream telecharger(String objectName) {
        log.info("Téléchargement MinIO : bucket={}, object={}", defaultBucket, objectName);
        // TODO: implémenter avec MinioClient
        // return minioClient.getObject(GetObjectArgs.builder()
        //     .bucket(defaultBucket).object(objectName).build());
        throw new UnsupportedOperationException("MinIO SDK non encore configuré — voir TODO dans StockageFichierService");
    }

    /**
     * Supprime un fichier du bucket.
     */
    public void supprimer(String objectName) {
        log.info("Suppression MinIO : bucket={}, object={}", defaultBucket, objectName);
        // TODO: implémenter avec MinioClient
    }

    /**
     * Génère une URL présignée valable {@code expirationMinutes} minutes.
     */
    public String genererUrlPresignee(String objectName, int expirationMinutes) {
        // TODO: implémenter avec MinioClient
        return endpoint + "/" + defaultBucket + "/" + objectName + "?presigned=true";
    }
}
