import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import type { PageResponse } from "./utilisateurs";

export interface SatisfactionStatsDto {
  envoyees: number;
  repondues: number;
  tauxReponse: number;
  moyenneGlobale: number;
  moyenneDelai: number;
  moyenneClarte: number;
  moyenneRelation: number;
  repartitionNotes: Record<number, number>;
  serieMensuelle: SerieMensuelleSatisfactionDto[];
}

export interface SerieMensuelleSatisfactionDto {
  mois: string;
  valeur: number;
}

export interface EnqueteReponseDto {
  id: number;
  code: string;
  dateReponse: string;
  demandeNumero: string;
  clientNom: string | null;
  note: number | null;
  delaiNote: number | null;
  clarteNote: number | null;
  relationNote: number | null;
  commentaire: string | null;
}

export interface ListerEnquetesParams {
  debut?: string | undefined;
  fin?: string | undefined;
  note?: number | undefined;
  avecCommentaire?: boolean | undefined;
  page?: number | undefined;
  size?: number | undefined;
}

export function messageErreurSatisfactionAdmin(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) return "Trop de tentatives, réessayez dans un instant.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 0 || error.message === "Network Error") return "Serveur indisponible.";
    if (error.status === 400) return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

export async function getStats(debut?: string, fin?: string): Promise<SatisfactionStatsDto> {
  const params: Record<string, string> = {};
  if (debut !== undefined) params["debut"] = debut;
  if (fin !== undefined) params["fin"] = fin;
  const res = await apiClient.get<ApiResponse<SatisfactionStatsDto>>("/satisfaction/stats", {
    params,
  });
  return res.data;
}

export async function listerEnquetes(
  params?: ListerEnquetesParams,
): Promise<PageResponse<EnqueteReponseDto>> {
  const queryParams: Record<string, string | number | boolean> = {};
  if (params) {
    if (params.debut !== undefined) queryParams["debut"] = params.debut;
    if (params.fin !== undefined) queryParams["fin"] = params.fin;
    if (params.note !== undefined) queryParams["note"] = params.note;
    if (params.avecCommentaire !== undefined)
      queryParams["avecCommentaire"] = params.avecCommentaire;
    if (params.page !== undefined && params.page >= 0) queryParams["page"] = params.page;
    if (params.size !== undefined && params.size > 0)
      queryParams["size"] = Math.min(params.size, 50);
  }
  const res = await apiClient.get<ApiResponse<PageResponse<unknown[]>>>("/satisfaction/enquetes", {
    params: queryParams,
  });
  return {
    ...res.data,
    content: res.data.content.map((row) => {
      if (
        row.length < 11 ||
        typeof row[0] !== "number" ||
        typeof row[1] !== "string" ||
        typeof row[3] !== "string" ||
        typeof row[9] !== "string" ||
        (row[10] !== null && typeof row[10] !== "string") ||
        (row[4] !== null && typeof row[4] !== "number")
      ) {
        throw new Error("Format de réponse des enquêtes de satisfaction invalide.");
      }

      return {
        id: row[0],
        code: row[1],
        dateReponse: row[3],
        demandeNumero: row[9],
        clientNom: typeof row[10] === "string" ? row[10] : null,
        note: row[4],
        delaiNote: typeof row[5] === "number" ? row[5] : null,
        clarteNote: typeof row[6] === "number" ? row[6] : null,
        relationNote: typeof row[7] === "number" ? row[7] : null,
        commentaire: typeof row[8] === "string" ? row[8] : null,
      };
    }),
  };
}
