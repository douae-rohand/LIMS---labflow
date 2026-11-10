package com.backend.modules.validation.repository;

import com.backend.modules.validation.entity.Validation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ValidationRepository extends JpaRepository<Validation, Long> {

    List<Validation> findByLigneEssai_Id(Long ligneEssaiId);

    List<Validation> findByLigneEssai_IdOrderByDateValidationAscIdAsc(Long ligneEssaiId);

    List<Validation> findByLigneEssai_IdInOrderByDateValidationAscIdAsc(java.util.Collection<Long> ligneEssaiIds);

    Page<Validation> findByValidateurId(Long validateurId, Pageable pageable);

    // ---------------------------------------------------------------
    // CMD-13 : requêtes Espace Technicien
    // ---------------------------------------------------------------

    /**
     * Validations niveau 1 postérieures à une date (pour tester
     * si la dernière validation couvre la dernière saisie).
     */
    @Query("""
            SELECT v FROM Validation v
            WHERE v.ligneEssai.id = :ligneEssaiId
              AND v.niveau = :niveau
              AND v.dateValidation >= :depuis
            ORDER BY v.dateValidation DESC
            """)
    List<Validation> findByLigneEssaiIdAndNiveauDepuis(
            @Param("ligneEssaiId") Long ligneEssaiId,
            @Param("niveau") Integer niveau,
            @Param("depuis") Instant depuis);

    /**
     * Vérifie qu'il existe une validation de niveau :niveau
     * postérieure à :depuis pour la ligne donnée.
     */
    @Query("""
            SELECT COUNT(v) > 0 FROM Validation v
            WHERE v.ligneEssai.id = :ligneEssaiId
              AND v.niveau = :niveau
              AND v.dateValidation >= :depuis
            """)
    boolean existsValidationNiveauDepuis(
            @Param("ligneEssaiId") Long ligneEssaiId,
            @Param("niveau") Integer niveau,
            @Param("depuis") Instant depuis);

    /**
     * Validations niveau 2 (Responsable) sur une ligne.
     * Utilisé pour bloquer la modification de résultat si déjà validé N2.
     */
    @Query("""
            SELECT COUNT(v) > 0 FROM Validation v
            WHERE v.ligneEssai.id = :ligneEssaiId
              AND v.niveau = 2
              AND v.decision = 'VALIDEE'
            """)
    boolean existsValidationResponsableValidee(@Param("ligneEssaiId") Long ligneEssaiId);

    /**
     * Compte les lignes EN_COURS avec valeur saisie mais sans validation N1
     * postérieure à la dernière saisie — pour le tableau de bord.
     *
     * Logique : essais EN_COURS, valeur non null, et aucune validation N1
     * dont dateValidation >= dateSaisie.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT le.id_ligne_essai)
            FROM ligne_essai le
            WHERE le.technicien_id = :technicienId
              AND le.statut = 'EN_COURS'
              AND le.valeur IS NOT NULL
              AND le.date_saisie IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM validation v
                  WHERE v.ligne_essai_id = le.id_ligne_essai
                    AND v.niveau = 1
                    AND v.date_validation >= le.date_saisie
              )
            """, nativeQuery = true)
    long countValidationsTechniquesEnAttente(@Param("technicienId") Long technicienId);
}
