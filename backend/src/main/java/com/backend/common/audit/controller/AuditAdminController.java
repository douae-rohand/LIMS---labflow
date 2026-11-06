package com.backend.common.audit.controller;

import com.backend.common.audit.dto.AuditEntryDto;
import com.backend.common.audit.service.AuditAdminService;
import com.backend.common.dto.ApiResponse;
import com.backend.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Admin", description = "Journal d'audit pour les administrateurs (M12)")
public class AuditAdminController {

    private final AuditAdminService auditAdminService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_AUDIT_CONSULTER')")
    public ResponseEntity<ApiResponse<PageResponse<AuditEntryDto>>> lister(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String typeObjet,
            @RequestParam(required = false) String acteur,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String debut,
            @RequestParam(required = false) String fin,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Instant debutInstant = null;
        Instant finInstant = null;
        if (debut != null && !debut.isBlank()) {
            debutInstant = LocalDate.parse(debut).atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        if (fin != null && !fin.isBlank()) {
            finInstant = LocalDate.parse(fin).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        }
        org.springframework.data.domain.Page<AuditEntryDto> pageResult =
                auditAdminService.lister(action, typeObjet, acteur, role, debutInstant, finInstant, q,
                        org.springframework.data.domain.PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(pageResult)));
    }
}
