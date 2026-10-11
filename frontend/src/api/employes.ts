import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import type { PageResponse } from "./utilisateurs";

// ---------------------------------------------------------------------------
// Types du domaine Employé
// ---------------------------------------------------------------------------

export type StatutEmploye = "ACTIF" | "EN_ATTENTE" | "DESACTIVE";

export type RoleEmploye = "RESPONSABLE" | "TECHNICIEN" | "ACCUEIL";

export type TriEmploye =
  | "nom"
  | "prenom"
  | "matricule"
  | "dateCreation"
  | "derniereConnexion";

export type DirectionTri = "asc" | "desc";

export interface EmployeDto {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  nomComplet: string;
  email: string;
  telephone: string | null;
  role: RoleEmploye;
  statut: StatutEmploye;
  derniereConnexion: string | null;
  dateCreation: string;
}

export interface EmployeDetailDto extends EmployeDto {
  actif: boolean;
  compteConfirme: boolean;
}

export interface ListerEmployesParams {
  q?: string | undefined;
  statut?: StatutEmploye | undefined;
  role?: RoleEmploye | undefined;
  page?: number | undefined;
  size?: number | undefined;
  tri?: TriEmploye | undefined;
  direction?: DirectionTri | undefined;
}

export interface ModifierEmployePayload {
  nom?: string | undefined;
  prenom?: string | undefined;
  telephone?: string | undefined;
  role?: RoleEmploye | undefined;
  email?: string | undefined;
}

// ---------------------------------------------------------------------------
// Gestion d'erreur normalisée
// ---------------------------------------------------------------------------

/**
 * Traduit et normalise les erreurs API en messages clairs et lisibles côté utilisateur.
 * Ne retourne jamais d'objet d'erreur brut.
 */
export function messageErreurApi(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) {
      return "Trop de tentatives, réessayez dans un instant.";
    }
    if (error.status === 403) {
      return "Accès refusé.";
    }
    if (error.status === 404) {
      return "Employé introuvable.";
    }
    // Erreur réseau (pas de réponse HTTP du serveur, status 0 ou message réseau axios)
    if (error.status === 0 || error.message === "Network Error") {
      return "Serveur indisponible.";
    }
    if (error.status === 400 && typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

// ---------------------------------------------------------------------------
// Fonctions API Employés
// ---------------------------------------------------------------------------

/**
 * Récupère la liste paginée et filtrée des employés du laboratoire courant.
 * Plafonne le paramètre size à 50 maximum. N'envoie aucun paramètre vide ou indéfini.
 */
export async function listerEmployes(
  params?: ListerEmployesParams,
): Promise<PageResponse<EmployeDto>> {
  const queryParams: Record<string, string | number> = {};

  if (params) {
    if (params.q !== undefined && params.q.trim().length > 0) {
      queryParams["q"] = params.q.trim();
    }
    if (params.statut !== undefined && params.statut.length > 0) {
      queryParams["statut"] = params.statut;
    }
    if (params.role !== undefined && params.role.length > 0) {
      queryParams["role"] = params.role;
    }
    if (params.page !== undefined && params.page >= 0) {
      queryParams["page"] = params.page;
    }
    if (params.size !== undefined && params.size > 0) {
      queryParams["size"] = Math.min(params.size, 50);
    }
    if (params.tri !== undefined && params.tri.length > 0) {
      queryParams["tri"] = params.tri;
    }
    if (params.direction !== undefined && params.direction.length > 0) {
      queryParams["direction"] = params.direction;
    }
  }

  const res = await apiClient.get<ApiResponse<PageResponse<EmployeDto>>>(
    "/employes",
    { params: queryParams },
  );
  return res.data;
}

/**
 * Récupère le détail complet d'un employé par son identifiant.
 */
export async function trouverEmploye(id: number): Promise<EmployeDetailDto> {
  const res = await apiClient.get<ApiResponse<EmployeDetailDto>>(`/employes/${id}`);
  return res.data;
}

/**
 * Modifie les données d'un employé.
 */
export async function modifierEmploye(
  id: number,
  payload: ModifierEmployePayload,
): Promise<EmployeDetailDto> {
  const res = await apiClient.patch<ApiResponse<EmployeDetailDto>>(
    `/employes/${id}`,
    payload,
  );
  return res.data;
}

/**
 * Désactive un employé actif.
 */
export async function desactiverEmploye(id: number): Promise<EmployeDetailDto> {
  const res = await apiClient.patch<ApiResponse<EmployeDetailDto>>(
    `/employes/${id}/desactiver`,
  );
  return res.data;
}

/**
 * Réactive un employé désactivé.
 */
export async function reactiverEmploye(id: number): Promise<EmployeDetailDto> {
  const res = await apiClient.patch<ApiResponse<EmployeDetailDto>>(
    `/employes/${id}/reactiver`,
  );
  return res.data;
}

/**
 * Renvoie une invitation par e-mail à un employé en attente d'activation.
 */
export async function renvoyerInvitationEmploye(id: number): Promise<EmployeDetailDto> {
  const res = await apiClient.post<ApiResponse<EmployeDetailDto>>(
    `/employes/${id}/renvoyer-invitation`,
  );
  return res.data;
}
