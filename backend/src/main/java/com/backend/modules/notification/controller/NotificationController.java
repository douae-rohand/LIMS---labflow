package com.backend.modules.notification.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.modules.notification.dto.NotificationDto;
import com.backend.modules.notification.entity.NotificationDestinataire;
import com.backend.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestion des notifications temps réel et email")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/mes")
    @Operation(summary = "Mes notifications (paginées)")
    public ResponseEntity<ApiResponse<Page<NotificationDestinataire>>> mesNotifications(
            @RequestParam Long utilisateurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.mesNotifications(utilisateurId, PageRequest.of(page, size))));
    }

    @GetMapping("/non-lues/count")
    @Operation(summary = "Nombre de notifications non lues")
    public ResponseEntity<ApiResponse<Long>> compterNonLues(@RequestParam Long utilisateurId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.compterNonLues(utilisateurId)));
    }

    @PatchMapping("/{id}/lire")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<ApiResponse<Void>> marquerCommeLue(@PathVariable Long id) {
        notificationService.marquerCommeLue(id);
        return ResponseEntity.ok(ApiResponse.success("Notification lue", null));
    }

    @PatchMapping("/lire-tout")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ResponseEntity<ApiResponse<Void>> marquerToutesCommeLues(@RequestParam Long utilisateurId) {
        notificationService.marquerToutesCommeLues(utilisateurId);
        return ResponseEntity.ok(ApiResponse.success("Tout marqué comme lu", null));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE','ADMINISTRATEUR','SUPER_ADMINISTRATEUR')")
    @Operation(summary = "Publier une notification manuellement")
    public ResponseEntity<ApiResponse<NotificationDto>> publier(@RequestBody NotificationDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Notification publiée", notificationService.publier(dto)));
    }
}
