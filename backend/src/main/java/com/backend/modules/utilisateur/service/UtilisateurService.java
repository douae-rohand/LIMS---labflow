package com.backend.modules.utilisateur.service;

import com.backend.common.audit.Auditable;
import com.backend.common.dto.PageResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.modules.client.service.ClientService;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import com.backend.modules.utilisateur.dto.CreerUtilisateurRequest;
import com.backend.modules.utilisateur.dto.ModifierUtilisateurRequest;
import com.backend.modules.utilisateur.dto.UtilisateurDto;
import com.backend.modules.utilisateur.entity.Role;
import com.backend.modules.utilisateur.entity.RoleUtilisateur;
import com.backend.modules.utilisateur.entity.Utilisateur;
import com.backend.modules.utilisateur.mapper.UtilisateurMapper;
import com.backend.modules.utilisateur.repository.RoleRepository;
import com.backend.modules.utilisateur.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final LaboratoireRepository laboratoireRepository;
    private final ClientService clientService;
    private final UtilisateurMapper utilisateurMapper;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    @Auditable(action = "CREATION_UTILISATEUR", entite = "Utilisateur")
    public UtilisateurDto creer(CreerUtilisateurRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("EMAIL_DEJA_UTILISE",
                    "L'email '" + request.getEmail() + "' est déjà utilisé");
        }

        Role role = roleRepository.findByCode(request.getRole().name())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role", "code", request.getRole().name()));

        Laboratoire laboratoire = null;
        if (estPersonnelLaboratoire(request.getRole())) {
            if (request.getLaboratoireId() == null) {
                throw new BusinessRuleException("LABORATOIRE_OBLIGATOIRE",
                        "Un employé de laboratoire doit être rattaché à un laboratoire");
            }
            laboratoire = laboratoireRepository.findById(request.getLaboratoireId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Laboratoire", "id", request.getLaboratoireId()));
        } else if (request.getLaboratoireId() != null) {
            throw new BusinessRuleException("LABORATOIRE_INTERDIT",
                    "Un compte " + request.getRole() + " ne doit pas avoir de laboratoire fixe");
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .motDePasseHash(passwordEncoder.encode(request.getMotDePasse()))
                .telephone(request.getTelephone())
                .role(role)
                .laboratoire(laboratoire)
                .actif(true)
                .build();
        utilisateur = utilisateurRepository.save(utilisateur);

        if (request.getRole() == RoleUtilisateur.CLIENT) {
            clientService.creerProfil(utilisateur, request.getRaisonSociale(),
                    request.getIce(), request.getAdresse(), request.getConsentementCndp());
        }

        log.info("Utilisateur créé : id={}, email={}, role={}",
                utilisateur.getId(), utilisateur.getEmail(), utilisateur.getRole());
        return utilisateurMapper.toDto(utilisateur);
    }

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
            utilisateur.setMotDePasseHash(passwordEncoder.encode(request.getNouveauMotDePasse()));
        }

        if (request.getActif() != null) utilisateur.setActif(request.getActif());
        if (request.getDeuxFacteursActif() != null)
            utilisateur.setDoubleAuthentification(request.getDeuxFacteursActif());

        return utilisateurMapper.toDto(utilisateurRepository.save(utilisateur));
    }

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

    private Utilisateur chargerParId(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
    }

    private static boolean estPersonnelLaboratoire(RoleUtilisateur role) {
        return role == RoleUtilisateur.ACCUEIL
                || role == RoleUtilisateur.TECHNICIEN
                || role == RoleUtilisateur.RESPONSABLE
                || role == RoleUtilisateur.ADMINISTRATEUR;
    }
}
