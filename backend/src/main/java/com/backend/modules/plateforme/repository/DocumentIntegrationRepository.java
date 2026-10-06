package com.backend.modules.plateforme.repository;

import com.backend.modules.plateforme.entity.DocumentIntegration;
import com.backend.modules.plateforme.entity.TypeDocumentIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentIntegrationRepository extends JpaRepository<DocumentIntegration, Long> {

    List<DocumentIntegration> findByDemande_Id(Long demandeId);

    Optional<DocumentIntegration> findByDemande_IdAndTypeDocument(Long demandeId, TypeDocumentIntegration typeDocument);

    long countByDemande_Id(Long demandeId);
}
