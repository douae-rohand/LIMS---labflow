import apiClient from "./axios";
import type { ApiResponse } from "./auth";

/** Compte personnel du Super Administrateur. */
export interface MonCompte {
  id: number;
  email: string;
  nom: string;
  prenom: string | null;
  nomComplet: string;
  dateCreation: string | null;
  derniereConnexion: string | null;
}

export interface ModifierMonCompte {
  nom: string;
  prenom: string;
}

export interface MaSession {
  id: number;
  appareil: string | null;
  ouverture: string;
  expiration: string;
  courante: boolean;
}

export async function lireMonCompte(): Promise<MonCompte> {
  const res = await apiClient.get<ApiResponse<MonCompte>>("/plateforme/mon-compte");
  return res.data;
}

export async function modifierMonCompte(corps: ModifierMonCompte): Promise<MonCompte> {
  const res = await apiClient.patch<ApiResponse<MonCompte>>("/plateforme/mon-compte", corps);
  return res.data;
}

/** Servi sous /auth : le cookie de session n'est envoyé que sur ce chemin. */
export async function listerMesSessions(): Promise<MaSession[]> {
  const res = await apiClient.get<ApiResponse<MaSession[]>>("/auth/mes-sessions");
  return res.data;
}

export async function fermerMaSession(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<null>>(`/auth/mes-sessions/${id}`);
}

export async function fermerMesAutresSessions(): Promise<number> {
  const res = await apiClient.post<ApiResponse<number>>("/auth/mes-sessions/revoquer-autres");
  return res.data;
}
