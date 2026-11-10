package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.ParametrePlateforme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametrePlateformeRepository extends JpaRepository<ParametrePlateforme, String> {
}
