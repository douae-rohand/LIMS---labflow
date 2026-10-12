package com.backend.modules.plateforme.service;

import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.dto.LigneImportLaboratoireDto;
import com.backend.modules.plateforme.dto.RapportImportLaboratoireDto;
import com.backend.modules.plateforme.entity.DemandeIntegration;
import com.backend.modules.plateforme.entity.StatutIntegration;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaboratoireImportServiceTest {

    @Mock
    private DemandeIntegrationRepository demandeIntegrationRepository;
    @Mock
    private LaboratoireRepository laboratoireRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private LaboratoireImportService importService;

    @Test
    void confirmerCreeUneDemandeIncompleteSansLaboratoire() {
        when(demandeIntegrationRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            DemandeIntegration demande = invocation.getArgument(0);
            demande.setId(9L);
            return demande;
        });

        RapportImportLaboratoireDto rapport = importService.confirmer(List.of(ligneValide()));

        assertEquals(1, rapport.getEnregistrees());
        assertFalse(rapport.isApercu());
        ArgumentCaptor<DemandeIntegration> capteur = ArgumentCaptor.forClass(DemandeIntegration.class);
        verify(demandeIntegrationRepository).saveAndFlush(capteur.capture());
        DemandeIntegration enregistree = capteur.getValue();
        assertEquals(StatutIntegration.EN_ATTENTE.name(), enregistree.getStatut());
        assertNull(enregistree.getLaboratoire());
        assertTrue(enregistree.getMessage().contains("documents justificatifs"));
        assertTrue(enregistree.getMessage().contains("Aucun espace laboratoire"));
    }

    @Test
    void verifierSignaleLesChampsObligatoiresSansEnregistrer() {
        LigneImportLaboratoireDto ligne = ligneValide();
        ligne.setVille(null);
        ligne.setAdminEmail("pas-un-email");

        RapportImportLaboratoireDto rapport = importService.verifier(List.of(ligne));

        assertEquals(0, rapport.getValides());
        assertTrue(rapport.isApercu());
        assertTrue(rapport.getLignes().getFirst().getErreurs().stream()
                .anyMatch(erreur -> erreur.contains("ville") || erreur.contains("e-mail")));
        verify(demandeIntegrationRepository, never()).saveAndFlush(any());
    }

    @Test
    void verifierDetecteUnDoublonDansLeFichier() {
        LigneImportLaboratoireDto premiere = ligneValide();
        LigneImportLaboratoireDto seconde = ligneValide();
        seconde.setNomLaboratoire("Autre laboratoire");
        seconde.setIce("001545678000013");

        RapportImportLaboratoireDto rapport = importService.verifier(List.of(premiere, seconde));

        assertEquals(1, rapport.getValides());
        assertEquals(1, rapport.getInvalides());
        assertTrue(rapport.getLignes().get(1).getErreurs().stream()
                .anyMatch(erreur -> erreur.contains("déjà présente dans le fichier")));
    }

    @Test
    void modeleCsvDecritLesColonnesEtLeProcessus() {
        String csv = new String(importService.modele("csv").contenu(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("Nom du laboratoire;Raison sociale;Types de laboratoire"));
        assertTrue(csv.contains("Aucun espace laboratoire n'est créé"));
        assertThrows(BusinessRuleException.class, () -> importService.modele("pdf"));
    }

    private static LigneImportLaboratoireDto ligneValide() {
        return LigneImportLaboratoireDto.builder()
                .numeroLigne(2)
                .nomLaboratoire("Laboratoire Atlas")
                .raisonSociale("Atlas Analyses SARL")
                .typesLaboratoire("Médical")
                .ice("001545678000012")
                .ville("Casablanca")
                .adresse("12 Boulevard Zerktouni")
                .adminNom("Bennani")
                .adminPrenom("Sara")
                .adminEmail("sara.bennani@atlas.ma")
                .adminTelephone("0612345678")
                .adminFonction("Biologiste responsable")
                .adminCin("AB123456")
                .build();
    }
}
