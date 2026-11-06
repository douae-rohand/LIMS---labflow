import apiClient from "./axios";
import type { ApiResponse } from "./auth";

export type EffetPermission = "AJOUT" | "RETRAIT";

export interface LignePermissionEmploye {
  code: string;
  module: string | null;
  action: string | null;
  libelle: string;
  parDefaut: boolean;
  ecart: EffetPermission | null;
  effective: boolean;
  deleguable: boolean;
}

export interface PermissionsEmploye {
  personnalisable: boolean;
  personnalisees: boolean;
  permissions: LignePermissionEmploye[];
  actionsNonDelegables: string[];
}

export interface EcartPermissionEmploye {
  code: string;
  effet: EffetPermission;
}

export async function lirePermissionsEmploye(id: number): Promise<PermissionsEmploye> {
  const res = await apiClient.get<ApiResponse<PermissionsEmploye>>(`/employes/${id}/permissions`);
  return res.data;
}

export async function remplacerPermissionsEmploye(
  id: number,
  ecarts: EcartPermissionEmploye[],
): Promise<PermissionsEmploye> {
  const res = await apiClient.put<ApiResponse<PermissionsEmploye>>(
    `/employes/${id}/permissions`,
    { ecarts },
  );
  return res.data;
}

export async function retablirPermissionsEmploye(id: number): Promise<PermissionsEmploye> {
  const res = await apiClient.delete<ApiResponse<PermissionsEmploye>>(
    `/employes/${id}/permissions`,
  );
  return res.data;
}
