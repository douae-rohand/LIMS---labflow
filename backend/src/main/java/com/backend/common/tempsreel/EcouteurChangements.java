package com.backend.common.tempsreel;

import com.backend.common.audit.JournalAudit;
import com.backend.common.tenant.TenantContext;
import com.backend.modules.demande.entity.Demande;
import com.backend.modules.echantillon.entity.Echantillon;
import com.backend.modules.essai.entity.LigneEssai;
import com.backend.modules.facturation.entity.Facture;
import com.backend.modules.facturation.entity.Paiement;
import com.backend.modules.plateforme.entity.DemandeIntegration;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.rapport.entity.Rapport;
import com.backend.modules.satisfaction.entity.EnqueteSatisfaction;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.validation.entity.Validation;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Relève, <b>après commit</b>, chaque création, modification ou suppression d'une entité métier
 * et la transmet au {@link DiffuseurTempsReel}. Aucun service métier n'a besoin d'être modifié :
 * tout ce qui est enregistré en base est signalé, quel que soit l'espace qui l'a fait.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcouteurChangements implements PostCommitInsertEventListener, PostCommitUpdateEventListener,
        PostCommitDeleteEventListener {

    /** Domaine fonctionnel de chaque entité suivie (nom simple de la classe). */
    private static final Map<String, String> DOMAINES = Map.ofEntries(
            Map.entry("Demande", "DEMANDES"), Map.entry("PieceJointe", "DEMANDES"),
            Map.entry("LigneEssai", "ANALYSES"), Map.entry("Echantillon", "ECHANTILLONS"),
            Map.entry("Validation", "VALIDATIONS"), Map.entry("Rapport", "RAPPORTS"), Map.entry("RapportLigne", "RAPPORTS"),
            Map.entry("Facture", "FACTURES"), Map.entry("FactureLigne", "FACTURES"), Map.entry("Paiement", "FACTURES"),
            Map.entry("Produit", "STOCK"), Map.entry("Lot", "STOCK"), Map.entry("ConsommationLot", "STOCK"),
            Map.entry("Essai", "CATALOGUE"), Map.entry("ExigenceEchantillon", "CATALOGUE"), Map.entry("Domaine", "CATALOGUE"),
            Map.entry("EnqueteSatisfaction", "SATISFACTION"), Map.entry("Satisfaction", "SATISFACTION"),
            Map.entry("Client", "CLIENTS"), Map.entry("Patient", "CLIENTS"),
            Map.entry("Utilisateur", "EMPLOYES"), Map.entry("JournalAudit", "AUDIT"),
            Map.entry("DemandeIntegration", "PLATEFORME"), Map.entry("DocumentIntegration", "PLATEFORME"),
            Map.entry("Laboratoire", "PLATEFORME"));

    private final EntityManagerFactory entityManagerFactory;
    private final DiffuseurTempsReel diffuseur;

    @PostConstruct
    void enregistrer() {
        EventListenerRegistry registre = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry().requireService(EventListenerRegistry.class);
        registre.appendListeners(EventType.POST_COMMIT_INSERT, this);
        registre.appendListeners(EventType.POST_COMMIT_UPDATE, this);
        registre.appendListeners(EventType.POST_COMMIT_DELETE, this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        relever(event.getEntity());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        relever(event.getEntity());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        relever(event.getEntity());
    }

    @Override
    public void onPostInsertCommitFailed(PostInsertEvent event) {
        // Transaction annulée : rien n'a changé, rien à signaler.
    }

    @Override
    public void onPostUpdateCommitFailed(PostUpdateEvent event) {
    }

    @Override
    public void onPostDeleteCommitFailed(PostDeleteEvent event) {
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return DOMAINES.containsKey(persister.getMappedClass().getSimpleName());
    }

    private void relever(Object entite) {
        try {
            String domaine = DOMAINES.get(entite.getClass().getSimpleName());
            if (domaine == null) {
                return;
            }
            String schema = TenantContext.getCurrentTenant();
            Long laboratoire = null;
            String refType = null;
            Long refId = null;
            switch (entite) {
                case Demande d -> { refType = "DEMANDE"; refId = d.getId(); }
                case LigneEssai l -> { refType = "DEMANDE"; refId = id(l.getDemande()); }
                case Echantillon e -> { refType = "DEMANDE"; refId = id(e.getDemande()); }
                case Facture f -> { refType = "DEMANDE"; refId = id(f.getDemande()); }
                case Rapport r -> { refType = "DEMANDE"; refId = id(r.getDemande()); }
                case EnqueteSatisfaction q -> { refType = "DEMANDE"; refId = id(q.getDemande()); }
                case Paiement p -> { refType = "FACTURE"; refId = id(p.getFacture()); }
                case Validation v -> { refType = "LIGNE"; refId = id(v.getLigneEssai()); }
                case Utilisateur u -> { schema = null; laboratoire = id(u.getLaboratoire()); }
                case JournalAudit j -> { schema = null; laboratoire = id(j.getLaboratoire()); }
                case DemandeIntegration i -> { schema = null; laboratoire = id(i.getLaboratoire()); }
                case Laboratoire l -> { schema = null; laboratoire = l.getId(); }
                default -> { }
            }
            diffuseur.signaler(new DiffuseurTempsReel.Changement(domaine, schema, laboratoire, refType, refId));
        } catch (RuntimeException ex) {
            log.debug("Changement non signalé : {}", ex.getMessage());
        }
    }

    /** Identifiant d'une association, sans charger l'entité si c'est un proxy. */
    private static Long id(Object entite) {
        if (entite == null) {
            return null;
        }
        if (entite instanceof HibernateProxy proxy) {
            Object id = proxy.getHibernateLazyInitializer().getIdentifier();
            return id instanceof Long l ? l : null;
        }
        return switch (entite) {
            case Demande d -> d.getId();
            case Facture f -> f.getId();
            case LigneEssai l -> l.getId();
            case Laboratoire l -> l.getId();
            default -> null;
        };
    }
}
