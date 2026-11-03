import apiClient from "./axios";
import type { ApiResponse } from "./auth";

export interface PointSerie {
  cle: string;
  nombre: number;
}

export interface ActiviteLaboratoire {
  laboratoireId: number;
  nom: string;
  statut?: string | null;
  couverture?: "INCLUS" | "EXCLU" | "INACCESSIBLE" | null;
  nombre?: number | null;
  parStatut?: PointSerie[] | null;
  utilisateurs?: number | null;
  tauxCloture?: number | null;
  tailleOctets?: number | null;
  versionSchema?: string | null;
  etatSchema?: string | null;
  demandesParMois?: PointSerie[] | null;
  indisponible: boolean;
}

export interface HistoriqueStatut {
  id: number;
  ancienStatut?: string | null;
  nouveauStatut: string;
  motif?: string | null;
  acteurEmail: string;
  dateChangement: string;
}

export interface AlerteIntegration {
  numero: string;
  nom?: string | null;
  jours: number;
}

export interface EtatService {
  nom: string;
  etat: "OK" | "INDISPONIBLE" | "NON_CONFIGURE" | "DESACTIVE";
}

export interface TableauBord {
  utilisateursActifs: number;
  totalDemandesAnalyse: number;
  laboratoiresIndisponibles: number;
  laboratoiresExclus: number;
  inscriptions: PointSerie[];
  activites: ActiviteLaboratoire[];
  domaines: PointSerie[];
  alertesIntegration: AlerteIntegration[];
  alertesN8n: string;
  services: EtatService[];
}

export async function obtenirTableauBord(): Promise<TableauBord> {
  const res = await apiClient.get<ApiResponse<TableauBord>>("/plateforme/tableau-de-bord");
  return res.data;
}

export async function historiqueLaboratoire(id: number): Promise<HistoriqueStatut[]> {
  const res = await apiClient.get<ApiResponse<HistoriqueStatut[]>>(`/plateforme/laboratoires/${id}/historique`);
  return res.data;
}
