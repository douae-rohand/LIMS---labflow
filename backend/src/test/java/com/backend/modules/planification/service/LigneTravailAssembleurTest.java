package com.backend.modules.planification.service;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.essai.entity.Essai;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.essai.entity.StatutEssai;
import com.backend.modules.planification.dto.LigneTravailDto;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import com.backend.modules.validation.repository.ValidationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LigneTravailAssembleurTest {

    @Mock
    private ValidationRepository validationRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private TenantExecutor tenantExecutor;

    @Test
    void signaleLeTechnicienDesactiveSurUneAnalyseToujoursOuverte() {
        Utilisateur technicien = Utilisateur.builder()
                .id(17L)
                .prenom("Samira")
                .nom("Test")
                .actif(false)
                .build();
        LigneEssai ligne = LigneEssai.builder()
                .id(42L)
                .code("ESS-42")
                .statut(StatutEssai.EN_COURS)
                .montant(BigDecimal.ZERO)
                .technicienId(17L)
                .demande(Demande.builder()
                        .id(9L)
                        .numero("DEM-9")
                        .titre("Analyse")
                        .statut("EN_COURS")
                        .dateSoumission(Instant.now())
                        .build())
                .essai(Essai.builder()
                        .code("A-1")
                        .designation("Analyse")
                        .dureeEstimee(30)
                        .build())
                .build();

        when(validationRepository.findByLigneEssai_IdInOrderByDateValidationAscIdAsc(List.of(42L)))
                .thenReturn(List.of());
        when(utilisateurRepository.findAllById(anyIterable())).thenReturn(List.of(technicien));
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(tenantExecutor).inCentral(any());

        LigneTravailDto resultat = new LigneTravailAssembleur(
                validationRepository, utilisateurRepository, tenantExecutor)
                .assembler(List.of(ligne)).getFirst();

        assertTrue(resultat.isTechnicienDesactive());
        assertEquals(17L, resultat.getTechnicienId());
        assertEquals("Samira Test", resultat.getTechnicienNom());
    }
}
