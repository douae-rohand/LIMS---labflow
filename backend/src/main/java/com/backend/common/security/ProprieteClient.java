package com.backend.common.security;

import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.demande.repository.DemandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Limite les lectures d'un client aux ressources rattachées à ses demandes.
 * Le personnel du laboratoire n'est pas concerné.
 */
@Component
@RequiredArgsConstructor
public class ProprieteClient {

    private final DemandeRepository demandeRepository;

    public void exiger(Demande demande) {
        if (!clientConnecte() || appartient(demande)) {
            return;
        }
        throw introuvable();
    }

    public void exigerDemande(Long demandeId) {
        if (!clientConnecte()) {
            return;
        }
        Demande demande = demandeId == null ? null : demandeRepository.findById(demandeId).orElse(null);
        if (demande == null || !appartient(demande)) {
            throw introuvable();
        }
    }

    public ResourceNotFoundException introuvable() {
        return new ResourceNotFoundException("Ressource introuvable");
    }

    public boolean clientConnecte() {
        return SecurityUtils.principalCourant().isClient();
    }

    private boolean appartient(Demande demande) {
        if (demande == null || demande.getClient() == null) {
            return false;
        }
        Long proprietaire = demande.getClient().getUtilisateurId();
        UtilisateurPrincipal principal = SecurityUtils.principalCourant();
        return proprietaire != null && proprietaire.equals(principal.getId());
    }
}
