package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.ReferentielDomaine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReferentielDomaineRepository extends JpaRepository<ReferentielDomaine, Long> {

    List<ReferentielDomaine> findAllByOrderByOrdreAscLibelleAsc();

    boolean existsByCode(String code);
}
