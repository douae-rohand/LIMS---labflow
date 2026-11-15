package com.backend.modules.accueil.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.config.SchemaConstants;
import com.backend.common.tenant.TenantDataSourceRegistry;
import com.backend.modules.notification.service.NotificationsLaboratoire;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Demandes de patients sur site : acceptées d'office à l'accueil, leurs échantillons sont attendus
 * sous 48 h. Passé ce délai, une demande sans aucun échantillon reçu et sans aucun paiement est
 * refusée automatiquement et sa facture (jamais payée) est annulée par un avoir. Un paiement, même partiel,
 * la maintient en cours.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelaiEchantillonsPatient {

    public static final Duration DELAI = Duration.ofHours(48);
    static final String MOTIF = "Aucun échantillon reçu dans les 48 h suivant l'enregistrement, et aucun paiement.";

    private final TenantDataSourceRegistry registry;
    private final JournalAuditService journalAuditService;
    private final NotificationsLaboratoire notificationsLaboratoire;

    @Value("${app.accueil.delai-echantillons.actif:true}")
    private boolean actif;

    private record Laboratoire(Long id, String schema) {
    }

    private record DemandeEchue(Long id, String numero, String statut, Long decideurId) {
    }

    /** Toutes les 15 minutes (premier passage une minute après le démarrage). */
    @Scheduled(initialDelay = 60_000, fixedDelay = 900_000)
    public void refuserDemandesEchues() {
        if (!actif) {
            return;
        }
        for (Laboratoire laboratoire : laboratoires()) {
            try {
                traiter(laboratoire);
            } catch (Exception ex) {
                log.error("Délai échantillons patients : laboratoire {} non traité : {}", laboratoire.schema(), ex.getMessage());
            }
        }
    }

    private void traiter(Laboratoire laboratoire) throws SQLException {
        String s = "`" + laboratoire.schema() + "`";
        Timestamp limite = Timestamp.from(Instant.now().minus(DELAI));
        List<DemandeEchue> echues = new ArrayList<>();
        try (Connection connexion = registry.getCentral().getConnection()) {
            try (PreparedStatement requete = connexion.prepareStatement(
                    "SELECT d.id_demande, d.numero, d.statut, d.decideur_id FROM " + s + ".demande d"
                            + " WHERE d.client_id IS NULL AND d.patient_id IS NOT NULL"
                            + " AND d.statut IN ('ACCEPTEE', 'EN_COURS')"
                            + " AND COALESCE(d.date_decision, d.date_soumission) < ?"
                            + " AND NOT EXISTS (SELECT 1 FROM " + s + ".echantillon e WHERE e.demande_id = d.id_demande)"
                            // Paiement net nul : les encaissements annulés (contre-passation) ne comptent pas.
                            + " AND COALESCE((SELECT SUM(p.montant) FROM " + s + ".paiement p JOIN " + s + ".facture f"
                            + "   ON f.id_facture = p.facture_id WHERE f.demande_id = d.id_demande), 0) <= 0")) {
                requete.setTimestamp(1, limite);
                try (ResultSet rs = requete.executeQuery()) {
                    while (rs.next()) {
                        echues.add(new DemandeEchue(rs.getLong(1), rs.getString(2), rs.getString(3),
                                rs.getObject(4) == null ? null : rs.getLong(4)));
                    }
                }
            }
            for (DemandeEchue demande : echues) {
                refuser(connexion, laboratoire.schema(), demande, laboratoire.id());
            }
        }
    }

    /** Refus et annulation de la facture impayée par un avoir, dans une même transaction. */
    private void refuser(Connection connexion, String schema, DemandeEchue demande, Long laboratoireId) throws SQLException {
        String s = "`" + schema + "`";
        String factures;
        connexion.setAutoCommit(false);
        try {
            factures = numerosFactures(connexion, s, demande.id());
            // Une facture ne se supprime jamais : elle est annulée par un avoir numéroté.
            for (Long factureId : facturesNonAnnulees(connexion, s, demande.id())) {
                executer(connexion, "UPDATE " + s + ".facture SET statut = 'ANNULEE', numero_avoir = ?,"
                        + " date_annulation = ?, motif_annulation = ? WHERE id_facture = ?",
                        prochainAvoir(connexion, s), Timestamp.from(Instant.now()), MOTIF, factureId);
            }
            // Le statut est revérifié : une réception ou un paiement arrivé entre-temps l'emporte.
            int misAJour = executer(connexion, "UPDATE " + s + ".demande SET statut = 'REFUSEE', motif = ?,"
                    + " date_decision = ? WHERE id_demande = ? AND statut IN ('ACCEPTEE', 'EN_COURS')"
                    + " AND NOT EXISTS (SELECT 1 FROM " + s + ".echantillon e WHERE e.demande_id = ?)",
                    MOTIF, Timestamp.from(Instant.now()), demande.id(), demande.id());
            if (misAJour == 0) {
                connexion.rollback();
                return;
            }
            connexion.commit();
        } catch (SQLException ex) {
            connexion.rollback();
            throw ex;
        } finally {
            connexion.setAutoCommit(true);
        }
        log.info("Demande patient {} refusée automatiquement (48 h sans échantillon ni paiement)", demande.numero());
        String auteur = emailDe(connexion, demande.decideurId());
        // Même objet que les autres décisions (« schéma/Demande#id ») : le refus apparaît dans l'historique de la demande.
        journalAuditService.enregistrer("REFUS_AUTOMATIQUE_DEMANDE", schema + "/Demande", demande.id(), auteur,
                "{\"statut\":\"" + demande.statut() + "\"}",
                "{\"statut\":\"REFUSEE\",\"facturesAnnulees\":\"" + factures + "\"}", MOTIF, laboratoireId);
        notificationsLaboratoire.notifierApresCommit(RoleUtilisateur.ACCUEIL, laboratoireId, "DEMANDE_REFUSEE_DELAI",
                "Demande " + demande.numero() + " refusée automatiquement", MOTIF, "/app/suivi-demandes");
        notificationsLaboratoire.notifierApresCommit(RoleUtilisateur.RESPONSABLE, laboratoireId, "DEMANDE_REFUSEE_DELAI",
                "Demande " + demande.numero() + " refusée automatiquement", MOTIF, "/app/demandes-clients");
    }

    private static String numerosFactures(Connection connexion, String s, Long demandeId) throws SQLException {
        List<String> numeros = new ArrayList<>();
        try (PreparedStatement requete = connexion.prepareStatement(
                "SELECT numero FROM " + s + ".facture WHERE demande_id = ?")) {
            requete.setLong(1, demandeId);
            try (ResultSet rs = requete.executeQuery()) {
                while (rs.next()) {
                    numeros.add(rs.getString(1));
                }
            }
        }
        return String.join(",", numeros);
    }

    private static List<Long> facturesNonAnnulees(Connection connexion, String s, Long demandeId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement requete = connexion.prepareStatement(
                "SELECT id_facture FROM " + s + ".facture WHERE demande_id = ? AND statut <> 'ANNULEE'")) {
            requete.setLong(1, demandeId);
            try (ResultSet rs = requete.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    /** Prochain numéro d'avoir AV-AAAAMM-NNN du mois (même logique que les factures FAC-). */
    static String prochainAvoir(Connection connexion, String s) throws SQLException {
        String prefixe = "AV-" + java.time.LocalDate.now(java.time.ZoneId.of("Africa/Casablanca"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        int dernier = 0;
        try (PreparedStatement requete = connexion.prepareStatement("SELECT MAX(CAST(SUBSTRING(numero_avoir, "
                + (prefixe.length() + 1) + ") AS UNSIGNED)) FROM " + s + ".facture WHERE numero_avoir LIKE ?")) {
            requete.setString(1, prefixe + "%");
            try (ResultSet rs = requete.executeQuery()) {
                if (rs.next()) {
                    dernier = rs.getInt(1);
                }
            }
        }
        return prefixe + String.format(java.util.Locale.ROOT, "%03d", dernier + 1);
    }

    private static int executer(Connection connexion, String sql, Object... parametres) throws SQLException {
        try (PreparedStatement requete = connexion.prepareStatement(sql)) {
            for (int i = 0; i < parametres.length; i++) {
                requete.setObject(i + 1, parametres[i]);
            }
            return requete.executeUpdate();
        }
    }

    /** L'agent d'accueil qui a enregistré la demande signe l'entrée d'audit du refus automatique. */
    private static String emailDe(Connection connexion, Long utilisateurId) throws SQLException {
        if (utilisateurId == null) {
            return null;
        }
        try (PreparedStatement requete = connexion.prepareStatement(
                "SELECT email FROM " + SchemaConstants.CENTRAL_SCHEMA + ".utilisateur WHERE id_utilisateur = ?")) {
            requete.setLong(1, utilisateurId);
            try (ResultSet rs = requete.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private List<Laboratoire> laboratoires() {
        List<Laboratoire> laboratoires = new ArrayList<>();
        try (Connection connexion = registry.getCentral().getConnection();
             PreparedStatement requete = connexion.prepareStatement(
                     "SELECT l.id_laboratoire, l.nom_schema FROM " + SchemaConstants.CENTRAL_SCHEMA + ".laboratoire l"
                             + " JOIN INFORMATION_SCHEMA.SCHEMATA x ON x.SCHEMA_NAME = l.nom_schema"
                             + " WHERE l.nom_schema REGEXP '^lims_[a-z0-9_]+$'");
             ResultSet rs = requete.executeQuery()) {
            while (rs.next()) {
                laboratoires.add(new Laboratoire(rs.getLong(1), rs.getString(2)));
            }
        } catch (SQLException ex) {
            log.error("Délai échantillons patients : liste des laboratoires illisible : {}", ex.getMessage());
        }
        return laboratoires;
    }
}
