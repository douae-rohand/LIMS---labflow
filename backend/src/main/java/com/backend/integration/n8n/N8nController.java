package com.backend.integration.n8n;

import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.repository.FactureRepository;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.produit.service.StockAdminService;
import com.backend.modules.satisfaction.entity.EnqueteSatisfaction;
import com.backend.modules.satisfaction.repository.EnqueteSatisfactionRepository;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Endpoint de diagnostic de l'authentification n8n. */
@RestController
@RequestMapping("/api/n8n")
@RequiredArgsConstructor
@Slf4j
public class N8nController {

    private final TenantExecutor tenantExecutor;
    private final LaboratoireRepository laboratoireRepository;
    private final EnqueteSatisfactionRepository enqueteRepository;
    private final FactureRepository factureRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StockAdminService stockAdminService;

    @GetMapping("/ping")
    public Map<String, Boolean> ping() {
        return Map.of("ok", true);
    }

    @GetMapping("/enquetes/{laboratoireId}/{enqueteId}/repondue")
    public ResponseEntity<Map<String, Boolean>> enqueteRepondue(@PathVariable Long laboratoireId,
                                                                 @PathVariable Long enqueteId) {
        Laboratoire laboratoire = tenantExecutor.inCentral(
                () -> laboratoireRepository.findById(laboratoireId).orElse(null));
        if (laboratoire == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("repondue", false));
        }
        Boolean repondue = tenantExecutor.inTenant(laboratoire.getNomSchema(), () ->
                enqueteRepository.findById(enqueteId).map(e -> e.getDateReponse() != null).orElse(null));
        if (repondue == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("repondue", false));
        }
        return ResponseEntity.ok(Map.of("repondue", repondue));
    }

    @GetMapping("/factures-echues")
    public List<Map<String, Object>> facturesEchues() {
        List<Laboratoire> laboratoires = tenantExecutor.inCentral(
                () -> laboratoireRepository.findByStatutOrderByRaisonSocialeAsc("ACTIF"));
        List<Map<String, Object>> resultat = new ArrayList<>();
        java.time.LocalDate aujourdHui = java.time.LocalDate.now();
        java.time.LocalDate limiteRelance = aujourdHui.minusDays(7);
        for (Laboratoire laboratoire : laboratoires) {
            try {
                List<Facture> factures = tenantExecutor.inTenant(laboratoire.getNomSchema(),
                        () -> factureRepository.listerEchuesPourRelance(aujourdHui, limiteRelance));
                for (Facture facture : factures) {
                    Long clientId = facture.getDemande().getClient() == null ? null
                            : facture.getDemande().getClient().getUtilisateurId();
                    Utilisateur client = clientId == null ? null
                            : tenantExecutor.inCentral(() -> utilisateurRepository.findById(clientId).orElse(null));
                    if (client == null || !StringUtils.hasText(client.getEmail())) continue;
                    Map<String, Object> ligne = new HashMap<>();
                    ligne.put("laboratoireId", laboratoire.getId());
                    ligne.put("nomLaboratoire", laboratoire.getRaisonSociale());
                    ligne.put("factureId", facture.getId());
                    ligne.put("numeroFacture", facture.getNumero());
                    ligne.put("statut", facture.getStatut());
                    ligne.put("dateEcheance", facture.getDateEcheance());
                    ligne.put("montantHt", facture.getMontantHt());
                    java.math.BigDecimal paye = tenantExecutor.inTenant(laboratoire.getNomSchema(),
                            () -> factureRepository.montantPaye(facture.getId()));
                    ligne.put("montantRestant", facture.getMontantHt().subtract(paye));
                    ligne.put("email", client.getEmail());
                    ligne.put("nom", client.getNomComplet());
                    ligne.put("demandeReference", facture.getDemande().getNumero());
                    resultat.add(ligne);
                }
            } catch (Exception ex) {
                log.warn("Lecture n8n des factures échues ignorée pour laboratoire {} : {}",
                        laboratoire.getId(), ex.getClass().getSimpleName());
            }
        }
        return resultat;
    }

    @org.springframework.web.bind.annotation.PostMapping("/factures/{laboratoireId}/{factureId}/relance-envoyee")
    public ResponseEntity<Map<String, Boolean>> relanceEnvoyee(@PathVariable Long laboratoireId,
                                                               @PathVariable Long factureId) {
        Laboratoire laboratoire = tenantExecutor.inCentral(
                () -> laboratoireRepository.findById(laboratoireId).orElse(null));
        if (laboratoire == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ok", false));
        }
        int lignes = tenantExecutor.inTenant(laboratoire.getNomSchema(), () ->
                factureRepository.findById(factureId).map(f -> factureRepository.marquerRelanceEnvoyee(factureId))
                        .orElse(0));
        if (lignes == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ok", false));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/stock/alertes")
    public List<Map<String, Object>> alertesStock() {
        List<Laboratoire> laboratoires = tenantExecutor.inCentral(
                () -> laboratoireRepository.findByStatutOrderByRaisonSocialeAsc("ACTIF"));
        List<Map<String, Object>> resultat = new ArrayList<>();
        for (Laboratoire laboratoire : laboratoires) {
            try {
                Map<String, List<?>> alertes = tenantExecutor.inTenant(laboratoire.getNomSchema(),
                        stockAdminService::getAlertes);
                List<Utilisateur> admins = tenantExecutor.inCentral(() -> utilisateurRepository
                        .findActifsParRoleEtLaboratoire(RoleUtilisateur.ADMINISTRATEUR.name(), laboratoire.getId())
                        .stream().filter(u -> StringUtils.hasText(u.getEmail())).toList());
                boolean contientAlertes = alertes.values().stream().anyMatch(l -> l != null && !l.isEmpty());
                if (!contientAlertes || admins.isEmpty()) continue;
                Map<String, Object> ligne = new HashMap<>();
                ligne.put("laboratoireId", laboratoire.getId());
                ligne.put("nomLaboratoire", laboratoire.getRaisonSociale());
                ligne.put("emailsAdministrateurs", admins.stream().map(Utilisateur::getEmail).toList());
                ligne.put("alertes", alertes);
                resultat.add(ligne);
            } catch (Exception ex) {
                log.warn("Lecture n8n des alertes de stock ignorée pour laboratoire {} : {}",
                        laboratoire.getId(), ex.getClass().getSimpleName());
            }
        }
        return resultat;
    }
}
