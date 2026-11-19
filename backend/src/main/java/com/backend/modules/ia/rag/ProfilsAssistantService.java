package com.backend.modules.ia.rag;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.client.repository.ClientLaboratoireRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Construit le {@link ProfilAssistant} de chaque interlocuteur, côté serveur uniquement :
 * le navigateur n'envoie jamais ni rôle, ni laboratoire, ni audience.
 *
 * <table>
 *   <tr><th>Interlocuteur</th><th>Documentation</th><th>Catalogues</th><th>Limite</th></tr>
 *   <tr><td>Visiteur</td><td>tous, public</td><td>tous les laboratoires actifs</td><td>10 / h / IP</td></tr>
 *   <tr><td>Client</td><td>tous, client</td><td>ses laboratoires (compte actif)</td><td>20 / h</td></tr>
 *   <tr><td>Personnel</td><td>tous, personnel, son rôle</td><td>son laboratoire</td><td>30 / h</td></tr>
 *   <tr><td>Super Admin</td><td>tous, super-admin</td><td>tous les laboratoires actifs</td><td>30 / h</td></tr>
 * </table>
 */
@Service
@RequiredArgsConstructor
public class ProfilsAssistantService {

    public static final String AUDIENCE_TOUS = "tous";

    private static final String SANS_INFO_CLIENT = "Je n'ai pas cette information. Vous pouvez contacter le laboratoire.";
    private static final String SANS_INFO_VISITEUR = "Je n'ai pas cette information. Vous pouvez contacter l'équipe LabFlow ou le laboratoire concerné.";
    private static final String SANS_INFO_PERSONNEL = "Je n'ai pas cette information. Rapprochez-vous de l'administrateur de votre laboratoire.";
    private static final String SANS_INFO_SUPER_ADMIN = "Je n'ai pas cette information dans la documentation de la plateforme.";

    private final TenantExecutor tenantExecutor;
    private final LaboratoireRepository laboratoireRepository;
    private final ClientLaboratoireRepository clientLaboratoireRepository;

    private record Labo(String nomSchema, String nom) {}

    /** Visiteur non connecté des pages publiques. */
    public ProfilAssistant visiteur(String adresseIp) {
        List<Labo> labos = laboratoiresActifs();
        return new ProfilAssistant("ip:" + adresseIp, 10,
                Set.of(AUDIENCE_TOUS, "public"),
                tenants(labos),
                "un visiteur du site public de LabFlow, non connecté (patient, entreprise ou laboratoire qui découvre la plateforme)",
                noms(labos, "les laboratoires partenaires de la plateforme"),
                SANS_INFO_VISITEUR, "public");
    }

    /** Utilisateur connecté, quel que soit son rôle. */
    public ProfilAssistant utilisateur(UtilisateurPrincipal principal) {
        String cle = "u:" + principal.getId();
        return switch (principal.getRole()) {
            case CLIENT -> {
                List<Labo> labos = laboratoiresDuClient(principal.getId());
                yield new ProfilAssistant(cle, 20, Set.of(AUDIENCE_TOUS, "client"), tenants(labos),
                        "un client connecté à son espace client (particulier ou entreprise)",
                        noms(labos, "aucun laboratoire pour le moment"), SANS_INFO_CLIENT, "client");
            }
            case SUPER_ADMINISTRATEUR -> {
                List<Labo> labos = laboratoiresActifs();
                yield new ProfilAssistant(cle, 30, Set.of(AUDIENCE_TOUS, "super-admin"), tenants(labos),
                        "le Super Administrateur de la plateforme LabFlow, connecté à son espace",
                        noms(labos, "aucun laboratoire actif"), SANS_INFO_SUPER_ADMIN, "super-admin");
            }
            default -> {
                String role = principal.getRole().name().toLowerCase();
                List<Labo> labos = laboratoireDuPersonnel(principal.getNomSchema());
                yield new ProfilAssistant(cle, 30, Set.of(AUDIENCE_TOUS, "personnel", role), tenants(labos),
                        "un membre du personnel de laboratoire connecté à son espace, avec le rôle " + libelleRole(role),
                        noms(labos, "son laboratoire"), SANS_INFO_PERSONNEL, role);
            }
        };
    }

    // -------------------------------------------------------------------------

    private List<Labo> laboratoiresActifs() {
        return tenantExecutor.inCentral(() -> laboratoireRepository.findByStatutOrderByRaisonSocialeAsc("ACTIF").stream()
                .map(this::labo)
                .toList());
    }

    private List<Labo> laboratoiresDuClient(Long utilisateurId) {
        return tenantExecutor.inCentral(() -> clientLaboratoireRepository.findByUtilisateur_Id(utilisateurId).stream()
                .filter(cl -> "ACTIF".equalsIgnoreCase(cl.getStatut()))
                .filter(cl -> "ACTIF".equalsIgnoreCase(cl.getLaboratoire().getStatut()))
                .map(cl -> labo(cl.getLaboratoire()))
                .toList());
    }

    private List<Labo> laboratoireDuPersonnel(String nomSchema) {
        if (nomSchema == null) return List.of();
        return laboratoiresActifs().stream().filter(l -> nomSchema.equals(l.nomSchema())).toList();
    }

    private Labo labo(Laboratoire l) {
        return new Labo(l.getNomSchema(), IndexationRagService.nomLaboratoire(l));
    }

    /** {@code commun} + schémas valides : la valeur finit dans une expression de filtre. */
    private static Set<String> tenants(List<Labo> labos) {
        Set<String> tenants = new LinkedHashSet<>();
        tenants.add(IndexationRagService.TENANT_COMMUN);
        labos.stream().map(Labo::nomSchema)
                .filter(s -> s != null && s.matches("[A-Za-z0-9_]+"))
                .forEach(tenants::add);
        return tenants;
    }

    private static String noms(List<Labo> labos, String siVide) {
        return labos.isEmpty() ? siVide : labos.stream().map(Labo::nom).collect(Collectors.joining(", "));
    }

    private static String libelleRole(String role) {
        return switch (role) {
            case "accueil" -> "Accueil (réception des échantillons, caisse, patients)";
            case "responsable" -> "Responsable de laboratoire (décision, planification, validation, rapports)";
            case "technicien" -> "Technicien (réalisation des essais et saisie des résultats)";
            case "administrateur" -> "Administrateur du laboratoire (employés, catalogue, stock, facturation)";
            default -> role;
        };
    }
}
