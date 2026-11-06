package com.backend.modules.plateforme.service;

import com.backend.common.audit.JournalAudit;
import com.backend.common.audit.JournalAuditRepository;
import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.plateforme.dto.JournalAuditDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class JournalAuditConsultationService {

    private static final int TAILLE_MAX = 100;
    private static final int EXPORT_MAX = 5000;
    private static final Set<String> TRIS = Set.of("dateAction", "action", "objet", "code");

    private final JournalAuditRepository journalAuditRepository;

    @Transactional(readOnly = true)
    public Page<JournalAuditDto> rechercher(String acteur, String action, Long laboratoireId,
                                            LocalDate du, LocalDate au,
                                            int page, int size, String tri, String ordre) {
        String nomSchema = null;
        if (laboratoireId != null) {
            nomSchema = laboratoireId.toString();
        }
        return journalAuditRepository.rechercher(
                        like(acteur),
                null,
                texte(action),
                null,
                        nomSchema,
                        null,
                        debut(du),
                        fin(au),
                        null,
                        page(page, size, tri, ordre))
                .map(this::versDto);
    }

    @Transactional(readOnly = true)
    public List<String> listerActions() {
        return journalAuditRepository.listerActions();
    }

    @Transactional(readOnly = true)
    public byte[] exporterCsv(String acteur, String action, Long laboratoireId, LocalDate du, LocalDate au) {
        String nomSchema = null;
        if (laboratoireId != null) {
            nomSchema = laboratoireId.toString();
        }
        Page<JournalAuditDto> page = journalAuditRepository.rechercher(
                        like(acteur),
                null,
                texte(action),
                null,
                        nomSchema,
                        null,
                        debut(du),
                        fin(au),
                        null,
                        PageRequest.of(0, EXPORT_MAX, Sort.by(Sort.Direction.DESC, "dateAction")))
                .map(this::versDto);
        if (page.getTotalElements() > EXPORT_MAX) {
            throw new BusinessRuleException("EXPORT_TROP_LARGE",
                    "Plus de " + EXPORT_MAX + " lignes correspondent. Affinez les filtres avant d'exporter.");
        }
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("code;date;action;objet;acteur;motif;valeurAvant;valeurApres\n");
        for (JournalAuditDto ligne : page.getContent()) {
            csv.append(cellule(ligne.getCode())).append(';')
                    .append(cellule(ligne.getDateAction() == null ? "" : ligne.getDateAction().toString())).append(';')
                    .append(cellule(ligne.getAction())).append(';')
                    .append(cellule(ligne.getObjet())).append(';')
                    .append(cellule(ligne.getActeurEmail())).append(';')
                    .append(cellule(ligne.getMotif())).append(';')
                    .append(cellule(ligne.getValeurAvant())).append(';')
                    .append(cellule(ligne.getValeurApres())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private JournalAuditDto versDto(JournalAudit entree) {
        String nom = entree.getUtilisateur() == null ? null : entree.getUtilisateur().getNomComplet();
        String email = entree.getUtilisateur() == null ? null : entree.getUtilisateur().getEmail();
        return JournalAuditDto.builder()
                .id(entree.getId())
                .code(entree.getCode())
                .dateAction(entree.getDateAction())
                .action(entree.getAction())
                .objet(entree.getObjet())
                .valeurAvant(entree.getValeurAvant())
                .valeurApres(entree.getValeurApres())
                .motif(entree.getMotif())
                .acteurEmail(email)
                .acteurNom(nom)
                .build();
    }

    private PageRequest page(int page, int size, String tri, String ordre) {
        int pageSafe = Math.max(page, 0);
        int sizeSafe = Math.min(Math.max(size, 1), TAILLE_MAX);
        String champ = TRIS.contains(tri) ? tri : "dateAction";
        Sort.Direction direction = "asc".equalsIgnoreCase(ordre) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(pageSafe, sizeSafe, Sort.by(direction, champ));
    }

    private static String objetLaboratoire(Long laboratoireId) {
        return laboratoireId == null ? null : "Laboratoire#" + laboratoireId;
    }

    private static Instant debut(LocalDate jour) {
        return jour == null ? null : jour.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static Instant fin(LocalDate jour) {
        return jour == null ? null : jour.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static String texte(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return valeur.trim();
    }

    private static String like(String valeur) {
        String texte = texte(valeur);
        if (texte == null) {
            return null;
        }
        return texte.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static String cellule(String valeur) {
        if (valeur == null) {
            return "";
        }
        String texte = valeur;
        int index = 0;
        while (index < texte.length()) {
            char caractere = texte.charAt(index);
            if (caractere != ' ' && caractere != '\t' && caractere != '\r') {
                break;
            }
            index++;
        }
        if (index < texte.length() && "=+-@".indexOf(texte.charAt(index)) >= 0) {
            texte = "'" + texte;
        }
        return "\"" + texte.replace("\"", "\"\"") + "\"";
    }
}
