package com.backend.modules.satisfaction.repository;

import com.backend.modules.satisfaction.entity.EnqueteSatisfaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EnqueteSatisfactionRepository extends JpaRepository<EnqueteSatisfaction, Long> {

    boolean existsByDemande_Id(Long demandeId);

    @Query("SELECT AVG(e.noteGlobale) FROM EnqueteSatisfaction e")
    Double calculerMoyenneGlobale();
}
