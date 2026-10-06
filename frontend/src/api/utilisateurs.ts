/**
 * utilisateurs.ts — Client API pour la gestion des utilisateurs internes.
 *
 * Endpoints backend utilisés :
 *  GET    /api/utilisateurs                   → lister
 *  GET    /api/utilisateurs/role/{role}        → lister par rôle
 *  POST   /api/utilisateurs                   → créer (flux invitation)
 *  PATCH  /api/utilisateurs/{id}/desactiver   → désactiver
 *  PATCH  /api/utilisateurs/{id}/activer      → réactiver
 *  POST   /api/utilisateurs/{id}/renvoyer-invitation → renvoyer email
 */

import apiClient from './axios';
import type { RoleUtilisateur } from './session';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface UtilisateurDto {
  id: number;
  nom: string;
  prenom: string;
  nomComplet: string;
  email: string;
  telephone: string | null;
  role: RoleUtilisateur;
  actif: boolean;
  compteConfirme: boolean;
  deuxFacteursActif: boolean;
  dateCreation: string | null;
  derniereConnexion: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}

/** Payload de création d'un utilisateur interne (flux invitation sans mot de passe). */
export interface CreerUtilisateurInterne {
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  role: 'RESPONSABLE' | 'TECHNICIEN' | 'ACCUEIL';
}

// ---------------------------------------------------------------------------
// Fonctions API
// ---------------------------------------------------------------------------

/** Lister les utilisateurs avec pagination et recherche optionnelle. */
export async function listerUtilisateurs(params: {
  search?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<UtilisateurDto>> {
  const queryParams: Record<string, string | number> = {
    page: params.page ?? 0,
    size: params.size ?? 20,
  };
  if (params.search) {
    queryParams['search'] = params.search;
  }
  const res = await apiClient.get<ApiEnvelope<PageResponse<UtilisateurDto>>>(
    '/utilisateurs',
    { params: queryParams }
  );
  return res.data;
}

/** Lister les utilisateurs par rôle. */
export async function listerParRole(
  role: RoleUtilisateur,
  params: { page?: number; size?: number } = {}
): Promise<PageResponse<UtilisateurDto>> {
  const res = await apiClient.get<ApiEnvelope<PageResponse<UtilisateurDto>>>(
    `/utilisateurs/role/${role}`,
    { params: { page: params.page ?? 0, size: params.size ?? 20 } }
  );
  return res.data;
}

/** Créer un utilisateur interne (le compte est créé inactif, email d'invitation envoyé). */
export async function creerUtilisateur(data: CreerUtilisateurInterne): Promise<UtilisateurDto> {
  const res = await apiClient.post<ApiEnvelope<UtilisateurDto>>('/utilisateurs', data);
  return res.data;
}

/** Désactiver un utilisateur. */
export async function desactiverUtilisateur(id: number): Promise<void> {
  await apiClient.patch(`/utilisateurs/${id}/desactiver`);
}

/** Réactiver un utilisateur. */
export async function activerUtilisateur(id: number): Promise<void> {
  await apiClient.patch(`/utilisateurs/${id}/activer`);
}

/** Renvoyer l'email d'invitation à un utilisateur dont le compte n'est pas encore activé. */
export async function renvoyerInvitation(id: number): Promise<void> {
  await apiClient.post(`/utilisateurs/${id}/renvoyer-invitation`);
}
