import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import type { PageResponse } from "./utilisateurs";

export interface FactureAdminDto {
  id: number;
  numero: string;
  type: string;
  dateFacture: string;
  dateEcheance: string | null;
  /** Total TTC (les prix du catalogue sont saisis TTC). */
  montantHt: number;
  tauxTva: number;
  montantTva: number;
  montantHorsTaxes: number;
  statut: string;
  demandeId: number | null;
  demandeReference: string | null;
  demandeTitre: string | null;
  clientId: number | null;
  clientNom: string | null;
  clientIce: string | null;
  lignes: FactureLigneAdminDto[];
  paiements: PaiementAdminDto[];
}

export interface FactureLigneAdminDto {
  id: number;
  designation: string;
  montant: number;
}

export interface PaiementAdminDto {
  id: number;
  mode: string;
  reference: string | null;
  montant: number;
  datePaiement: string;
}

export interface EmettreFactureRequest {
  demandeId: number;
  dateEcheance?: string | undefined;
}

export interface DemandeFacturableDto {
  id: number;
  numero: string;
  titre: string;
  clientNom: string | null;
  statut: string;
  dateSoumission: string;
  total: number;
}

export const DEMANDES_FACTURABLES_QUERY_KEY = ["demandes-facturables"] as const;

export interface EnregistrerPaiementRequest {
  mode: "ESPECES" | "VIREMENT" | "CHEQUE" | "CARTE";
  reference?: string | undefined;
}

export interface FactureStatsDto {
  facture: number;
  encaisse: number;
  impaye: number;
  nombreFacturesImpayees: number;
  nombreEnRetard: number;
  serieMensuelle: SerieMensuelleDto[];
}

export interface SerieMensuelleDto {
  mois: string;
  valeur: number;
}

export interface ListerFacturesParams {
  statut?: "EMISE" | "PAYEE" | "EN_RETARD" | "IMPAYEE" | undefined;
  debut?: string | undefined;
  fin?: string | undefined;
  clientId?: number | undefined;
  q?: string | undefined;
  tri?: "numero" | "date" | undefined;
  direction?: "asc" | "desc" | undefined;
  page?: number | undefined;
  size?: number | undefined;
}

export function messageErreurFacturesAdmin(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) return "Trop de tentatives, réessayez dans un instant.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 404) return "Facture introuvable.";
    if (error.status === 0 || error.message === "Network Error") return "Serveur indisponible.";
    if (error.status === 400) return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

export async function listerFactures(params?: ListerFacturesParams): Promise<PageResponse<FactureAdminDto>> {
  const queryParams: Record<string, string | number | boolean> = {};
  if (params) {
    if (params.statut !== undefined) queryParams["statut"] = params.statut;
    if (params.debut !== undefined) queryParams["debut"] = params.debut;
    if (params.fin !== undefined) queryParams["fin"] = params.fin;
    if (params.clientId !== undefined) queryParams["clientId"] = params.clientId;
    if (params.q !== undefined && params.q.trim().length > 0) queryParams["q"] = params.q.trim();
    if (params.tri !== undefined) queryParams["tri"] = params.tri;
    if (params.direction !== undefined) queryParams["direction"] = params.direction;
    if (params.page !== undefined && params.page >= 0) queryParams["page"] = params.page;
    if (params.size !== undefined && params.size > 0) queryParams["size"] = Math.min(params.size, 50);
  }
  const res = await apiClient.get<ApiResponse<PageResponse<FactureAdminDto>>>(
    "/factures-admin",
    { params: queryParams },
  );
  return res.data;
}

export async function getFacture(id: number): Promise<FactureAdminDto> {
  const res = await apiClient.get<ApiResponse<FactureAdminDto>>(`/factures-admin/${id}`);
  return res.data;
}

export async function emettreFacture(request: EmettreFactureRequest): Promise<FactureAdminDto> {
  const res = await apiClient.post<ApiResponse<FactureAdminDto>>("/factures-admin", request);
  return res.data;
}

export async function listerDemandesFacturables(): Promise<DemandeFacturableDto[]> {
  const res = await apiClient.get<ApiResponse<DemandeFacturableDto[]>>(
    "/factures-admin/demandes-facturables",
  );
  return res.data;
}

export async function enregistrerPaiement(id: number, request: EnregistrerPaiementRequest): Promise<FactureAdminDto> {
  const res = await apiClient.post<ApiResponse<FactureAdminDto>>(`/factures-admin/${id}/paiements`, request);
  return res.data;
}

export async function getStats(debut?: string, fin?: string): Promise<FactureStatsDto> {
  const params: Record<string, string> = {};
  if (debut !== undefined) params["debut"] = debut;
  if (fin !== undefined) params["fin"] = fin;
  const res = await apiClient.get<ApiResponse<FactureStatsDto>>("/factures-admin/stats", { params });
  return res.data;
}
