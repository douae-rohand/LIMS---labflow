/**
 * auth.ts — Fonctions et types pour l'API d'authentification.
 *
 * Contrat backend implémenté (Lot B + B2) :
 *
 *  POST /api/auth/login
 *    Corps   : { email: string; motDePasse: string }
 *    Réponse : LoginResponse (voir ci-dessous)
 *    Cookie  : refresh_token (HttpOnly; Secure; SameSite=Strict; Path=/api/auth)
 *              posé uniquement quand l'auth est complète (cas sans 2FA).
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
 *  POST /api/auth/2fa/setup
 *    Corps   : { twoFactorToken?: string }
 *  POST /api/auth/2fa/activer
 *    Corps   : { code: string; twoFactorToken?: string }
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

/** Réponse de POST /api/auth/login. */
export interface LoginResponse {
  accessToken?: string;
  role?: RoleUtilisateur;
  tenantId?: string;
  mustChangePassword?: boolean;
  email?: string;
  nomComplet?: string;
  userId?: number;
  permissions?: string[];
  requiresTwoFactor?: boolean;
  setupRequired?: boolean;
  twoFactorToken?: string;
  /** Pendant la 2FA : durée de validité du code en secondes. */
  expiresIn?: number;
}

/** Réponse de POST /api/auth/refresh. */
export interface RefreshResponse {
  accessToken: string;
  role: RoleUtilisateur;
  tenantId: string;
  mustChangePassword: boolean;
  email: string;
  nomComplet: string;
  userId?: number;
  permissions?: string[];
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

export interface TwoFactorSetupBody {
  twoFactorToken?: string;
}

export interface TwoFactorActiverBody {
  twoFactorToken?: string;
  code: string;
}

export interface TwoFactorSetupResponse {
  otpAuthUrl: string;
}

export interface TwoFactorDesactiverBody {
  motDePasse: string;
  code: string;
}

export interface ChangerMotDePasseBody {
  ancienMotDePasse: string;
  nouveauMotDePasse: string;
}

// ---------------------------------------------------------------------------
// Corps inscription
// ---------------------------------------------------------------------------

/** Correspond exactement aux champs acceptés par InscriptionRequest.java.
 *  La version du consentement est fixée côté serveur — ne pas l'envoyer. */
export interface InscriptionBody {
  typeClient: 'PARTICULIER' | 'ENTREPRISE';
  nom: string;
  prenom: string;  email: string;
  telephone: string;
  motDePasse: string;
  consentementCndp: true;
  raisonSociale?: string | undefined;
  ice?: string | undefined;
  adresse?: string | undefined;
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

export async function valider2FA(body: TwoFactorValiderBody): Promise<LoginResponse> {
  const res = await apiClient.post<ApiResponse<LoginResponse>>('/auth/2fa/valider', body);
  return res.data;
}

/** Nouveau code de connexion par e-mail (au plus une fois par minute). */
export async function renvoyerCode2FA(twoFactorToken: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/2fa/renvoyer', { twoFactorToken });
}

export async function setup2FA(body: TwoFactorSetupBody = {}): Promise<TwoFactorSetupResponse> {
  const res = await apiClient.post<ApiResponse<TwoFactorSetupResponse>>('/auth/2fa/setup', body);
  return res.data;
}

export async function activer2FA(body: TwoFactorActiverBody): Promise<LoginResponse | null> {
  const res = await apiClient.post<ApiResponse<LoginResponse | null>>('/auth/2fa/activer', body);
  return res.data;
}

export async function desactiver2FA(body: TwoFactorDesactiverBody): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/2fa/desactiver', body);
}

/**
 * Déconnexion.
 * Le backend révoque le refresh token (lu depuis le cookie) et efface le cookie.
 */
export async function logout(): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/logout');
}

/**
 * Changement de mot de passe pour l'utilisateur connecté.
 * Requiert un Bearer token valide.
 * Succès : retourne la même structure qu'un login (nouveau jeton + mustChangePassword=false).
 * Erreur 400 : le backend retourne un message précis (mauvais ancien MDP, politique non respectée).
 */
export async function changerMotDePasse(body: ChangerMotDePasseBody): Promise<LoginResponse> {
  const res = await apiClient.post<ApiResponse<LoginResponse>>(
    '/auth/mot-de-passe/changer',
    body,
  );
  return res.data;
}

export async function verifierJetonActivation(token: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/compte/activer/verifier', { token });
}

export async function activerCompte(token: string, motDePasse: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>("/auth/compte/activer", { token, motDePasse });
}

/**
 * Inscription publique d'un client.
 * Retourne toujours 202 — ne révèle pas si l'email existe déjà.
 * En cas de 400, le backend retourne { data: Map<champ, message> }.
 */
export async function inscrire(body: InscriptionBody): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/inscription', body);
}

/**
 * Confirme l'adresse email via le jeton reçu par email.
 * 200 = succès ; 400 = lien invalide, expiré ou déjà utilisé.
 */
export async function confirmerActivation(token: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/activation/confirmer', { token });
}

/**
 * Demande le renvoi du lien d'activation de compte client (CLI-02).
 * Retourne toujours 202 (ou 429 en cas de rate-limit / cooldown).
 */
export async function renvoyerActivation(email: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/activation/renvoyer', { email });
}

/**
 * Demande de réinitialisation de mot de passe.
 * Retourne toujours 202 (même si l'email n'existe pas) — anti-énumération.
 * 429 en cas de cooldown (60 s) ou de limite dépassée.
 */
export async function motDePasseOublie(email: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/mot-de-passe/oublie', { email });
}

/**
 * Réinitialise le mot de passe via le jeton reçu par e-mail.
 * 200 = succès (aucune session créée).
 * 400 = jeton invalide/expiré/utilisé, ou politique non respectée (message du serveur).
 * 429 = limite par IP dépassée.
 * Le jeton n'est PAS consommé si la politique est refusée.
 */
export async function reinitialiserMotDePasse(
  token: string,
  nouveauMotDePasse: string,
): Promise<void> {
  await apiClient.post<ApiResponse<null>>('/auth/mot-de-passe/reinitialiser', {
    token,
    nouveauMotDePasse,
  });
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

/** Extrait SessionUser depuis une réponse de login. */
export function toSessionUser(r: LoginResponse): SessionUser {
  return {
    role: r.role!,
    tenantId: r.tenantId ?? '',
    mustChangePassword: r.mustChangePassword ?? false,
    email: r.email ?? '',
    nomComplet: r.nomComplet ?? '',
    userId: r.userId ?? null,
    permissions: r.permissions ?? [],
  };
}
