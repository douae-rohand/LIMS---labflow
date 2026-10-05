import { apiClient } from "./axios";

/** Enveloppe {@code ApiResponse<T>} renvoyée par le backend. */
export type ApiEnvelope<T> = {
  success: boolean;
  message?: string | null;
  data?: T | null;
  timestamp?: string;
};

export type RolePublic = {
  id?: number | null;
  code: string;
  libelle: string;
};

export type LaboratoirePublic = {
  id: number;
  code: string;
  raisonSociale: string;
  ville?: string | null;
  adresse?: string | null;
  statut: string;
};

export type LandingStatistiques = {
  nombreRoles: number;
  nombreLaboratoiresActifs: number;
  nombreStatutsDemande: number;
};

export type LandingPublic = {
  statistiques: LandingStatistiques;
  roles: RolePublic[];
  laboratoires: LaboratoirePublic[];
  statutsDemande: string[];
};

function unwrap<T>(envelope: ApiEnvelope<T>, fallbackMessage: string): T {
  if (!envelope.success || envelope.data == null) {
    throw new Error(envelope.message ?? fallbackMessage);
  }
  return envelope.data;
}

export async function fetchLandingPublic(): Promise<LandingPublic> {
  const envelope = await apiClient.get<ApiEnvelope<LandingPublic>>("/public/landing");
  const payload = unwrap(envelope, "Impossible de charger les données de la landing");
  return {
    statistiques: payload.statistiques,
    roles: payload.roles ?? [],
    laboratoires: payload.laboratoires ?? [],
    statutsDemande: payload.statutsDemande ?? [],
  };
}
