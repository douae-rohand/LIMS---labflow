package com.backend.modules.utilisateur.repository;

import com.backend.modules.utilisateur.entity.MatriculeSequence;
import com.backend.modules.utilisateur.entity.MatriculeSequenceId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatriculeSequenceRepository extends JpaRepository<MatriculeSequence, MatriculeSequenceId> {

    /**
     * Recherche la séquence pour un préfixe et une année donnés avec un verrou pessimiste en écriture.
     * Génère un `SELECT ... FOR UPDATE` en MySQL, garantissant l'absence de collision
     * lors de créations simultanées.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MatriculeSequence m WHERE m.prefixe = :prefixe AND m.annee = :annee")
    Optional<MatriculeSequence> findWithLock(@Param("prefixe") String prefixe, @Param("annee") Integer annee);
}
