package com.backend.modules.utilisateur.service;

import com.backend.common.audit.Auditable;
import com.backend.common.dto.PageResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.utilisateur.dto.CreerUtilisateurRequest;
import com.backend.modules.utilisateur.dto.ModifierUtilisateurRequest;
import com.backend.modules.utilisateur.dto.UtilisateurDto;
import com.backend.modules.utilisateur.entity.*;
import com.backend.modules.utilisateur.mapper.UtilisateurMapper;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Service de gestion des utilisateurs (M12).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public UtilisateurDto trouverParId(Long id) {
        return utilisateurMapper.toDto(chargerParId(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UtilisateurDto> lister(String search, Pageable pageable) {
        Page<Utilisateur> page = StringUtils.hasText(search)
                ? utilisateurRepository.rechercher(search, pageable)
                : utilisateurRepository.findAll(pageable);
        return PageResponse.from(page.map(utilisateurMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<UtilisateurDto> listerParRole(RoleUtilisateur role, Pageable pageable) {
        return PageResponse.from(
                utilisateurRepository.findByRole(role, pageable)
                        .map(utilisateurMapper::toDto));
    }

    // -------------------------------------------------------------------------
    // Création
    // -------------------------------------------------------------------------

    @Transactional
    @Auditable(action = "CREATION_UTILISATEUR", entite = "Utilisateur")
    public UtilisateurDto creer(CreerUtilisateurRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("EMAIL_DEJA_UTILISE",
                    "L'email '" + request.getEmail() + "' est déjà utilisé");
        }

        Utilisateur utilisateur = construireUtilisateur(request);
        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Utilisateur créé : id={}, email={}, role={}",
                utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole());
        return utilisateurMapper.toDto(utilisateur);
    }

    // -------------------------------------------------------------------------
    // Modification
    // -------------------------------------------------------------------------

    @Transactional
    @Auditable(action = "MODIFICATION_UTILISATEUR", entite = "Utilisateur")
    public UtilisateurDto modifier(Long id, ModifierUtilisateurRequest request) {
        Utilisateur utilisateur = chargerParId(id);

        if (StringUtils.hasText(request.getNom())) utilisateur.setNom(request.getNom());
        if (StringUtils.hasText(request.getPrenom())) utilisateur.setPrenom(request.getPrenom());
        if (StringUtils.hasText(request.getTelephone())) utilisateur.setTelephone(request.getTelephone());

        if (StringUtils.hasText(request.getEmail()) &&
                !request.getEmail().equalsIgnoreCase(utilisateur.getEmail())) {
            if (utilisateurRepository.existsByEmail(request.getEmail())) {
                throw new BusinessRuleException("EMAIL_DEJA_UTILISE",
                        "L'email '" + request.getEmail() + "' est déjà utilisé");
            }
            utilisateur.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getNouveauMotDePasse())) {
            utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        }

        if (request.getActif() != null) utilisateur.setActif(request.getActif());
        if (request.getDeuxFacteursActif() != null)
            utilisateur.setDeuxFacteursActif(request.getDeuxFacteursActif());

        return utilisateurMapper.toDto(utilisateurRepository.save(utilisateur));
    }

    // -------------------------------------------------------------------------
    // Activation / Désactivation
    // -------------------------------------------------------------------------

    @Transactional
    @Auditable(action = "DESACTIVATION_UTILISATEUR", entite = "Utilisateur")
    public void desactiver(Long id) {
        Utilisateur utilisateur = chargerParId(id);
        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);
        log.info("Utilisateur désactivé : id={}", id);
    }

    @Transactional
    @Auditable(action = "ACTIVATION_UTILISATEUR", entite = "Utilisateur")
    public void activer(Long id) {
        Utilisateur utilisateur = chargerParId(id);
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
        log.info("Utilisateur activé : id={}", id);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Utilisateur chargerParId(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
    }

    /**
     * Instancie le bon sous-type d'utilisateur selon le rôle.
     */
    private Utilisateur construireUtilisateur(CreerUtilisateurRequest request) {
        String mdpHash = passwordEncoder.encode(request.getMotDePasse());

        return switch (request.getRole()) {
            case CLIENT -> Client.builder()
                    .nom(request.getNom()).prenom(request.getPrenom())
                    .email(request.getEmail()).motDePasse(mdpHash)
                    .telephone(request.getTelephone()).role(RoleUtilisateur.CLIENT)
                    .actif(true).dateCreation(Instant.now())
                    .build();
            case ACCUEIL -> Accueil.builder()
                    .nom(request.getNom()).prenom(request.getPrenom())
                    .email(request.getEmail()).motDePasse(mdpHash)
                    .telephone(request.getTelephone()).role(RoleUtilisateur.ACCUEIL)
                    .actif(true).dateCreation(Instant.now())
                    .build();
            case TECHNICIEN -> Technicien.builder()
                    .nom(request.getNom()).prenom(request.getPrenom())
                    .email(request.getEmail()).motDePasse(mdpHash)
                    .telephone(request.getTelephone()).role(RoleUtilisateur.TECHNICIEN)
                    .actif(true).dateCreation(Instant.now())
                    .build();
            case RESPONSABLE -> Responsable.builder()
                    .nom(request.getNom()).prenom(request.getPrenom())
                    .email(request.getEmail()).motDePasse(mdpHash)
                    .telephone(request.getTelephone()).role(RoleUtilisateur.RESPONSABLE)
                    .actif(true).dateCreation(Instant.now())
                    .build();
            case ADMINISTRATEUR -> Administrateur.builder()
                    .nom(request.getNom()).prenom(request.getPrenom())
                    .email(request.getEmail()).motDePasse(mdpHash)
                    .telephone(request.getTelephone()).role(RoleUtilisateur.ADMINISTRATEUR)
                    .actif(true).dateCreation(Instant.now())
                    .build();
            case SUPER_ADMINISTRATEUR -> SuperAdministrateur.builder()
                    .nom(request.getNom()).prenom(request.getPrenom())
                    .email(request.getEmail()).motDePasse(mdpHash)
                    .telephone(request.getTelephone()).role(RoleUtilisateur.SUPER_ADMINISTRATEUR)
                    .actif(true).dateCreation(Instant.now())
                    .build();
        };
    }
}
