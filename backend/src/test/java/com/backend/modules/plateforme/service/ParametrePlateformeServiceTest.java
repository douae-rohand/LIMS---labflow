package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.dto.ParametresPlateformeDtos.Securite;
import com.backend.modules.plateforme.entity.ParametrePlateforme;
import com.backend.modules.plateforme.repository.ParametrePlateformeRepository;
import com.backend.modules.plateforme.repository.ReferentielTypeLaboratoireRepository;
import com.backend.modules.plateforme.repository.TypeDocumentIntegrationRepository;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ParametrePlateformeServiceTest {

    @Mock
    private ParametrePlateformeRepository parametreRepository;
    @Mock
    private TypeDocumentIntegrationRepository typeDocumentRepository;
    @Mock
    private ReferentielTypeLaboratoireRepository typeLaboratoireRepository;
    @Mock
    private JournalAuditService journalAuditService;

    private ParametrePlateformeService service;
    private final Map<String, ParametrePlateforme> base = new HashMap<>();

    @BeforeEach
    void preparer() {
        service = new ParametrePlateformeService(parametreRepository, typeDocumentRepository,
                typeLaboratoireRepository, journalAuditService, new ObjectMapper(),
                new com.backend.common.tenant.TenantExecutor(null) {
                    @Override
                    public <T> T inCentral(java.util.function.Supplier<T> action) {
                        return action.get();
                    }
                });
        lenient().when(parametreRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(base.values()));
        lenient().when(parametreRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(base.get(invocation.<String>getArgument(0))));
        lenient().when(parametreRepository.save(any())).thenAnswer(invocation -> {
            ParametrePlateforme parametre = invocation.getArgument(0);
            base.put(parametre.getCle(), parametre);
            return parametre;
        });
    }

    @Test
    void valeursParDefautSansParametreEnBase() {
        Securite securite = service.securite();
        assertTrue(securite.roles2faObligatoire().isEmpty());
        assertEquals(10, securite.longueurMinMotDePasse());
        assertFalse(securite.exigerCaractereSpecial());
        assertEquals(7, securite.dureeSessionJours());
        assertFalse(service.deuxFacteursObligatoire(RoleUtilisateur.ADMINISTRATEUR));
    }

    @Test
    void modifierSecuriteEstRelueImmediatement() {
        service.securite();
        service.modifierSecurite(new Securite(List.of("administrateur", "RESPONSABLE"), 12, true, 3),
                "super@labflow.test");

        assertTrue(service.deuxFacteursObligatoire(RoleUtilisateur.ADMINISTRATEUR));
        assertTrue(service.deuxFacteursObligatoire(RoleUtilisateur.RESPONSABLE));
        assertFalse(service.deuxFacteursObligatoire(RoleUtilisateur.CLIENT));
        assertEquals(12, service.politiqueMotDePasse().longueurMin());
        assertTrue(service.politiqueMotDePasse().exigerCaractereSpecial());
        assertEquals(3L * 24 * 3600 * 1000, service.dureeSessionMs());
    }

    @Test
    void refuseUnRoleInconnuOuUneLongueurTropFaible() {
        assertThrows(BusinessRuleException.class, () -> service.modifierSecurite(
                new Securite(List.of("DIRECTEUR"), 10, false, 7), "super@labflow.test"));
        assertThrows(BusinessRuleException.class, () -> service.modifierSecurite(
                new Securite(List.of(), 8, false, 7), "super@labflow.test"));
        assertThrows(BusinessRuleException.class, () -> service.modifierSecurite(
                new Securite(List.of(), 10, false, 45), "super@labflow.test"));
    }

    @Test
    void codeDocumentSansAccentNiPonctuation() {
        assertEquals("ATTESTATION_D_ASSURANCE", ParametrePlateformeService.codeDepuis("Attestation d'assurance"));
        assertEquals("CERTIFICAT_D_HYGIENE_2026", ParametrePlateformeService.codeDepuis("  Certificat d'hygiène (2026) "));
        assertThrows(BusinessRuleException.class, () -> ParametrePlateformeService.codeDepuis(" -- "));
    }
}
