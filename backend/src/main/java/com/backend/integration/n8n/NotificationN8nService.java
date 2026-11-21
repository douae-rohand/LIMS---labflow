package com.backend.integration.n8n;

import com.backend.common.tenant.TenantExecutor;
import com.backend.common.util.ApresCommit;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/** Résout en central le destinataire et le laboratoire, puis publie après commit. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationN8nService {

    private final NotificationEventPublisher publisher;
    private final TenantExecutor tenantExecutor;
    private final UtilisateurRepository utilisateurRepository;
    private final LaboratoireRepository laboratoireRepository;

    public void notifierClientApresCommit(String type, String nomSchema, Long utilisateurId,
                                          Map<String, Object> champs) {
        notifierApresCommit(type, nomSchema, utilisateurId, champs);
    }

    public void notifierTechnicienApresCommit(String type, String nomSchema, Long utilisateurId,
                                              Map<String, Object> champs) {
        notifierApresCommit(type, nomSchema, utilisateurId, champs);
    }

    private void notifierApresCommit(String type, String nomSchema, Long utilisateurId,
                                     Map<String, Object> champs) {
        Map<String, Object> champsCaptures = new HashMap<>(champs);
        ApresCommit.executer(() -> {
            try {
                Destinataire destinataire = utilisateurId == null ? null
                        : tenantExecutor.inCentral(() -> utilisateurRepository.findById(utilisateurId)
                                .map(utilisateur -> versDestinataire(utilisateur, nomSchema))
                                .orElse(null));
                if (destinataire == null || !StringUtils.hasText(destinataire.email())) {
                    log.warn("Publication n8n ignorée : destinataire sans e-mail (type={})", type);
                    return;
                }

                Map<String, Object> payload = new HashMap<>(champsCaptures);
                payload.put("email", destinataire.email());
                payload.put("nom", destinataire.nom());
                if (StringUtils.hasText(destinataire.nomLaboratoire())) {
                    payload.put("nomLaboratoire", destinataire.nomLaboratoire());
                }
                publisher.publier(type, nomSchema, payload);
            } catch (RuntimeException ex) {
                log.warn("Publication n8n ignorée : type={}, erreur={}", type,
                        ex.getClass().getSimpleName());
            }
        });
    }

    private Destinataire versDestinataire(Utilisateur utilisateur, String nomSchema) {
        Laboratoire laboratoire = StringUtils.hasText(nomSchema)
                ? laboratoireRepository.findByNomSchema(nomSchema).orElse(null)
                : null;
        return new Destinataire(utilisateur.getEmail(), utilisateur.getNomComplet(),
                laboratoire == null ? null : laboratoire.getRaisonSociale());
    }

    private record Destinataire(String email, String nom, String nomLaboratoire) {
    }
}
