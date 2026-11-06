import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import type { PageResponse } from "./utilisateurs";

export interface ProduitDto {
  id: number;
  reference: string;
  nom: string;
  unite: string | null;
  seuilMinimal: number | null;
  conditionsStockage: string | null;
  actif: boolean;
  stockCourant: number;
}

export interface LotDto {
  id: number;
  numero: string;
  quantite: number;
  peremption: string;
  statut: string;
  produitId: number;
  produitReference: string;
}

export interface CreerProduitRequest {
  reference: string;
  nom: string;
  unite?: string | undefined;
  seuilMinimal?: number | undefined;
  conditionsStockage?: string | undefined;
}

export interface ModifierProduitRequest {
  nom?: string | undefined;
  unite?: string | undefined;
  seuilMinimal?: number | undefined;
  conditionsStockage?: string | undefined;
  actif?: boolean | undefined;
}

export interface CreerLotRequest {
  produitId: number;
  numero: string;
  quantite: number;
  peremption: string;
}

export interface AjusterStockRequest {
  type: "SORTIE" | "PERTE" | "AJUSTEMENT";
  quantite: number;
  motif: string;
}

export interface AlertesStockDto {
  sousSeuil: ProduitDto[];
  rupture: ProduitDto[];
  perimes: LotDto[];
  peremptionProche: LotDto[];
}

export function messageErreurStockAdmin(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) return "Trop de tentatives, réessayez dans un instant.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 404) return "Produit introuvable.";
    if (error.status === 0 || error.message === "Network Error") return "Serveur indisponible.";
    if (error.status === 400) return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

export async function listerProduits(page = 0, size = 50): Promise<PageResponse<ProduitDto>> {
  const res = await apiClient.get<ApiResponse<PageResponse<ProduitDto>>>("/stock-admin/produits", {
    params: { page, size },
  });
  return res.data;
}

export async function getProduit(id: number): Promise<ProduitDto> {
  const res = await apiClient.get<ApiResponse<ProduitDto>>(`/stock-admin/produits/${id}`);
  return res.data;
}

export async function creerProduit(request: CreerProduitRequest): Promise<ProduitDto> {
  const res = await apiClient.post<ApiResponse<ProduitDto>>("/stock-admin/produits", request);
  return res.data;
}

export async function modifierProduit(
  id: number,
  request: ModifierProduitRequest,
): Promise<ProduitDto> {
  const res = await apiClient.put<ApiResponse<ProduitDto>>(`/stock-admin/produits/${id}`, request);
  return res.data;
}

export async function supprimerProduit(id: number): Promise<void> {
  await apiClient.delete(`/stock-admin/produits/${id}`);
}

export async function listerLots(produitId: number): Promise<LotDto[]> {
  const res = await apiClient.get<ApiResponse<LotDto[]>>(`/stock-admin/produits/${produitId}/lots`);
  return res.data;
}

export async function creerLot(request: CreerLotRequest): Promise<LotDto> {
  const res = await apiClient.post<ApiResponse<LotDto>>("/stock-admin/lots", request);
  return res.data;
}

export async function ajusterStock(lotId: number, request: AjusterStockRequest): Promise<LotDto> {
  const res = await apiClient.patch<ApiResponse<LotDto>>(
    `/stock-admin/lots/${lotId}/ajuster`,
    request,
  );
  return res.data;
}

export async function getAlertes(): Promise<AlertesStockDto> {
  const res = await apiClient.get<
    ApiResponse<
      Partial<AlertesStockDto> & {
        enRupture?: ProduitDto[] | undefined;
      }
    >
  >("/stock-admin/alertes");
  const data = res.data;
  return {
    sousSeuil: data.sousSeuil ?? [],
    rupture: data.rupture ?? data.enRupture ?? [],
    perimes: data.perimes ?? [],
    peremptionProche: data.peremptionProche ?? [],
  };
}
