package com.backend.modules.satisfaction.repository;

import com.backend.modules.satisfaction.entity.Satisfaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SatisfactionRepository extends JpaRepository<Satisfaction, Long> {
    Page<Satisfaction> findByClientId(Long clientId, Pageable pageable);
    boolean existsByDemandeIdAndClientId(Long demandeId, Long clientId);

    @Query("SELECT AVG(s.note) FROM Satisfaction s")
    Double calculerMoyenneGlobale();
}
