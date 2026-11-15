package com.backend.modules.notification.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.security.SecurityUtils;
import com.backend.modules.notification.dto.NotificationDto;
import com.backend.modules.notification.entity.NotificationReception;
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
    /** Les notifications vivent dans le schéma central, quel que soit le laboratoire de l'utilisateur. */
    private final com.backend.common.tenant.TenantExecutor tenantExecutor;

    @GetMapping("/mes")
    @Operation(summary = "Mes notifications (paginées)")
    public ResponseEntity<ApiResponse<java.util.List<NotificationDto>>> mesNotifications(
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(ApiResponse.success(
                tenantExecutor.inCentral(() ->
                        notificationService.mesNotifications(SecurityUtils.principalCourant().getId(), limite))));
    }

    @GetMapping("/non-lues/count")
    @Operation(summary = "Nombre de notifications non lues")
    public ResponseEntity<ApiResponse<Long>> compterNonLues() {
        return ResponseEntity.ok(ApiResponse.success(
                tenantExecutor.inCentral(() ->
                        notificationService.compterNonLues(SecurityUtils.principalCourant().getId()))));
    }

    @PatchMapping("/{id}/lire")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<ApiResponse<Void>> marquerCommeLue(@PathVariable Long id) {
        Long moi = SecurityUtils.principalCourant().getId();
        tenantExecutor.inCentral(() -> {
            notificationService.marquerCommeLue(id, moi);
            return null;
        });
        return ResponseEntity.ok(ApiResponse.success("Notification lue", null));
    }

    @PatchMapping("/lire-tout")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ResponseEntity<ApiResponse<Void>> marquerToutesCommeLues() {
        Long moi = SecurityUtils.principalCourant().getId();
        tenantExecutor.inCentral(() -> {
            notificationService.marquerToutesCommeLues(moi);
            return null;
        });
        return ResponseEntity.ok(ApiResponse.success("Tout marqué comme lu", null));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMINISTRATEUR') or hasAuthority('PERM_NOTIFICATIONS_GERER')")
    @Operation(summary = "Publier une notification manuellement")
    public ResponseEntity<ApiResponse<NotificationDto>> publier(@RequestBody NotificationDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Notification publiée", notificationService.publier(dto)));
    }
}
