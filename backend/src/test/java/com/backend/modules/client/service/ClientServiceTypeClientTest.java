package com.backend.modules.client.service;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.client.entity.ClientProfil;
import com.backend.modules.client.entity.TypeClient;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.client.repository.ClientProfilRepository;
import com.backend.modules.client.repository.ClientRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTypeClientTest {

    @Mock
    private ClientProfilRepository clientProfilRepository;
    @Mock
    private ClientLaboratoireRepository clientLaboratoireRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private LaboratoireRepository laboratoireRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private TenantExecutor tenantExecutor;
    @InjectMocks
    private ClientService clientService;

    @Test
    void uneRaisonSocialeDonneUneEntreprise() {
        when(clientProfilRepository.existsByUtilisateur_Id(7L)).thenReturn(false);
        when(clientProfilRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientProfil profil = clientService.creerProfil(utilisateur(), "Laboratoire Atlas", "001", null, true);

        assertEquals(TypeClient.ENTREPRISE, profil.getTypeClient());
        assertEquals("Laboratoire Atlas", profil.getRaisonSociale());
    }

    @Test
    void sansRaisonSocialeLeClientResteParticulier() {
        when(clientProfilRepository.existsByUtilisateur_Id(7L)).thenReturn(false);
        when(clientProfilRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClientProfil profil = clientService.creerProfil(utilisateur(), "  ", null, null, false);

        assertEquals(TypeClient.PARTICULIER, profil.getTypeClient());
    }

    private static Utilisateur utilisateur() {
        return Utilisateur.builder()
                .id(7L)
                .nom("Alaoui")
                .email("client@labflow.ma")
                .motDePasseHash("hash")
                .build();
    }
}
