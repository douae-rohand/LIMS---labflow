package com.backend.modules.plateforme.service;

import com.backend.common.tenant.TenantExecutor;
import com.backend.integration.sendgrid.SendGridMailClient;
import com.backend.integration.smtp.SmtpMailClient;
import com.backend.modules.plateforme.dto.TableauBordDto;
import com.backend.modules.plateforme.dto.TableauBordDto.ActiviteLaboratoire;
import com.backend.modules.plateforme.dto.TableauBordDto.AlerteIntegration;
import com.backend.modules.plateforme.dto.TableauBordDto.EtatService;
import com.backend.modules.plateforme.dto.TableauBordDto.PointSerie;
import com.backend.modules.plateforme.entity.DemandeIntegration;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TableauBordService {

    static final String MESSAGE_N8N =
            "Les échecs de workflows n8n ne sont pas enregistrés : l'envoi du webhook journalise l'erreur sans la conserver. Aucune alerte n'est calculée.";

    private static final Duration CACHE = Duration.ofSeconds(45);
    private static final Duration TIMEOUT_TENANT = Duration.ofSeconds(3);
    private static final int SEUIL_JOURS = 7;
    private static final Pattern SCHEMA = Pattern.compile("lims_[a-z0-9_]+");
    private static final String SQL_AGREGAT = """
            SELECT cle, nb, texte FROM (
                SELECT 'TOTAL' AS cle, COUNT(*) AS nb, '' AS texte FROM demande
                UNION ALL
                SELECT CONCAT('STATUT:', statut), COUNT(*), '' FROM demande GROUP BY statut
                UNION ALL
                SELECT CONCAT('DOMAINE:', d.code), COUNT(DISTINCT le.demande_id), ''
                FROM domaine d
                LEFT JOIN essai e ON e.domaine_id = d.id_domaine
                LEFT JOIN ligne_essai le ON le.essai_id = e.id_essai
                GROUP BY d.code
                UNION ALL
                SELECT CONCAT('MOIS:', DATE_FORMAT(date_soumission, '%Y-%m')), COUNT(*), ''
                FROM demande
                WHERE date_soumission IS NOT NULL
                  AND date_soumission >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 12 MONTH)
                GROUP BY DATE_FORMAT(date_soumission, '%Y-%m')
                UNION ALL
                SELECT 'TAILLE', IFNULL(SUM(data_length + index_length), 0), ''
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                UNION ALL
                SELECT 'VERSION', IFNULL(MAX(installed_rank), 0),
                       IFNULL(SUBSTRING_INDEX(GROUP_CONCAT(version ORDER BY installed_rank DESC SEPARATOR ','), ',', 1), '')
                FROM flyway_schema_history
                WHERE success = 1
            ) agregat
            """;

    private final UtilisateurRepository utilisateurRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final DemandeIntegrationRepository demandeIntegrationRepository;
    private final TenantExecutor tenantExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final SendGridMailClient sendGridMailClient;
    private final SmtpMailClient smtpMailClient;
    private final String n8nBaseUrl;
    private final String minioEndpoint;
    private final ExecutorService executants = Executors.newFixedThreadPool(4, runnable -> {
        Thread fil = new Thread(runnable, "tableau-bord");
        fil.setDaemon(true);
        return fil;
    });
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final Object verrou = new Object();
    private volatile Instant cacheJusqua;
    private volatile TableauBordDto cache;

    public TableauBordService(
            UtilisateurRepository utilisateurRepository,
            LaboratoireRepository laboratoireRepository,
            DemandeIntegrationRepository demandeIntegrationRepository,
            TenantExecutor tenantExecutor,
            JdbcTemplate jdbcTemplate,
            SendGridMailClient sendGridMailClient,
            SmtpMailClient smtpMailClient,
            @Value("${n8n.webhook-base-url:}") String n8nBaseUrl,
            @Value("${minio.endpoint:}") String minioEndpoint) {
        this.utilisateurRepository = utilisateurRepository;
        this.laboratoireRepository = laboratoireRepository;
        this.demandeIntegrationRepository = demandeIntegrationRepository;
        this.tenantExecutor = tenantExecutor;
        this.jdbcTemplate = jdbcTemplate;
        this.sendGridMailClient = sendGridMailClient;
        this.smtpMailClient = smtpMailClient;
        this.n8nBaseUrl = n8nBaseUrl;
        this.minioEndpoint = minioEndpoint;
    }

    @PreDestroy
    void fermer() {
        executants.shutdownNow();
    }

    public TableauBordDto obtenir() {
        TableauBordDto courant = cache;
        if (courant != null && cacheJusqua != null && Instant.now().isBefore(cacheJusqua)) {
            return courant;
        }
        synchronized (verrou) {
            courant = cache;
            if (courant != null && cacheJusqua != null && Instant.now().isBefore(cacheJusqua)) {
                return courant;
            }
            TableauBordDto construit = construire();
            cache = construit;
            cacheJusqua = Instant.now().plus(CACHE);
            return construit;
        }
    }

    private TableauBordDto construire() {
        long actifs = utilisateurRepository.countByActifTrue();
        List<PointSerie> inscriptions = inscriptions();
        List<AlerteIntegration> alertes = alertesIntegration();
        List<Laboratoire> laboratoires = laboratoireRepository.findAll();
        Map<Long, Long> comptes = comptesUtilisateurs();
        CompletableFuture<List<EtatService>> services = CompletableFuture.supplyAsync(this::services, executants);
        List<CompletableFuture<Agregat>> taches = laboratoires.stream()
                .map(laboratoire -> {
                    long utilisateurs = comptes.getOrDefault(laboratoire.getId(), 0L);
                    if (!"ACTIF".equals(laboratoire.getStatut())) {
                        return CompletableFuture.completedFuture(new Agregat(exclu(laboratoire, utilisateurs), Map.of()));
                    }
                    return CompletableFuture
                            .supplyAsync(() -> agreger(laboratoire, utilisateurs), executants)
                            .orTimeout(TIMEOUT_TENANT.toSeconds(), TimeUnit.SECONDS)
                            .exceptionally(ex -> new Agregat(inaccessible(laboratoire, utilisateurs), null));
                })
                .toList();
        List<ActiviteLaboratoire> activites = new ArrayList<>();
        Map<String, Long> domaines = new LinkedHashMap<>();
        long total = 0;
        int indisponibles = 0;
        int exclus = 0;
        for (CompletableFuture<Agregat> tache : taches) {
            Agregat agregat = tache.join();
            activites.add(agregat.activite());
            if ("EXCLU".equals(agregat.activite().couverture())) {
                exclus++;
                continue;
            }
            if (agregat.domaines() == null || agregat.activite().indisponible()) {
                indisponibles++;
                continue;
            }
            if (agregat.activite().nombre() != null) {
                total += agregat.activite().nombre();
            }
            agregat.domaines().forEach((code, nombre) -> domaines.merge(code, nombre, Long::sum));
        }
        activites.sort(Comparator.comparing(ActiviteLaboratoire::nom, Comparator.nullsLast(String::compareToIgnoreCase)));
        List<PointSerie> repartition = domaines.entrySet().stream()
                .map(entree -> new PointSerie(entree.getKey(), entree.getValue()))
                .sorted(Comparator.comparingLong(PointSerie::nombre).reversed())
                .toList();
        return new TableauBordDto(
                actifs,
                total,
                indisponibles,
                exclus,
                inscriptions,
                activites,
                repartition,
                alertes,
                MESSAGE_N8N,
                services.join());
    }

    private Agregat agreger(Laboratoire laboratoire, long utilisateurs) {
        if (!schemaValide(laboratoire.getNomSchema())) {
            return new Agregat(inaccessible(laboratoire, utilisateurs), null);
        }
        try {
            Lecture lecture = tenantExecutor.inTenant(laboratoire.getNomSchema(), this::lireAgregat);
            Long total = lecture.nombres().get("TOTAL");
            if (total == null) {
                return new Agregat(inaccessible(laboratoire, utilisateurs), null);
            }
            Map<String, Long> domaines = new LinkedHashMap<>();
            List<PointSerie> statuts = new ArrayList<>();
            List<PointSerie> mois = new ArrayList<>();
            lecture.nombres().forEach((cle, nombre) -> {
                if (cle == null) {
                    return;
                }
                if (cle.startsWith("DOMAINE:")) {
                    domaines.put(cle.substring("DOMAINE:".length()), nombre);
                } else if (cle.startsWith("STATUT:")) {
                    statuts.add(new PointSerie(cle.substring("STATUT:".length()), nombre));
                } else if (cle.startsWith("MOIS:")) {
                    mois.add(new PointSerie(cle.substring("MOIS:".length()), nombre));
                }
            });
            mois.sort(Comparator.comparing(PointSerie::cle));
            long cloturees = statuts.stream()
                    .filter(point -> "TERMINEE".equals(point.cle()))
                    .mapToLong(PointSerie::nombre)
                    .sum();
            Integer taux = total == 0 ? null : (int) Math.round(cloturees * 100.0 / total);
            Long taille = lecture.nombres().get("TAILLE");
            return new Agregat(new ActiviteLaboratoire(
                    laboratoire.getId(),
                    laboratoire.getRaisonSociale(),
                    laboratoire.getStatut(),
                    "INCLUS",
                    total,
                    statuts,
                    utilisateurs,
                    taux,
                    taille,
                    StringUtils.hasText(lecture.version()) ? lecture.version() : null,
                    "présent",
                    mois,
                    false), domaines);
        } catch (RuntimeException ex) {
            log.warn("Agrégat indisponible pour le laboratoire {} : {}", laboratoire.getId(), ex.getMessage());
            return new Agregat(inaccessible(laboratoire, utilisateurs), null);
        }
    }

    private Lecture lireAgregat() {
        return jdbcTemplate.execute((Connection connection) -> {
            Map<String, Long> compteurs = new LinkedHashMap<>();
            String version = null;
            try (PreparedStatement statement = connection.prepareStatement(SQL_AGREGAT)) {
                statement.setQueryTimeout(2);
                try (ResultSet lignes = statement.executeQuery()) {
                    while (lignes.next()) {
                        String cle = lignes.getString(1);
                        compteurs.put(cle, lignes.getLong(2));
                        if ("VERSION".equals(cle)) {
                            version = lignes.getString(3);
                        }
                    }
                }
            }
            return new Lecture(compteurs, version);
        });
    }

    private Map<Long, Long> comptesUtilisateurs() {
        Map<Long, Long> comptes = new LinkedHashMap<>();
        for (Object[] ligne : utilisateurRepository.compterParLaboratoire()) {
            if (ligne[0] instanceof Number id && ligne[1] instanceof Number nombre) {
                comptes.put(id.longValue(), nombre.longValue());
            }
        }
        return comptes;
    }

    private ActiviteLaboratoire exclu(Laboratoire laboratoire, long utilisateurs) {
        return new ActiviteLaboratoire(
                laboratoire.getId(), laboratoire.getRaisonSociale(), laboratoire.getStatut(),
                "EXCLU", null, List.of(), utilisateurs, null, null, null, "non interrogé", List.of(), false);
    }

    private ActiviteLaboratoire inaccessible(Laboratoire laboratoire, long utilisateurs) {
        return new ActiviteLaboratoire(
                laboratoire.getId(), laboratoire.getRaisonSociale(), laboratoire.getStatut(),
                "INACCESSIBLE", null, List.of(), utilisateurs, null, null, null, "inaccessible", List.of(), true);
    }

    private record Lecture(Map<String, Long> nombres, String version) {}

    private List<PointSerie> inscriptions() {
        Instant depuis = LocalDate.now(ZoneOffset.UTC).minusMonths(11).withDayOfMonth(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC);
        return tenantExecutor.inCentral(() -> jdbcTemplate.query("""
                SELECT DATE_FORMAT(date_creation, '%Y-%m') AS mois, COUNT(*) AS nb
                FROM laboratoire
                WHERE date_creation >= ?
                GROUP BY DATE_FORMAT(date_creation, '%Y-%m')
                ORDER BY mois
                """, (ligne, numero) -> new PointSerie(ligne.getString(1), ligne.getLong(2)), java.sql.Timestamp.from(depuis)));
    }

    private List<AlerteIntegration> alertesIntegration() {
        Instant seuil = Instant.now().minus(SEUIL_JOURS, ChronoUnit.DAYS);
        return demandeIntegrationRepository
                .trouverAnciennes(seuil, PageRequest.of(0, 20))
                .stream()
                .map(this::versAlerte)
                .toList();
    }

    private AlerteIntegration versAlerte(DemandeIntegration demande) {
        long jours = demande.getDateDemande() == null
                ? SEUIL_JOURS
                : Math.max(SEUIL_JOURS, ChronoUnit.DAYS.between(demande.getDateDemande(), Instant.now()));
        String nom = StringUtils.hasText(demande.getNomLaboratoire())
                ? demande.getNomLaboratoire()
                : demande.getRaisonSociale();
        return new AlerteIntegration(demande.getNumero(), nom, jours);
    }

    private List<EtatService> services() {
        return List.of(
                new EtatService("Base de données", sonderBase()),
                new EtatService("n8n", sonderHttp(origine(n8nBaseUrl, "/healthz"))),
                new EtatService("MinIO", sonderHttp(joindre(minioEndpoint, "/minio/health/live"))),
                new EtatService("SendGrid", sendGridMailClient.sonder()),
                new EtatService("SMTP", smtpMailClient.sonder())
        );
    }

    private String sonderBase() {
        try {
            Integer valeur = tenantExecutor.inCentral(() -> jdbcTemplate.execute((Connection connection) -> {
                try (PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
                    statement.setQueryTimeout(2);
                    try (ResultSet lignes = statement.executeQuery()) {
                        return lignes.next() ? lignes.getInt(1) : 0;
                    }
                }
            }));
            return valeur != null && valeur == 1 ? "OK" : "INDISPONIBLE";
        } catch (RuntimeException ex) {
            log.warn("Sonde base de données indisponible");
            return "INDISPONIBLE";
        }
    }

    private String sonderHttp(String url) {
        if (!StringUtils.hasText(url)) {
            return "NON_CONFIGURE";
        }
        try {
            HttpRequest requete = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            int statut = http.send(requete, HttpResponse.BodyHandlers.discarding()).statusCode();
            return statut >= 200 && statut < 300 ? "OK" : "INDISPONIBLE";
        } catch (Exception ex) {
            return "INDISPONIBLE";
        }
    }

    private static String origine(String base, String chemin) {
        if (!StringUtils.hasText(base)) {
            return null;
        }
        try {
            URI uri = URI.create(base.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            String racine = uri.getScheme() + "://" + uri.getHost() + (port > 0 ? ":" + port : "");
            return racine + chemin;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String joindre(String base, String chemin) {
        if (!StringUtils.hasText(base)) {
            return null;
        }
        return base.replaceAll("/$", "") + chemin;
    }

    private static boolean schemaValide(String nomSchema) {
        return nomSchema != null
                && SCHEMA.matcher(nomSchema).matches()
                && !"lims_central".equals(nomSchema);
    }

    private record Agregat(ActiviteLaboratoire activite, Map<String, Long> domaines) {}
}
