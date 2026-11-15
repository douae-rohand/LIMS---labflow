package com.backend.modules.utilisateur.repository;

import com.backend.modules.utilisateur.entity.TechnicienDomaine;
import com.backend.modules.utilisateur.entity.TechnicienDomaineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TechnicienDomaineRepository extends JpaRepository<TechnicienDomaine, TechnicienDomaineId> {

    List<TechnicienDomaine> findByTechnicienId(Long technicienId);

    List<TechnicienDomaine> findByTechnicienIdIn(Collection<Long> technicienIds);

    void deleteByTechnicienId(Long technicienId);
}
