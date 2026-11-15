package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.auth.entity.RefreshToken;
import com.backend.modules.auth.repository.RefreshTokenRepository;
import com.backend.modules.auth.service.RefreshTokenService;
import com.backend.modules.plateforme.dto.MonCompteDtos.MaSessionDto;
import com.backend.modules.plateforme.dto.MonCompteDtos.ModifierMonCompteRequest;
import com.backend.modules.plateforme.dto.MonCompteDtos.MonCompteDto;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Espace « Mon compte » du Super Administrateur : identité, sessions.
 * Le changement de mot de passe réutilise {@code POST /api/auth/mot-de-passe/changer}.
 */
@Service
@RequiredArgsConstructor
public class MonCompteService {

    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JournalAuditService journalAuditService;

    @Transactional(readOnly = true)
    public MonCompteDto obtenir(Long id) {
        return versDto(charger(id));
    }

    @Transactional
    public MonCompteDto modifier(Long id, ModifierMonCompteRequest request, String acteurEmail) {
        Utilisateur compte = charger(id);
        String avant = snapshot(compte);
        compte.setNom(request.nom().trim());
        compte.setPrenom(texte(request.prenom()));
        utilisateurRepository.save(compte);
        journalAuditService.enregistrer("MODIFIER_MON_COMPTE", "Utilisateur", compte.getId(), acteurEmail,
                avant, snapshot(compte), null);
        return versDto(compte);
    }

    @Transactional(readOnly = true)
    public List<MaSessionDto> sessions(Long id, Optional<String> jetonCourant) {
        String hashCourant = jetonCourant.map(RefreshTokenService::hash).orElse(null);
        return refreshTokenRepository.listerActifs(id, Instant.now()).stream()
                .map(jeton -> new MaSessionDto(jeton.getId(), jeton.getUserAgent(), jeton.getCreatedAt(),
                        jeton.getDateExpiration(), jeton.getTokenHash().equals(hashCourant)))
                .toList();
    }

    @Transactional
    public void revoquerSession(Long id, Long sessionId, Optional<String> jetonCourant, String acteurEmail) {
        RefreshToken jeton = refreshTokenRepository.findById(sessionId)
                .filter(j -> j.getUtilisateur() != null && id.equals(j.getUtilisateur().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        String hashCourant = jetonCourant.map(RefreshTokenService::hash).orElse(null);
        if (jeton.getTokenHash().equals(hashCourant)) {
            throw new BusinessRuleException("SESSION_COURANTE",
                    "Pour fermer la session en cours, utilisez « Se déconnecter ».");
        }
        if (jeton.isRevoque()) {
            return;
        }
        jeton.setRevoque(true);
        refreshTokenRepository.save(jeton);
        journalAuditService.enregistrer("REVOQUER_MA_SESSION", "Utilisateur", id, acteurEmail,
                null, "{\"session\":" + sessionId + "}", null);
    }

    /** Ferme toutes les sessions sauf celle du navigateur appelant. */
    @Transactional
    public int revoquerAutres(Long id, Optional<String> jetonCourant, String acteurEmail) {
        String hashCourant = jetonCourant.map(RefreshTokenService::hash).orElse(null);
        int fermees = 0;
        for (RefreshToken jeton : refreshTokenRepository.listerActifs(id, Instant.now())) {
            if (jeton.getTokenHash().equals(hashCourant)) {
                continue;
            }
            jeton.setRevoque(true);
            refreshTokenRepository.save(jeton);
            fermees++;
        }
        if (fermees > 0) {
            journalAuditService.enregistrer("REVOQUER_AUTRES_SESSIONS", "Utilisateur", id, acteurEmail,
                    null, "{\"sessionsRevoquees\":" + fermees + "}", null);
        }
        return fermees;
    }

    private Utilisateur charger(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
    }

    private static MonCompteDto versDto(Utilisateur u) {
        return new MonCompteDto(u.getId(), u.getEmail(), u.getNom(), u.getPrenom(), u.getNomComplet(),
                u.getDateCreation(), u.getDerniereConnexion());
    }

    private static String snapshot(Utilisateur u) {
        return "{\"nom\":" + json(u.getNom()) + ",\"prenom\":" + json(u.getPrenom()) + "}";
    }

    private static String json(String valeur) {
        if (valeur == null) {
            return "null";
        }
        return "\"" + valeur.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }
}
