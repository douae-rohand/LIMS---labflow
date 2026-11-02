package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.auth.entity.RefreshToken;
import com.backend.modules.auth.repository.RefreshTokenRepository;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.auth.service.RefreshTokenService;
import com.backend.modules.plateforme.dto.ModifierProfilRequest;
import com.backend.modules.plateforme.dto.ProfilCompteDto;
import com.backend.modules.plateforme.dto.SessionActiveDto;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CompteService {

    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JournalAuditService journalAuditService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProfilCompteDto profil(UtilisateurPrincipal principal) {
        return versProfil(charger(principal.getId()));
    }

    @Transactional
    public ProfilCompteDto modifier(UtilisateurPrincipal principal, ModifierProfilRequest request) {
        Utilisateur utilisateur = charger(principal.getId());
        String avant = snapshot(utilisateur);
        utilisateur.setNom(request.getNom().trim());
        utilisateur.setPrenom(texte(request.getPrenom()));
        utilisateur.setTelephone(texte(request.getTelephone()));
        utilisateur.setFonction(texte(request.getFonction()));
        utilisateurRepository.save(utilisateur);
        journalAuditService.enregistrer(
                "MODIFIER_PROFIL",
                "Utilisateur",
                utilisateur.getId(),
                principal.getEmail(),
                avant,
                snapshot(utilisateur),
                null);
        return versProfil(utilisateur);
    }

    @Transactional(readOnly = true)
    public List<SessionActiveDto> sessions(UtilisateurPrincipal principal, String refreshBrut) {
        String hashCourant = refreshBrut == null || refreshBrut.isBlank()
                ? null
                : RefreshTokenService.hash(refreshBrut);
        return refreshTokenRepository.findActifs(principal.getId(), Instant.now()).stream()
                .map(token -> SessionActiveDto.builder()
                        .id(token.getId())
                        .userAgent(token.getUserAgent())
                        .creeLe(token.getCreatedAt())
                        .expireLe(token.getDateExpiration())
                        .courante(hashCourant != null && hashCourant.equals(token.getTokenHash()))
                        .build())
                .toList();
    }

    @Transactional
    public boolean revoquerSession(UtilisateurPrincipal principal, Long sessionId, String refreshBrut) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));
        if (token.getUtilisateur() == null || !principal.getId().equals(token.getUtilisateur().getId())) {
            throw new BusinessRuleException("SESSION_HORS_COMPTE", "Cette session n'appartient pas à votre compte");
        }
        boolean courante = refreshBrut != null
                && !refreshBrut.isBlank()
                && RefreshTokenService.hash(refreshBrut).equals(token.getTokenHash());
        token.setRevoque(true);
        refreshTokenRepository.save(token);
        journalAuditService.enregistrer(
                "REVOQUER_SESSION",
                "RefreshToken",
                token.getId(),
                principal.getEmail(),
                "{\"revoque\":false}",
                "{\"revoque\":true,\"courante\":" + courante + "}",
                null);
        return courante;
    }

    private Utilisateur charger(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
    }

    private ProfilCompteDto versProfil(Utilisateur utilisateur) {
        return ProfilCompteDto.builder()
                .id(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .telephone(utilisateur.getTelephone())
                .fonction(utilisateur.getFonction())
                .role(utilisateur.getRole() == null ? null : utilisateur.getRole().getCode())
                .doubleAuthentification(utilisateur.isDoubleAuthentification())
                .derniereConnexion(utilisateur.getDerniereConnexion())
                .build();
    }

    private String snapshot(Utilisateur utilisateur) {
        Map<String, Object> valeurs = new HashMap<>();
        valeurs.put("nom", utilisateur.getNom());
        valeurs.put("prenom", utilisateur.getPrenom());
        valeurs.put("telephone", utilisateur.getTelephone());
        valeurs.put("fonction", utilisateur.getFonction());
        try {
            return objectMapper.writeValueAsString(valeurs);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }
}
