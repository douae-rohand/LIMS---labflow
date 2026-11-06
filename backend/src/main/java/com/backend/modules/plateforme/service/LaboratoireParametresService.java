package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAuditService;
import com.backend.common.exception.BusinessRuleException;
import com.backend.common.security.SecurityUtils;
import com.backend.common.tenant.TenantExecutor;
import com.backend.modules.plateforme.dto.LaboratoireParametresDto;
import com.backend.modules.plateforme.dto.ModifierLaboratoireParametresRequest;
import com.backend.modules.plateforme.entity.Laboratoire;
import com.backend.modules.plateforme.repository.LaboratoireRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LaboratoireParametresService {

    private final LaboratoireRepository laboratoireRepository;
    private final TenantExecutor tenantExecutor;
    private final JournalAuditService journalAuditService;

    private static final Set<String> TYPES_LABORATOIRE_AUTORISES = Set.of(
            "Médical", "Industriel", "Environnemental", "Pharmaceutique et cosmétique"
    );

    @Transactional(readOnly = true)
    public LaboratoireParametresDto getParametres() {
        return tenantExecutor.inCentral(() -> {
            Long laboratoireId = SecurityUtils.principalCourant().getLaboratoireId();
            if (laboratoireId == null) {
                throw new BusinessRuleException("ADMIN_SANS_LABORATOIRE",
                        "Administrateur sans laboratoire rattaché");
            }
            Laboratoire laboratoire = laboratoireRepository.findById(laboratoireId)
                    .orElseThrow(() -> new BusinessRuleException("LABORATOIRE_NON_TROUVE",
                            "Laboratoire non trouvé"));
            return toDto(laboratoire);
        });
    }

    @Transactional
    public LaboratoireParametresDto modifierParametres(ModifierLaboratoireParametresRequest request) {
        return tenantExecutor.inCentral(() -> {
            Long laboratoireId = SecurityUtils.principalCourant().getLaboratoireId();
            if (laboratoireId == null) {
                throw new BusinessRuleException("ADMIN_SANS_LABORATOIRE",
                        "Administrateur sans laboratoire rattaché");
            }
            Laboratoire laboratoire = laboratoireRepository.findById(laboratoireId)
                    .orElseThrow(() -> new BusinessRuleException("LABORATOIRE_NON_TROUVE",
                            "Laboratoire non trouvé"));

            if (request.getTypeLaboratoire() != null) {
                Set<String> typesFournis = new HashSet<>(Set.of(request.getTypeLaboratoire().split(",\\s*")));
                for (String type : typesFournis) {
                    if (!TYPES_LABORATOIRE_AUTORISES.contains(type.trim())) {
                        throw new BusinessRuleException("TYPE_LABORATOIRE_INVALIDE",
                                "Type de laboratoire non autorisé : " + type);
                    }
                }
            }

            Set<String> champsModifies = new HashSet<>();
            if (request.getRaisonSociale() != null && !request.getRaisonSociale().equals(laboratoire.getRaisonSociale())) {
                laboratoire.setRaisonSociale(request.getRaisonSociale());
                champsModifies.add("raisonSociale");
            }
            if (request.getAdresse() != null && !request.getAdresse().equals(laboratoire.getAdresse())) {
                laboratoire.setAdresse(request.getAdresse());
                champsModifies.add("adresse");
            }
            if (request.getVille() != null && !request.getVille().equals(laboratoire.getVille())) {
                laboratoire.setVille(request.getVille());
                champsModifies.add("ville");
            }
            if (request.getRegion() != null && !request.getRegion().equals(laboratoire.getRegion())) {
                laboratoire.setRegion(request.getRegion());
                champsModifies.add("region");
            }
            if (request.getCodePostal() != null && !request.getCodePostal().equals(laboratoire.getCodePostal())) {
                laboratoire.setCodePostal(request.getCodePostal());
                champsModifies.add("codePostal");
            }
            if (request.getPays() != null && !request.getPays().equals(laboratoire.getPays())) {
                laboratoire.setPays(request.getPays());
                champsModifies.add("pays");
            }
            if (request.getTelephone() != null && !request.getTelephone().equals(laboratoire.getTelephone())) {
                laboratoire.setTelephone(request.getTelephone());
                champsModifies.add("telephone");
            }
            if (request.getEmail() != null && !request.getEmail().equals(laboratoire.getEmail())) {
                laboratoire.setEmail(request.getEmail());
                champsModifies.add("email");
            }
            if (request.getSiteWeb() != null && !request.getSiteWeb().equals(laboratoire.getSiteWeb())) {
                laboratoire.setSiteWeb(request.getSiteWeb());
                champsModifies.add("siteWeb");
            }
            if (request.getTypeLaboratoire() != null && !request.getTypeLaboratoire().equals(laboratoire.getTypeLaboratoire())) {
                laboratoire.setTypeLaboratoire(request.getTypeLaboratoire());
                champsModifies.add("typeLaboratoire");
            }
            if (request.getLatitude() != null && !request.getLatitude().equals(laboratoire.getLatitude())) {
                laboratoire.setLatitude(request.getLatitude());
                champsModifies.add("latitude");
            }
            if (request.getLongitude() != null && !request.getLongitude().equals(laboratoire.getLongitude())) {
                laboratoire.setLongitude(request.getLongitude());
                champsModifies.add("longitude");
            }

            if (champsModifies.isEmpty()) {
                return toDto(laboratoire);
            }

            laboratoire = laboratoireRepository.save(laboratoire);

            journalAuditService.enregistrer("MODIFICATION_LABORATOIRE", "Laboratoire", laboratoire.getId(),
                    SecurityUtils.principalCourant().getEmail(), null,
                    "{\"champs\": " + champsModifies + "}", null);

            return toDto(laboratoire);
        });
    }

    private LaboratoireParametresDto toDto(Laboratoire laboratoire) {
        return LaboratoireParametresDto.builder()
                .id(laboratoire.getId())
                .code(laboratoire.getCode())
                .raisonSociale(laboratoire.getRaisonSociale())
                .adresse(laboratoire.getAdresse())
                .ville(laboratoire.getVille())
                .region(laboratoire.getRegion())
                .codePostal(laboratoire.getCodePostal())
                .pays(laboratoire.getPays())
                .telephone(laboratoire.getTelephone())
                .email(laboratoire.getEmail())
                .siteWeb(laboratoire.getSiteWeb())
                .typeLaboratoire(laboratoire.getTypeLaboratoire())
                .latitude(laboratoire.getLatitude())
                .longitude(laboratoire.getLongitude())
                .build();
    }
}
