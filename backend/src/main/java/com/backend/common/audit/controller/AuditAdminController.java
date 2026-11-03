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

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Admin", description = "Journal d'audit pour les administrateurs (M12)")
public class AuditAdminController {

    private final AuditAdminService auditAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<ApiResponse<PageResponse<AuditEntryDto>>> lister(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String typeObjet,
            @RequestParam(required = false) String acteur,
            @RequestParam(required = false) Instant debut,
            @RequestParam(required = false) Instant fin,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Page<AuditEntryDto> pageResult = 
                auditAdminService.lister(action, typeObjet, acteur, debut, fin, q,
                        org.springframework.data.domain.PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(pageResult)));
    }
}
