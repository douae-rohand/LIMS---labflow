package com.backend.modules.ia.donnees;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.accueil.service.CaisseService;
import com.backend.modules.accueil.service.TableauAccueilService;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.essai.repository.LigneEssaiRepository;
import com.backend.modules.facturation.service.FacturationAdminService;
import com.backend.modules.planification.service.LigneTravailAssembleur;
import com.backend.modules.planification.service.SuiviResponsableService;
import com.backend.modules.plateforme.repository.DemandeIntegrationRepository;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.produit.service.StockAdminService;
import com.backend.modules.satisfaction.service.SatisfactionAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Choisit les outils de lecture des données (CDC § 3.4) selon le rôle de l'utilisateur connecté.
 *
 * <p>Le rôle, le laboratoire et l'identifiant viennent du jeton : un Technicien ne reçoit jamais les
 * outils de la caisse, un laboratoire ne lit jamais le schéma d'un autre. Le client et le visiteur
 * n'ont aucun outil : l'assistant reste pour eux un RAG sur la documentation et les catalogues.
 */
@Service
@RequiredArgsConstructor
public class OutilsDonneesService {

    private final TenantExecutor tenantExecutor;
    private final SuiviResponsableService suiviResponsableService;
    private final LigneEssaiRepository ligneEssaiRepository;
    private final LigneTravailAssembleur ligneTravailAssembleur;
    private final TableauAccueilService tableauAccueilService;
    private final CaisseService caisseService;
    private final StockAdminService stockAdminService;
    private final FacturationAdminService facturationAdminService;
    private final SatisfactionAdminService satisfactionAdminService;
    private final LaboratoireRepository laboratoireRepository;
    private final DemandeIntegrationRepository demandeIntegrationRepository;

    /** Outils de l'utilisateur et leur session, ou vide pour un client. */
    public Optional<OutilsAssistant> pour(UtilisateurPrincipal principal) {
        String schema = principal.getNomSchema();
        return switch (principal.getRole()) {
            case SUPER_ADMINISTRATEUR -> {
                SessionOutils session = new SessionOutils(tenantExecutor, null, principal.getId());
                yield Optional.of(new OutilsAssistant(session,
                        new OutilsSuperAdmin(session, laboratoireRepository, demandeIntegrationRepository)));
            }
            case RESPONSABLE -> personnel(schema, principal, s -> new OutilsResponsable(s, suiviResponsableService));
            case TECHNICIEN -> personnel(schema, principal,
                    s -> new OutilsTechnicien(s, ligneEssaiRepository, ligneTravailAssembleur));
            case ACCUEIL -> personnel(schema, principal,
                    s -> new OutilsAccueil(s, tableauAccueilService, caisseService));
            case ADMINISTRATEUR -> personnel(schema, principal,
                    s -> new OutilsAdministrateur(s, stockAdminService, facturationAdminService, satisfactionAdminService));
            case CLIENT -> Optional.empty();
        };
    }

    private Optional<OutilsAssistant> personnel(String schema, UtilisateurPrincipal principal,
                                                java.util.function.Function<SessionOutils, Object> fabrique) {
        // Sans schéma valide, aucun outil : jamais de lecture dans le schéma central par erreur
        if (schema == null || !schema.matches("[A-Za-z0-9_]+")) return Optional.empty();
        SessionOutils session = new SessionOutils(tenantExecutor, schema, principal.getId());
        return Optional.of(new OutilsAssistant(session, fabrique.apply(session)));
    }

    /** Objet portant les méthodes {@code @Tool}, et la session qui en garde la trace. */
    public record OutilsAssistant(SessionOutils session, Object outils) {}
}
