package com.backend.modules.produit.repository;

import com.backend.modules.produit.entity.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    Optional<Produit> findByReference(String reference);

    boolean existsByReference(String reference);

    @Query("SELECT p FROM Produit p WHERE p.actif = true ORDER BY p.nom")
    List<Produit> findActifs();

    @Query("SELECT p FROM Produit p WHERE (SELECT COALESCE(SUM(l.quantite), 0) FROM Lot l WHERE l.produit.id = p.id AND l.statut = 'DISPONIBLE' AND l.peremption > CURRENT_DATE) < p.seuilMinimal")
    List<Produit> findProduitsSousSeuil();

    @Query("SELECT p FROM Produit p WHERE (SELECT COALESCE(SUM(l.quantite), 0) FROM Lot l WHERE l.produit.id = p.id AND l.statut = 'DISPONIBLE' AND l.peremption > CURRENT_DATE) = 0")
    List<Produit> findProduitsEnRupture();

    @Query("SELECT u.id, u.nom, u.prenom, u.email, r.code, l.raisonSociale FROM Utilisateur u JOIN u.role r LEFT JOIN u.laboratoire l WHERE r.code IN ('RESPONSABLE', 'TECHNICIEN', 'ACCUEIL') ORDER BY u.nom")
    org.springframework.data.domain.Page<Object[]> listeEmployes(org.springframework.data.domain.Pageable pageable);
}
