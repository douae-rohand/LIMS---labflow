/**
 * auth.ts — Fonctions et types pour l'API d'authentification.
 *
 * Contrat backend implémenté (Lot B + B2) :
 *
 *  POST /api/auth/login
 *    Corps   : { email: string; motDePasse: string }
 *    Réponse : LoginResponse (voir ci-dessous)
 *    Cookie  : refresh_token (HttpOnly; Secure; SameSite=Strict; Path=/api/auth)
 *              posé uniquement quand l'auth est complète (cas c).
 *
 *  POST /api/auth/refresh
 *    Corps   : vide — le refresh token est dans le cookie HttpOnly
 *    Réponse : { accessToken: string }
 *    Cookie  : nouveau refresh_token (rotation)
 *
 *  POST /api/auth/logout
 *    Corps   : vide — le refresh token est dans le cookie HttpOnly
 *    Réponse : ApiResponse<null>
 *    Cookie  : refresh_token effacé (Max-Age=0)
 *
 *  POST /api/auth/2fa/valider
 *    Corps   : { twoFactorToken: string; code: string }
 *    Réponse : { accessToken: string; role; tenantId; mustChangePassword }
 *    Cookie  : refresh_token posé
 *
 *  POST /api/auth/2fa/setup
 *    En-tête : Authorization: Bearer <twoFactorToken>  (type=2fa)
 *    Réponse : { qrCodeUri: string; secret: string }
 *
 *  POST /api/auth/2fa/activer
 *    En-tête : Authorization: Bearer <twoFactorToken>  (type=2fa)
 *    Corps   : { code: string }
 *    Réponse : { accessToken: string; role; tenantId; mustChangePassword }
 *    Cookie  : refresh_token posé
 */

import apiClient from './axios';
import type { RoleUtilisateur, SessionUser } from './session';

// ---------------------------------------------------------------------------
// Enveloppe standard backend
// ---------------------------------------------------------------------------

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

// ---------------------------------------------------------------------------
// Types de réponse
// ---------------------------------------------------------------------------

/**
 * Réponse de POST /api/auth/login.
 *
 * Trois cas mutuellement exclusifs :
 *  (a) 2FA requis et activé    → requiresTwoFactor=true,  setupRequired=false, twoFactorToken défini
 *  (b) 2FA requis mais non activé → requiresTwoFactor=true, setupRequired=true, twoFactorToken défini
 *  (c) Pas de 2FA requis       → accessToken défini, cookie refresh_token posé
 */
export interface LoginResponse {
  // --- Cas (c) uniquement ---
  accessToken?: string;
  role?: RoleUtilisateur;
  tenantId?: string;
  mustChangePassword?: boolean;

  // --- Cas (a) et (b) ---
  requiresTwoFactor?: boolean;
  setupRequired?: boolean;
  twoFactorToken?: string;
}

/** Réponse de POST /api/auth/2fa/valider et POST /api/auth/2fa/activer. */
export interface TwoFactorCompleteResponse {
  accessToken: string;
  role: RoleUtilisateur;
  tenantId: string;
  mustChangePassword: boolean;
}

/** Réponse de POST /api/auth/2fa/setup. */
export interface TwoFactorSetupResponse {
  qrCodeUri: string;
  secret: string;
}

/** Réponse de POST /api/auth/refresh. */
export interface RefreshResponse {
  accessToken: string;
}

// ---------------------------------------------------------------------------
// Corps de requêtes
// ---------------------------------------------------------------------------

export interface LoginBody {
  email: string;
  motDePasse: string;
}

export interface TwoFactorValiderBody {
  twoFactorToken: string;
  code: string;
}

export interface TwoFactorActiverBody {
  code: string;
}

// ---------------------------------------------------------------------------
// Fonctions API
// ---------------------------------------------------------------------------

/**
 * Connexion initiale.
 * Le refresh token est posé automatiquement dans le cookie HttpOnly par le backend.
 * Ne jamais logger la valeur de `accessToken`.
 */
export async function login(body: LoginBody): Promise<LoginResponse> {
  const res = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', body);
  return res.data;
}

/**
 * Validation du code TOTP après login quand le 2FA est activé.
 * Pose le cookie refresh_token et retourne l'accessToken.
 */
export async function valider2FA(body: TwoFactorValiderBody): Promise<TwoFactorCompleteResponse> {
  const res = await apiClient.post<ApiResponse<TwoFactorCompleteResponse>>(
    '/auth/2fa/valider',
    body,
  );
  return res.data;
}

/**
 * Récupère le QR code pour configurer l'application TOTP.
 * Requiert un twoFactorToken (type=2fa) dans l'Authorization header —
 * à passer via apiClient.setTwoFactorToken() avant l'appel.
 */
export async function setup2FA(): Promise<TwoFactorSetupResponse> {
  const res = await apiClient.post<ApiResponse<TwoFactorSetupResponse>>('/auth/2fa/setup');
  return res.data;
}

/**
 * Active le 2FA en soumettant le premier code TOTP valide.
 * Pose le cookie refresh_token et retourne l'accessToken.
 */
export async function activer2FA(body: TwoFactorActiverBody): Promise<TwoFactorCompleteResponse> {
  const res = await apiClient.post<ApiResponse<TwoFactorCompleteResponse>>(
    '/auth/2fa/activer',
    body,
  );
  return res.data;
}

/**
 * Déconnexion.
 * Le backend révoque le refresh token (lu depuis le cookie) et efface le cookie.
 */
export async function logout(): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/logout');
}

/**
 * Rafraîchissement de l'accessToken.
 * Le refresh token est envoyé automatiquement via le cookie HttpOnly.
 * Ne pas appeler directement : utiliser l'intercepteur 401 de ApiClient.
 */
export async function refreshTokens(): Promise<RefreshResponse> {
  const res = await apiClient.postForRefresh<ApiResponse<RefreshResponse>>('/auth/refresh');
  // res.data est ApiResponse<RefreshResponse> ; l'accessToken est dans res.data.data
  return res.data.data;
}

// ---------------------------------------------------------------------------
// Helpers de typage
// ---------------------------------------------------------------------------

/** Extrait SessionUser depuis une réponse de login complète (cas c). */
export function toSessionUser(r: LoginResponse): SessionUser {
  return {
    role: r.role!,
    tenantId: r.tenantId!,
    mustChangePassword: r.mustChangePassword ?? false,
  };
}

/** Extrait SessionUser depuis une réponse de complétion 2FA. */
export function toSessionUserFrom2FA(r: TwoFactorCompleteResponse): SessionUser {
  return {
    role: r.role,
    tenantId: r.tenantId,
    mustChangePassword: r.mustChangePassword,
  };
}
