package com.backend.modules.essai.repository;

import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LigneEssaiRepository extends JpaRepository<LigneEssai, Long> {

    List<LigneEssai> findByEchantillon_Id(Long echantillonId);

    List<LigneEssai> findByDemande_Id(Long demandeId);

    /**
     * Charge les lignes de plusieurs demandes avec leur essai et le domaine
     * de l'essai (LEFT JOIN FETCH). Ne jamais appeler avec une collection vide.
     */
    @Query("""
            SELECT le FROM LigneEssai le
            LEFT JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            WHERE le.demande.id IN :demandeIds
            """)
    List<LigneEssai> findByDemande_IdIn(@Param("demandeIds") Collection<Long> demandeIds);

    Page<LigneEssai> findByTechnicienId(Long technicienId, Pageable pageable);

    /** Lignes des demandes dans les statuts donnés, avec demande, essai et domaine. */
    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.demande d
            LEFT JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            WHERE d.statut IN :statuts
            ORDER BY d.dateSoumission DESC, le.id ASC
            """)
    List<LigneEssai> findLignesDesDemandes(@Param("statuts") Collection<String> statuts);

    @Query("""
            SELECT COUNT(le) FROM LigneEssai le
            WHERE le.technicienId = :technicienId
              AND le.demande.statut IN :statutsDemandes
              AND (le.statut IS NULL OR le.statut NOT IN :statutsTermines)
            """)
    long compterEssaisOuvertsParTechnicien(
            @Param("technicienId") Long technicienId,
            @Param("statutsDemandes") Collection<String> statutsDemandes,
            @Param("statutsTermines") Collection<StatutEssai> statutsTermines);

    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.demande d
            LEFT JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            WHERE le.id IN :ids
            """)
    List<LigneEssai> findAvecDetails(@Param("ids") Collection<Long> ids);

    /** Nombre d'analyses réalisables par demande, en une requête. */
    @Query("""
            SELECT le.demande.id, COUNT(le) FROM LigneEssai le
            WHERE le.demande.id IN :demandeIds AND (le.statut IS NULL OR le.statut <> :exclu)
            GROUP BY le.demande.id
            """)
    List<Object[]> compterParDemande(@Param("demandeIds") Collection<Long> demandeIds,
                                     @Param("exclu") com.backend.modules.essai.entity.StatutEssai exclu);

    Page<LigneEssai> findByStatut(StatutEssai statut, Pageable pageable);

    boolean existsByEssaiId(Long essaiId);

    // ---------------------------------------------------------------
    // CMD-13 : requêtes Espace Technicien
    // ---------------------------------------------------------------

    /**
     * Liste paginée filtrée pour le technicien courant.
     * JOIN FETCH essai + domaine (pas de N+1). Échantillon en LEFT JOIN FETCH.
     * Les filtres nullables sont ignorés quand false / null.
     *
     * @param technicienId  id du principal (jamais un paramètre HTTP)
     * @param statut        filtre statut (null = tous)
     * @param domaineId     filtre domaine (null = tous)
     * @param demandeId     filtre demande (null = toutes)
     * @param debutAttrib   début période dateAttribution (null = sans borne)
     * @param finAttrib     fin période dateAttribution (null = sans borne)
     * @param q             recherche sur code / désignation / numéro demande
     */
    @Query(value = """
            SELECT DISTINCT le FROM LigneEssai le
            JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            LEFT JOIN FETCH le.echantillon
            JOIN le.demande d
            WHERE le.technicienId = :technicienId
              AND (:statut IS NULL OR le.statut = :statut)
              AND (:domaineId IS NULL OR e.domaine.id = :domaineId)
              AND (:demandeId IS NULL OR d.id = :demandeId)
              AND (:debutAttrib IS NULL OR le.dateAttribution >= :debutAttrib)
              AND (:finAttrib   IS NULL OR le.dateAttribution <= :finAttrib)
              AND (
                    :q = ''
                    OR LOWER(le.code) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(e.designation) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(d.numero) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT le) FROM LigneEssai le
            JOIN le.essai e
            LEFT JOIN e.domaine
            JOIN le.demande d
            WHERE le.technicienId = :technicienId
              AND (:statut IS NULL OR le.statut = :statut)
              AND (:domaineId IS NULL OR e.domaine.id = :domaineId)
              AND (:demandeId IS NULL OR d.id = :demandeId)
              AND (:debutAttrib IS NULL OR le.dateAttribution >= :debutAttrib)
              AND (:finAttrib   IS NULL OR le.dateAttribution <= :finAttrib)
              AND (
                    :q = ''
                    OR LOWER(le.code) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(e.designation) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(d.numero) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            """)
    Page<LigneEssai> rechercherPourTechnicien(
            @Param("technicienId") Long technicienId,
            @Param("statut") StatutEssai statut,
            @Param("domaineId") Long domaineId,
            @Param("demandeId") Long demandeId,
            @Param("debutAttrib") java.time.Instant debutAttrib,
            @Param("finAttrib") java.time.Instant finAttrib,
            @Param("q") String q,
            Pageable pageable);

    /**
     * Ligne unique avec JOIN FETCH, filtrée par technicienId.
     * Retourne Optional.empty() si la ligne n'appartient pas au technicien.
     */
    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            LEFT JOIN FETCH le.echantillon
            JOIN FETCH le.demande
            WHERE le.id = :id AND le.technicienId = :technicienId
            """)
    java.util.Optional<LigneEssai> findByIdAndTechnicienId(
            @Param("id") Long id,
            @Param("technicienId") Long technicienId);

    /**
     * Lignes d'un technicien dans une plage de dates d'attribution (planning).
     * Triées par dateAttribution ASC puis id ASC.
     */
    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            LEFT JOIN FETCH le.echantillon
            JOIN FETCH le.demande
            WHERE le.technicienId = :technicienId
              AND le.dateAttribution >= :debut
              AND le.dateAttribution < :fin
            ORDER BY le.dateAttribution ASC, le.id ASC
            """)
    List<LigneEssai> findPlanning(
            @Param("technicienId") Long technicienId,
            @Param("debut") java.time.Instant debut,
            @Param("fin") java.time.Instant fin);

    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.essai e LEFT JOIN FETCH e.domaine
            LEFT JOIN FETCH le.echantillon JOIN FETCH le.demande
            WHERE le.technicienId = :technicienId
              AND COALESCE(le.dateDebut, le.dateAttribution) >= :debut
              AND COALESCE(le.dateDebut, le.dateAttribution) < :fin
            ORDER BY COALESCE(le.dateDebut, le.dateAttribution) ASC, le.id ASC
            """)
    List<LigneEssai> findPlanningParDatePrevue(@Param("technicienId") Long technicienId, @Param("debut") java.time.Instant debut, @Param("fin") java.time.Instant fin);

    /**
     * Lignes de la même demande (pour tester la transition EN_VALIDATION).
     * Charge uniquement id + statut, pas les associations.
     */
    @Query("SELECT le FROM LigneEssai le WHERE le.demande.id = :demandeId")
    List<LigneEssai> findByDemandeIdLeger(@Param("demandeId") Long demandeId);

    // ----------- Agrégats pour la synthèse (pas de chargement d'entité) -------

    @Query(value = """
            SELECT COUNT(le) FROM LigneEssai le
            WHERE le.technicienId = :technicienId
              AND FUNCTION('DATE', le.dateAttribution) = FUNCTION('DATE', CURRENT_TIMESTAMP)
            """)
    long countEssaisAujourdhuiPourTechnicien(@Param("technicienId") Long technicienId);

    @Query("SELECT COUNT(le) FROM LigneEssai le WHERE le.technicienId = :technicienId AND le.dateAttribution >= :debut AND le.dateAttribution < :fin")
    long countEssaisAttribuesDansPeriode(@Param("technicienId") Long technicienId, @Param("debut") java.time.Instant debut, @Param("fin") java.time.Instant fin);

    @Query("SELECT COUNT(le) FROM LigneEssai le WHERE le.technicienId = :technicienId AND le.statut = 'EN_COURS'")
    long countEnCoursPourTechnicien(@Param("technicienId") Long technicienId);

    @Query("SELECT COUNT(le) FROM LigneEssai le WHERE le.technicienId = :technicienId AND le.statut = 'ATTENTE_ECHANTILLON'")
    long countEnAttenteEchantillonPourTechnicien(@Param("technicienId") Long technicienId);

    /**
     * Essais terminés sur les N derniers jours.
     */
    @Query("""
            SELECT COUNT(le) FROM LigneEssai le
            WHERE le.technicienId = :technicienId
              AND le.statut = 'TERMINE'
              AND le.dateFin >= :depuis
            """)
    long countTerminesPourTechnicienDepuis(
            @Param("technicienId") Long technicienId,
            @Param("depuis") java.time.Instant depuis);

    /**
     * Charge les données pour calculer enRetard côté Java
     * (échéance = dateAttribution + dureeEstimee minutes).
     * Récupère uniquement les lignes non terminées avec dateAttribution non null.
     */
    @Query(value = """
            SELECT COUNT(le) FROM LigneEssai le
            JOIN le.essai e
            WHERE le.technicienId = :technicienId
              AND le.statut NOT IN ('TERMINE', 'NON_REALISABLE')
              AND le.dateAttribution IS NOT NULL
              AND FUNCTION('TIMESTAMPADD', MINUTE, e.dureeEstimee, le.dateAttribution) < CURRENT_TIMESTAMP
            """)
    long countEnRetardPourTechnicien(@Param("technicienId") Long technicienId);

    @Query(value = "SELECT le.id_ligne_essai FROM ligne_essai le JOIN essai e ON e.id_essai = le.essai_id WHERE le.technicien_id = :technicienId AND le.statut NOT IN ('TERMINE', 'NON_REALISABLE') AND le.date_attribution IS NOT NULL AND TIMESTAMPADD(MINUTE, e.duree_estimee, le.date_attribution) < CURRENT_TIMESTAMP", nativeQuery = true)
    List<Long> findIdsEnRetardPourTechnicien(@Param("technicienId") Long technicienId);

    @Query(value = "SELECT le.id_ligne_essai FROM ligne_essai le JOIN essai e ON e.id_essai = le.essai_id WHERE le.technicien_id = :technicienId AND le.statut NOT IN ('TERMINE', 'NON_REALISABLE') AND le.date_attribution IS NOT NULL AND TIMESTAMPADD(MINUTE, e.duree_estimee, le.date_attribution) >= CURRENT_TIMESTAMP AND TIMESTAMPADD(MINUTE, e.duree_estimee, le.date_attribution) < :limite AND NOT (le.statut = 'EN_COURS' AND (le.valeur IS NULL OR TRIM(le.valeur) = ''))", nativeQuery = true)
    List<Long> findIdsEcheanceAvant(@Param("technicienId") Long technicienId, @Param("limite") java.time.Instant limite);

    @Query("SELECT le.id FROM LigneEssai le WHERE le.technicienId = :technicienId AND le.statut = 'EN_COURS' AND (le.valeur IS NULL OR TRIM(le.valeur) = '')")
    List<Long> findIdsResultatASaisir(@Param("technicienId") Long technicienId);

    /**
     * Charge des 7 prochains jours : nombre d'essais par date d'attribution.
     * Retourne List<Object[]> { dateAttribution (Date), count (Long) }.
     */
    @Query(value = """
            SELECT FUNCTION('DATE', le.dateAttribution), COUNT(le)
            FROM LigneEssai le
            WHERE le.technicienId = :technicienId
              AND le.dateAttribution >= :debut
              AND le.dateAttribution < :fin
            GROUP BY FUNCTION('DATE', le.dateAttribution)
            ORDER BY FUNCTION('DATE', le.dateAttribution) ASC
            """)
    List<Object[]> countParJourPourTechnicien(
            @Param("technicienId") Long technicienId,
            @Param("debut") java.time.Instant debut,
            @Param("fin") java.time.Instant fin);

    @Query("SELECT COUNT(le) FROM LigneEssai le WHERE le.technicienId = :technicienId AND le.statut = 'TERMINE'")
    long countTerminesPourTechnicien(@Param("technicienId") Long technicienId);

    @Query("SELECT COUNT(le) FROM LigneEssai le WHERE le.technicienId = :technicienId AND le.statut = 'EN_COURS' AND (le.valeur IS NULL OR TRIM(le.valeur) = '')")
    long countResultatsASaisir(@Param("technicienId") Long technicienId);

    @Query(value = """
            SELECT COUNT(DISTINCT le.id_ligne_essai)
            FROM ligne_essai le
            JOIN validation v ON v.ligne_essai_id = le.id_ligne_essai
            WHERE le.technicien_id = :technicienId
              AND v.niveau = 2 AND v.decision = 'REVISION_DEMANDEE'
              AND NOT EXISTS (
                  SELECT 1 FROM validation v2
                  WHERE v2.ligne_essai_id = le.id_ligne_essai AND v2.niveau = 2
                    AND (v2.date_validation > v.date_validation OR (v2.date_validation = v.date_validation AND v2.id_validation > v.id_validation))
              )
            """, nativeQuery = true)
    long countRetournesCorrection(@Param("technicienId") Long technicienId);

    @Query(value = "SELECT le.id_ligne_essai FROM ligne_essai le JOIN validation v ON v.ligne_essai_id = le.id_ligne_essai WHERE le.technicien_id = :technicienId AND v.niveau = 2 AND v.decision = 'REVISION_DEMANDEE' AND NOT EXISTS (SELECT 1 FROM validation v2 WHERE v2.ligne_essai_id = le.id_ligne_essai AND v2.niveau = 2 AND (v2.date_validation > v.date_validation OR (v2.date_validation = v.date_validation AND v2.id_validation > v.id_validation)))", nativeQuery = true)
    List<Long> findIdsRetournesCorrection(@Param("technicienId") Long technicienId);

    @Query(value = """
            SELECT COUNT(le.id_ligne_essai)
            FROM ligne_essai le JOIN essai e ON e.id_essai = le.essai_id
            WHERE le.technicien_id = :technicienId
              AND le.statut NOT IN ('TERMINE', 'NON_REALISABLE') AND le.date_attribution IS NOT NULL
              AND TIMESTAMPADD(MINUTE, e.duree_estimee, le.date_attribution) >= CURRENT_TIMESTAMP
              AND TIMESTAMPADD(MINUTE, e.duree_estimee, le.date_attribution) < :limite
            """, nativeQuery = true)
    long countEcheancesAvant(@Param("technicienId") Long technicienId, @Param("limite") java.time.Instant limite);

    @Query("SELECT le FROM LigneEssai le JOIN FETCH le.essai e LEFT JOIN FETCH e.domaine LEFT JOIN FETCH le.echantillon JOIN FETCH le.demande WHERE le.technicienId = :technicienId AND le.statut NOT IN ('TERMINE', 'NON_REALISABLE') AND le.dateAttribution IS NOT NULL AND FUNCTION('TIMESTAMPADD', MINUTE, e.dureeEstimee, le.dateAttribution) >= :debut AND FUNCTION('TIMESTAMPADD', MINUTE, e.dureeEstimee, le.dateAttribution) < :fin ORDER BY le.dateAttribution ASC, le.id ASC")
    List<LigneEssai> findEcheancesDansPeriode(@Param("technicienId") Long technicienId, @Param("debut") java.time.Instant debut, @Param("fin") java.time.Instant fin);

    @Query(value = "SELECT le.id_ligne_essai FROM ligne_essai le WHERE le.technicien_id = :technicienId AND le.statut = 'EN_COURS' AND le.valeur IS NOT NULL AND TRIM(le.valeur) <> '' AND le.date_saisie IS NOT NULL AND NOT EXISTS (SELECT 1 FROM validation v WHERE v.ligne_essai_id = le.id_ligne_essai AND v.niveau = 1 AND v.date_validation >= le.date_saisie)", nativeQuery = true)
    List<Long> findIdsValidationTechniqueEnAttente(@Param("technicienId") Long technicienId);

    @Query("SELECT COUNT(le) FROM LigneEssai le WHERE le.technicienId = :technicienId AND le.statut = 'TERMINE' AND le.dateFin >= :debut AND le.dateFin < :fin")
    long countTerminesPeriode(@Param("technicienId") Long technicienId, @Param("debut") java.time.Instant debut, @Param("fin") java.time.Instant fin);

    @Query("SELECT COUNT(le) FROM LigneEssai le JOIN le.essai e WHERE le.technicienId = :technicienId AND le.statut = 'TERMINE' AND le.dateFin >= :debut AND le.dateFin < :fin AND le.dateAttribution IS NOT NULL AND e.dureeEstimee IS NOT NULL AND le.dateFin <= FUNCTION('TIMESTAMPADD', MINUTE, e.dureeEstimee, le.dateAttribution)")
    long countTerminesDansLesDelais(@Param("technicienId") Long technicienId, @Param("debut") java.time.Instant debut, @Param("fin") java.time.Instant fin);

    @Query("SELECT COUNT(le) FROM LigneEssai le JOIN le.essai e WHERE le.technicienId = :technicienId AND le.statut = 'TERMINE' AND le.dateFin >= :debut AND le.dateFin < :fin AND le.dateAttribution IS NOT NULL AND e.dureeEstimee IS NOT NULL")
    long countAvecEcheancePeriode(@Param("technicienId") Long technicienId, @Param("debut") java.time.Instant debut, @Param("fin") java.time.Instant fin);

    /**
     * Toutes les lignes d'un technicien (sans pagination), avec essai JOIN FETCH.
     * Utilisé uniquement pour l'import CSV (volume borné par la taille du fichier).
     */
    @Query("""
            SELECT le FROM LigneEssai le
            JOIN FETCH le.essai e
            LEFT JOIN FETCH e.domaine
            LEFT JOIN FETCH le.echantillon
            JOIN FETCH le.demande
            WHERE le.technicienId = :technicienId
            """)
    List<LigneEssai> findByTechnicienIdAll(@Param("technicienId") Long technicienId);

    // ---------------------------------------------------------------
    // CMD-13 Lot B : mises à jour atomiques
    // ---------------------------------------------------------------

    /**
     * Démarre l'essai : ATTENTE_ECHANTILLON → EN_COURS, uniquement si statut = ATTENTE_ECHANTILLON
     * et technicienId correspond. Retourne 1 si la ligne a été mise à jour, 0 sinon.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LigneEssai le
            SET le.statut = 'EN_COURS', le.dateDebut = :maintenant
            WHERE le.id = :id
              AND le.technicienId = :technicienId
              AND le.statut = 'ATTENTE_ECHANTILLON'
            """)
    int demarrerSiAttenteEchantillon(
            @Param("id") Long id,
            @Param("technicienId") Long technicienId,
            @Param("maintenant") java.time.Instant maintenant);

    /**
     * Saisit le résultat : met à jour valeur, commentaire, conformite, date_saisie.
     * La saisie n'est possible que si statut = EN_COURS et technicienId correspond.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LigneEssai le
            SET le.valeur = :valeur, le.motif = :motif,
                le.conformite = :conformite, le.dateSaisie = :maintenant
            WHERE le.id = :id
              AND le.technicienId = :technicienId
              AND le.statut = 'EN_COURS'
            """)
    int saisirResultat(
            @Param("id") Long id,
            @Param("technicienId") Long technicienId,
            @Param("valeur") String valeur,
            @Param("motif") String motif,
            @Param("conformite") Boolean conformite,
            @Param("maintenant") java.time.Instant maintenant);

    /**
     * Passe la ligne en NON_REALISABLE (depuis ATTENTE_ECHANTILLON ou EN_COURS).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LigneEssai le
            SET le.statut = 'NON_REALISABLE', le.motif = :motif, le.dateFin = :maintenant
            WHERE le.id = :id
              AND le.technicienId = :technicienId
              AND le.statut IN ('ATTENTE_ECHANTILLON', 'EN_COURS')
            """)
    int passerNonRealisable(
            @Param("id") Long id,
            @Param("technicienId") Long technicienId,
            @Param("motif") String motif,
            @Param("maintenant") java.time.Instant maintenant);

    /**
     * Enregistre la durée déclarée uniquement si aucune durée n'est déjà définie.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LigneEssai le
            SET le.dureeMinutes = :duree
            WHERE le.id = :id AND le.technicienId = :technicienId AND le.dureeMinutes IS NULL
            """)
    int mettreAJourDuree(
            @Param("id") Long id,
            @Param("technicienId") Long technicienId,
            @Param("duree") Integer duree);

    /**
     * Termine la ligne : EN_COURS → TERMINE, uniquement si statut = EN_COURS.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LigneEssai le
            SET le.statut = 'TERMINE', le.dateFin = :maintenant
            WHERE le.id = :id
              AND le.technicienId = :technicienId
              AND le.statut = 'EN_COURS'
            """)
    int terminerSiEnCours(
            @Param("id") Long id,
            @Param("technicienId") Long technicienId,
            @Param("maintenant") java.time.Instant maintenant);
}
