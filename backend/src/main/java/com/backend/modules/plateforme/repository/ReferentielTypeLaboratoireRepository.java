package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.ReferentielTypeLaboratoire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReferentielTypeLaboratoireRepository extends JpaRepository<ReferentielTypeLaboratoire, Long> {

    List<ReferentielTypeLaboratoire> findAllByOrderByOrdreAscLibelleAsc();

    List<ReferentielTypeLaboratoire> findByActifTrueOrderByOrdreAscLibelleAsc();

    boolean existsByLibelleIgnoreCase(String libelle);
}
