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
  email?: string;
  nomComplet?: string;

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
  email: string;
  nomComplet: string;
}

/** Réponse de POST /api/auth/2fa/setup. */
export interface TwoFactorSetupResponse {
  qrCodeUri: string;
  secret: string;
}

/** Réponse de POST /api/auth/refresh. */
export interface RefreshResponse {
  accessToken: string;
  role: RoleUtilisateur;
  tenantId: string;
  mustChangePassword: boolean;
  email: string;
  nomComplet: string;
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

/**
 * Corps de POST /api/auth/2fa/setup.
 *
 * Deux cas :
 *  - Enrôlement obligé (RESPONSABLE_LABO / ADMINISTRATEUR sans 2FA activé) :
 *    twoFactorToken présent (obtenu à l'étape login, cas b).
 *  - Activation volontaire (SUPER_ADMINISTRATEUR connecté) :
 *    twoFactorToken absent (la session active fournit l'identité via accessToken).
 */
export interface TwoFactorSetupBody {
  twoFactorToken?: string;
}

export interface TwoFactorActiverBody {
  twoFactorToken?: string;
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
 *
 * Deux cas :
 *  (enrôlement forcé)  twoFactorToken issu du login (cas b) → dans le corps.
 *  (activation libre)   pas de twoFactorToken → l'accessToken en mémoire suffit.
 *
 * Le twoFactorToken doit être effacé de la mémoire du consommateur après l'appel.
 */
export async function setup2FA(body: TwoFactorSetupBody = {}): Promise<TwoFactorSetupResponse> {
  const res = await apiClient.post<ApiResponse<TwoFactorSetupResponse>>('/auth/2fa/setup', body);
  return res.data;
}

/**
 * Active le 2FA en soumettant le premier code TOTP valide.
 *
 * twoFactorToken optionnel :
 *  - Présent si l'activation fait suite à un enrôlement forcé (même jeton que setup).
 *  - Absent si c'est une activation volontaire (l'accessToken en mémoire suffit).
 *
 * Pose le cookie refresh_token et retourne l'accessToken.
 * Après succès OU échec, le consommateur doit effacer le twoFactorToken de sa mémoire.
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
  await apiClient.post<ApiResponse<null>>(
    `/auth/compte/activer/verifier?token=${encodeURIComponent(token)}`,
  );
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

/** Extrait SessionUser depuis une réponse de login complète (cas c). */
export function toSessionUser(r: LoginResponse): SessionUser {
  return {
    role: r.role!,
    tenantId: r.tenantId ?? '',
    mustChangePassword: r.mustChangePassword ?? false,
    email: r.email ?? '',
    nomComplet: r.nomComplet ?? '',
  };
}

/** Extrait SessionUser depuis une réponse de complétion 2FA. */
export function toSessionUserFrom2FA(r: TwoFactorCompleteResponse): SessionUser {
  return {
    role: r.role,
    tenantId: r.tenantId,
    mustChangePassword: r.mustChangePassword,
    email: r.email,
    nomComplet: r.nomComplet,
  };
}
