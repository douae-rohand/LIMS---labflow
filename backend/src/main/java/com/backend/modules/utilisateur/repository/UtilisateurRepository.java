package com.backend.modules.utilisateur.repository;

import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository principal pour l'entité {@link Utilisateur} et ses sous-types.
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Utilisateur> findByRole(RoleUtilisateur role, Pageable pageable);

    Page<Utilisateur> findByActif(boolean actif, Pageable pageable);

    @Query("""
            SELECT u FROM Utilisateur u
            WHERE (:search IS NULL
                   OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.prenom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Utilisateur> rechercher(@Param("search") String search, Pageable pageable);
}
