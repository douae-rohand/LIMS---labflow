package com.backend.common.exception;

import com.backend.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions HTTP pour toute l'API REST.
 *
 * <p>Toutes les réponses d'erreur utilisent le format {@link ApiResponse}
 * pour une cohérence maximale côté client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // 404 – Ressource introuvable
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Ressource introuvable [{}] : {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 422 – Règle métier violée
    // -------------------------------------------------------------------------

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(
            BusinessRuleException ex, WebRequest request) {
        log.warn("Règle métier violée [{}] : {}", ex.getCode(), ex.getMessage());
        if ("REFRESH_TOKEN_REUSE".equals(ex.getCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Jeton révoqué ou invalide. Veuillez vous reconnecter."));
        }
        if ("2FA_ALREADY_ACTIVE".equals(ex.getCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("[" + ex.getCode() + "] " + ex.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("[" + ex.getCode() + "] " + ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 400 – En-tête X-Tenant-ID absent sur une route qui l'exige
    // -------------------------------------------------------------------------

    @ExceptionHandler(TenantHeaderRequiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantHeaderRequired(
            TenantHeaderRequiredException ex) {
        log.warn("En-tête tenant manquant : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 403 – Accès tenant non autorisé
    // -------------------------------------------------------------------------

    @ExceptionHandler(UnauthorizedTenantException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedTenant(
            UnauthorizedTenantException ex) {
        log.warn("Accès tenant refusé : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 400 – Validation Bean Validation (@Valid)
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            errors.put(field, error.getDefaultMessage());
        });
        log.debug("Erreurs de validation : {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Données de la requête invalides")
                        .data(errors)
                        .build());
    }

    // -------------------------------------------------------------------------
    // 400 – Violation de contrainte JPA / path variable
    // -------------------------------------------------------------------------

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + " : " + cv.getMessage())
                .reduce("", (a, b) -> a + "; " + b);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Contrainte violée : " + message));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUpload(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Fichier trop volumineux. Taille maximale : 10 Mo par document."));
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPart(
            org.springframework.web.multipart.support.MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Document obligatoire manquant : " + ex.getRequestPartName()));
    }



    // -------------------------------------------------------------------------
    // 401 – Authentification échouée
    // -------------------------------------------------------------------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        log.warn("Échec d'authentification : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentification requise : " + ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 403 – Accès refusé (Spring Security)
    // -------------------------------------------------------------------------

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Accès refusé : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Accès refusé : droits insuffisants"));
    }

    // -------------------------------------------------------------------------
    // 500 – Toute autre exception
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(
            Exception ex, WebRequest request) {
        log.error("Erreur inattendue [{}]", request.getDescription(false), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Une erreur interne est survenue. Veuillez réessayer."));
    }
}
