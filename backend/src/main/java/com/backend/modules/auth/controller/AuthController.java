package com.backend.modules.auth.controller;

import com.backend.common.dto.ApiResponse;
import com.backend.common.exception.BusinessRuleException;
import com.backend.modules.auth.dto.*;
import com.backend.modules.auth.security.UtilisateurPrincipal;
import com.backend.modules.auth.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Endpoints d'authentification (briques JWT & Cookie HttpOnly).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Login, refresh token via cookie HttpOnly, 2FA, logout")
public class AuthController {

    private final AuthService authService;
    private final MotDePasseService motDePasseService;
    private final RefreshCookieService refreshCookieService;
    private final InscriptionService inscriptionService;
    private final ActivationService activationService;
    private final InscriptionRateLimiter rateLimiter;
    private final ActivationCompteService activationCompteService;

    // -------------------------------------------------------------------------
    // Inscription publique (client)
    // -------------------------------------------------------------------------

    @PostMapping("/inscription")
    @Operation(summary = "Inscription d'un nouveau client (public)")
    public ResponseEntity<ApiResponse<Void>> inscrire(
            @Valid @RequestBody InscriptionRequest request,
            HttpServletRequest httpRequest) {

        String ip = rateLimiter.extraireIp(httpRequest);
        if (rateLimiter.estLimite(ip, request.getEmail())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Trop de tentatives. Réessayez plus tard."));
        }

        // Toujours 202 — ne permet pas l'énumération des emails
        inscriptionService.inscrire(request);
        return ResponseEntity.accepted()
                .body(ApiResponse.success(
                        "Si cet email est valide, un lien d'activation vous a été envoyé.", null));
    }

    // -------------------------------------------------------------------------
    // Confirmation d'activation (client)
    // -------------------------------------------------------------------------

    @PostMapping("/activation/confirmer")
    @Operation(summary = "Confirme le compte via le jeton reçu par email")
    public ResponseEntity<ApiResponse<Void>> confirmerActivation(
            @Valid @RequestBody ActivationRequest request) {
        activationService.confirmerCompte(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(
                "Compte activé. Vous pouvez maintenant vous connecter.", null));
    }

    // -------------------------------------------------------------------------
    // Connexion
    // -------------------------------------------------------------------------

    @PostMapping("/login")
    @Operation(summary = "Connexion avec email et mot de passe")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        Object result = authService.login(request, httpRequest);

        if (result instanceof AuthService.LoginResult loginResult) {
            ResponseCookie cookie = refreshCookieService.creerCookieRefreshToken(loginResult.rawRefreshToken());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(ApiResponse.success("Connexion réussie", loginResult.response()));
        } else if (result instanceof LoginResponse response) {
            return ResponseEntity.ok(ApiResponse.success("Vérification 2FA requise", response));
        }

        throw new IllegalStateException("Résultat de connexion inattendu");
    }

    // -------------------------------------------------------------------------
    // Authentification à deux facteurs — validation
    // -------------------------------------------------------------------------

    @PostMapping("/2fa/valider")
    @Operation(summary = "Validation du code TOTP (2FA)")
    public ResponseEntity<ApiResponse<LoginResponse>> validerTwoFactor(
            @Valid @RequestBody TwoFactorRequest request,
            HttpServletRequest httpRequest) {
        AuthService.LoginResult loginResult = authService.validerTwoFactor(request, httpRequest);
        ResponseCookie cookie = refreshCookieService.creerCookieRefreshToken(loginResult.rawRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("2FA validée", loginResult.response()));
    }

    // -------------------------------------------------------------------------
    // Refresh token (sans corps, via Cookie HttpOnly)
    // -------------------------------------------------------------------------

    @PostMapping("/refresh")
    @Operation(summary = "Renouvellement du token d'accès via le cookie HttpOnly refreshToken")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(HttpServletRequest httpRequest) {
        Optional<String> optionalRefreshToken = refreshCookieService.extraireRefreshToken(httpRequest);
        if (optionalRefreshToken.isEmpty()) {
            ResponseCookie clearCookie = refreshCookieService.effacerCookieRefreshToken();
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(ApiResponse.error("Jeton de rafraîchissement absent"));
        }

        try {
            AuthService.LoginResult loginResult = authService.refreshToken(optionalRefreshToken.get(), httpRequest);
            ResponseCookie cookie = refreshCookieService.creerCookieRefreshToken(loginResult.rawRefreshToken());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(ApiResponse.success("Token renouvelé", loginResult.response()));
        } catch (BusinessRuleException ex) {
            ResponseCookie clearCookie = refreshCookieService.effacerCookieRefreshToken();
            return ResponseEntity.status(401)
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(ApiResponse.error("Jeton révoqué ou invalide. Veuillez vous reconnecter."));
        }
    }

    // -------------------------------------------------------------------------
    // Logout — nécessite un access token valide (Bearer) + efface le cookie
    // -------------------------------------------------------------------------

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Déconnexion (révocation du refresh token du cookie et effacement du cookie)")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        Optional<String> optionalRefreshToken = refreshCookieService.extraireRefreshToken(httpRequest);
        optionalRefreshToken.ifPresent(token -> authService.logout(token, principal));

        ResponseCookie clearCookie = refreshCookieService.effacerCookieRefreshToken();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(ApiResponse.success("Déconnexion réussie", null));
    }

    // -------------------------------------------------------------------------
    // Changement de mot de passe (utilisateur connecté)
    // -------------------------------------------------------------------------

    @PostMapping("/mot-de-passe/changer")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Changer son mot de passe (nécessite un access token valide)")
    public ResponseEntity<ApiResponse<LoginResponse>> changerMotDePasse(
            @Valid @RequestBody ChangerMotDePasseRequest request,
            @AuthenticationPrincipal UtilisateurPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthService.LoginResult loginResult = motDePasseService.changerMotDePasse(request, principal, httpRequest);
        ResponseCookie cookie = refreshCookieService.creerCookieRefreshToken(loginResult.rawRefreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("Mot de passe changé avec succès", loginResult.response()));
    }

    // -------------------------------------------------------------------------
    // 2FA — Enrôlement : génération du secret (setup)
    // -------------------------------------------------------------------------

    @PostMapping("/2fa/setup")
    @Operation(summary = "Génère le secret TOTP et retourne l'URL QR code")
    public ResponseEntity<ApiResponse<SetupTwoFactorResponse>> setupTwoFactor(
            @RequestBody(required = false) SetupTwoFactorRequest request,
            @AuthenticationPrincipal UtilisateurPrincipal principal) {
        SetupTwoFactorResponse response = authService.setupTwoFactor(request, principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Secret TOTP généré. Scannez le QR code puis appelez /2fa/activer.",
                response));
    }

    // -------------------------------------------------------------------------
    // 2FA — Enrôlement : activation après vérification du premier code
    // -------------------------------------------------------------------------

    @PostMapping("/2fa/activer")
    @Operation(summary = "Active la 2FA après vérification du premier code TOTP")
    public ResponseEntity<ApiResponse<LoginResponse>> activerTwoFactor(
            @Valid @RequestBody ActivateTwoFactorRequest request,
            @AuthenticationPrincipal UtilisateurPrincipal principal,
            HttpServletRequest httpRequest) {
        AuthService.LoginResult loginResult = authService.activerTwoFactor(request, principal, httpRequest);
        if (loginResult != null) {
            ResponseCookie cookie = refreshCookieService.creerCookieRefreshToken(loginResult.rawRefreshToken());
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(ApiResponse.success("2FA activée avec succès. Connexion établie.", loginResult.response()));
        }
        return ResponseEntity.ok(ApiResponse.success("2FA activée avec succès", null));
    }

    @PostMapping("/compte/activer/verifier")
    @Operation(summary = "Vérifie un jeton d'activation de compte (public)")
    public ResponseEntity<ApiResponse<Void>> verifierActivation(
            @RequestParam String token) {
        activationCompteService.verifier(token);
        return ResponseEntity.ok(ApiResponse.success("Jeton d'activation valide", null));
    }

    @PostMapping("/compte/activer")
    @Operation(summary = "Active un compte administrateur et définit le mot de passe (public)")
    public ResponseEntity<ApiResponse<Void>> activerCompte(
            @Valid @RequestBody ActiverCompteRequest request) {
        activationCompteService.activer(request.getToken(), request.getMotDePasse());
        return ResponseEntity.ok(ApiResponse.success(
                "Compte activé. Vous pouvez maintenant vous connecter.", null));
    }
}
