import apiClient from "./axios";
import type { ApiResponse } from "./auth";

export interface ProfilCompte {
  id: number;
  nom: string;
  prenom?: string | null;
  email: string;
  telephone?: string | null;
  fonction?: string | null;
  role: string;
  doubleAuthentification: boolean;
  derniereConnexion?: string | null;
}

export interface SessionActive {
  id: number;
  userAgent?: string | null;
  creeLe: string;
  expireLe: string;
  courante: boolean;
}

export async function lireProfil(): Promise<ProfilCompte> {
  const res = await apiClient.get<ApiResponse<ProfilCompte>>("/auth/compte");
  return res.data;
}

export async function modifierProfil(payload: {
  nom: string;
  prenom?: string;
  telephone?: string;
  fonction?: string;
}): Promise<ProfilCompte> {
  const res = await apiClient.patch<ApiResponse<ProfilCompte>>("/auth/compte", payload);
  return res.data;
}

export async function listerSessions(): Promise<SessionActive[]> {
  const res = await apiClient.get<ApiResponse<SessionActive[]>>("/auth/compte/sessions");
  return res.data;
}

export async function revoquerSession(id: number): Promise<{ courante: boolean }> {
  const res = await apiClient.delete<ApiResponse<{ courante: boolean }>>(`/auth/compte/sessions/${id}`);
  return res.data;
}

export async function desactiver2FA(code: string): Promise<void> {
  await apiClient.post<ApiResponse<null>>("/auth/2fa/desactiver", { code });
}
