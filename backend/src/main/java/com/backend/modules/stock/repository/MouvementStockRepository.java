package com.backend.modules.stock.repository;

import com.backend.modules.stock.entity.MouvementStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {
    Page<MouvementStock> findByArticleId(Long articleId, Pageable pageable);
}
