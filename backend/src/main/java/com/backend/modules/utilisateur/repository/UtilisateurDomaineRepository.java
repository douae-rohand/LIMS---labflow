package com.backend.modules.utilisateur.repository;

import com.backend.modules.utilisateur.entity.UtilisateurDomaine;
import com.backend.modules.utilisateur.entity.UtilisateurDomaineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface UtilisateurDomaineRepository extends JpaRepository<UtilisateurDomaine, UtilisateurDomaineId> {

    List<UtilisateurDomaine> findByUtilisateurIdIn(Collection<Long> utilisateurIds);
}
