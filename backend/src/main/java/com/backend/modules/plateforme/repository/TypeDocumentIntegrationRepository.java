package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.TypeDocumentIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeDocumentIntegrationRepository extends JpaRepository<TypeDocumentIntegration, String> {

    List<TypeDocumentIntegration> findAllByOrderByOrdreAscLibelleAsc();

    List<TypeDocumentIntegration> findByActifTrueOrderByOrdreAscLibelleAsc();
}
