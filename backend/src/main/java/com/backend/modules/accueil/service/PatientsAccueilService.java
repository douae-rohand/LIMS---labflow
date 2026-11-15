package com.backend.modules.accueil.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.exception.ResourceNotFoundException;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.accueil.dto.ClientsDtos.PatientDto;
import com.backend.modules.accueil.dto.ClientsDtos.PatientRequest;
import com.backend.modules.accueil.repository.PatientAccueilRepository;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.patient.entity.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Fiches patients du laboratoire (schéma tenant), enregistrées à l'accueil avec le consentement
 * CNDP du patient (loi 09-08), tracé dans le journal d'audit. Code {@code PAT-AAAAMM-NNN}.
 */
@Service
@RequiredArgsConstructor
public class PatientsAccueilService {

    private static final ZoneId FUSEAU = ZoneId.of("Africa/Casablanca");
    private static final DateTimeFormatter MOIS = DateTimeFormatter.ofPattern("yyyyMM");

    private final PatientAccueilRepository patientRepository;
    private final JournalAuditService journalAuditService;

    @Transactional(readOnly = true)
    public List<PatientDto> lister() {
        return patientRepository.tous().stream().map(PatientsAccueilService::versDto).toList();
    }

    @Transactional
    public PatientDto creer(PatientRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        verifierCin(request.cin(), null);
        String prefixe = "PAT-" + LocalDate.now(FUSEAU).format(MOIS) + "-";
        Integer dernier = patientRepository.dernierNumero(prefixe + "%");
        Patient patient = new Patient();
        patient.setCode(prefixe + String.format(Locale.ROOT, "%03d", (dernier == null ? 0 : dernier) + 1));
        appliquer(patient, request);
        patient = patientRepository.save(patient);
        journalAuditService.enregistrer("CREER_PATIENT", "Patient", patient.getId(), acteur.getEmail(), null,
                "{\"code\":\"" + patient.getCode() + "\",\"consentementCndp\":true}", null, acteur.getLaboratoireId());
        return versDto(patient);
    }

    @Transactional
    public PatientDto modifier(Long id, PatientRequest request) {
        UtilisateurPrincipal acteur = SecurityUtils.principalCourant();
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
        verifierCin(request.cin(), id);
        appliquer(patient, request);
        journalAuditService.enregistrer("MODIFIER_PATIENT", "Patient", id, acteur.getEmail(), null,
                "{\"code\":\"" + patient.getCode() + "\"}", null, acteur.getLaboratoireId());
        return versDto(patientRepository.save(patient));
    }

    private void verifierCin(String cin, Long id) {
        if (StringUtils.hasText(cin) && patientRepository.cinUtilise(cin.trim(), id)) {
            throw new BusinessRuleException("CIN_DEJA_ENREGISTRE", "Un patient est déjà enregistré avec ce CIN.");
        }
    }

    private static void appliquer(Patient patient, PatientRequest r) {
        patient.setNom(r.nom().trim());
        patient.setPrenom(r.prenom().trim());
        patient.setDateNaissance(r.dateNaissance());
        patient.setSexe(texte(r.sexe()));
        patient.setCin(StringUtils.hasText(r.cin()) ? r.cin().trim().toUpperCase(Locale.ROOT) : null);
        patient.setTelephone(texte(r.telephone()));
        patient.setEmail(StringUtils.hasText(r.email()) ? r.email().trim().toLowerCase(Locale.ROOT) : null);
        patient.setAdresse(texte(r.adresse()));
    }

    private static String texte(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    private static PatientDto versDto(Patient p) {
        return new PatientDto(p.getId(), p.getCode(), p.getNom(), p.getPrenom(), p.getDateNaissance(), p.getSexe(),
                p.getCin(), p.getTelephone(), p.getEmail(), p.getAdresse());
    }
}
