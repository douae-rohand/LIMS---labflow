import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";

// ---------------------------------------------------------------------------
// Types du domaine Paramètres Laboratoire
// ---------------------------------------------------------------------------

export interface LaboratoireParametresDto {
  id: number;
  code: string;
  raisonSociale: string;
  adresse: string | null;
  ville: string | null;
  region: string | null;
  codePostal: string | null;
  pays: string | null;
  telephone: string | null;
  email: string | null;
  siteWeb: string | null;
  typeLaboratoire: string | null;
  latitude: number | null;
  longitude: number | null;
}

export interface ModifierLaboratoireParametresRequest {
  raisonSociale?: string | undefined;
  adresse?: string | undefined;
  ville?: string | undefined;
  region?: string | undefined;
  codePostal?: string | undefined;
  pays?: string | undefined;
  telephone?: string | undefined;
  email?: string | undefined;
  siteWeb?: string | undefined;
  typeLaboratoire?: string | undefined;
  latitude?: number | undefined;
  longitude?: number | undefined;
}

// ---------------------------------------------------------------------------
// Gestion d'erreur normalisée
// ---------------------------------------------------------------------------

export function messageErreurParametres(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) return "Trop de tentatives, réessayez dans un instant.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 404) return "Laboratoire introuvable.";
    if (error.status === 0 || error.message === "Network Error") return "Serveur indisponible.";
    if (error.status === 400) return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

// ---------------------------------------------------------------------------
// Fonctions API Paramètres Laboratoire
// ---------------------------------------------------------------------------

export async function getParametres(): Promise<LaboratoireParametresDto> {
  const res = await apiClient.get<ApiResponse<LaboratoireParametresDto>>("/laboratoire/parametres");
  return res.data;
}

export async function modifierParametres(
  request: ModifierLaboratoireParametresRequest,
): Promise<LaboratoireParametresDto> {
  const res = await apiClient.patch<ApiResponse<LaboratoireParametresDto>>(
    "/laboratoire/parametres",
    request,
  );
  return res.data;
}
