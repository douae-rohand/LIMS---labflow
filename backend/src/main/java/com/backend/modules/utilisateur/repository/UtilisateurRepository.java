package com.backend.modules.utilisateur.repository;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository principal pour l'entité {@link Utilisateur} et ses sous-types.
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    @Query("""
            SELECT u FROM Utilisateur u
            JOIN FETCH u.role
            LEFT JOIN FETCH u.laboratoire
            WHERE u.email = :email
            """)
    Optional<Utilisateur> findByEmailWithRoleAndLaboratoire(@Param("email") String email);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u FROM Utilisateur u
            WHERE u.role.code = :#{#role.name()}
            """)
    Page<Utilisateur> findByRole(@Param("role") RoleUtilisateur role, Pageable pageable);

    Page<Utilisateur> findByActif(boolean actif, Pageable pageable);

    @Query("""
            SELECT u FROM Utilisateur u
            WHERE (:search IS NULL
                   OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Utilisateur> rechercher(@Param("search") String search, Pageable pageable);

    /**
     * Recherche et filtre les employés d'un laboratoire (utilisé par l'Administrateur du laboratoire).
     */
    @Query("""
            SELECT u FROM Utilisateur u
            JOIN u.role r
            WHERE u.laboratoire.id = :laboratoireId
              AND r.code IN ('RESPONSABLE', 'TECHNICIEN', 'ACCUEIL')
              AND (:role IS NULL OR r.code = :role)
              AND (
                   :statut IS NULL
                   OR (:statut = 'ACTIF' AND u.actif = true AND u.compteConfirme = true)
                   OR (:statut = 'DESACTIVE' AND u.actif = false AND u.compteConfirme = true)
                   OR (:statut = 'EN_ATTENTE' AND u.compteConfirme = false)
              )
              AND (
                   :q IS NULL
                   OR LOWER(u.matricule) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
                   OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
                   OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '!'
              )
            """)
    Page<Utilisateur> rechercherEmployesLaboratoire(
            @Param("laboratoireId") Long laboratoireId,
            @Param("role") String role,
            @Param("statut") String statut,
            @Param("q") String q,
            Pageable pageable);

    /**
     * Recherche les administrateurs pour une collection de laboratoires (utilisé par le Super Administrateur).
     */
    @Query("""
            SELECT u FROM Utilisateur u
            JOIN FETCH u.role r
            WHERE u.laboratoire.id IN :ids
              AND r.code = 'ADMINISTRATEUR'
            ORDER BY u.id ASC
            """)
    List<Utilisateur> findAdministrateursDesLaboratoires(@Param("ids") Collection<Long> ids);
}
