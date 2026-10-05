package com.backend.modules.auth.dto;

/**
 * Réponse de l'endpoint {@code POST /api/auth/2fa/setup}.
 *
 * <p>Le client doit encoder {@code otpAuthUrl} en QR code et le faire scanner
 * par une application TOTP (Google Authenticator, Authy, etc.).
 */
public record SetupTwoFactorResponse(
        /** URL otpauth:// à encoder en QR code. */
        String otpAuthUrl
) {}
