package com.backend.modules.stock.repository;

import com.backend.modules.stock.entity.ArticleStock;
import com.backend.modules.stock.entity.CategorieStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArticleStockRepository extends JpaRepository<ArticleStock, Long> {
    Page<ArticleStock> findByCategorie(CategorieStock categorie, Pageable pageable);
    /** Articles sous le seuil minimum. */
    @Query("SELECT a FROM ArticleStock a WHERE a.actif = true AND a.quantiteDisponible < a.quantiteMinimum")
    List<ArticleStock> findEnRuptureImminente();
}
