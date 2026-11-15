package com.backend.modules.accueil.repository;

import com.backend.modules.patient.entity.Patient;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Patients du laboratoire (schéma tenant). */
public interface PatientAccueilRepository extends Repository<Patient, Long> {

    Patient save(Patient patient);

    Optional<Patient> findById(Long id);

    @Query("SELECT p FROM Patient p ORDER BY p.id DESC")
    List<Patient> tous();

    @Query("SELECT COUNT(p) > 0 FROM Patient p WHERE UPPER(p.cin) = UPPER(:cin) AND (:id IS NULL OR p.id <> :id)")
    boolean cinUtilise(@Param("cin") String cin, @Param("id") Long id);

    /** Plus grand numéro attribué pour un préfixe PAT-AAAAMM- (longueur 11). */
    @Query("SELECT MAX(CAST(SUBSTRING(p.code, 12) AS Integer)) FROM Patient p WHERE p.code LIKE :prefixe")
    Integer dernierNumero(@Param("prefixe") String prefixe);
}
