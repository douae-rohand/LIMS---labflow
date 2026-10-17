package com.backend.modules.essai.repository;

import com.backend.modules.essai.entity.ExigenceEchantillon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExigenceEchantillonRepository extends JpaRepository<ExigenceEchantillon, Long> {

    List<ExigenceEchantillon> findByEssai_Id(Long essaiId);

    List<ExigenceEchantillon> findByEssai_IdIn(Collection<Long> essaiIds);
}
