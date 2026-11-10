import apiClient from "./axios";
import type { ApiResponse } from "./auth";

export type EtatReception = "A_RECEVOIR" | "PARTIELLE" | "COMPLETE";

/** Demande acceptée ou en cours, vue sous l'angle de la réception des échantillons. */
export interface DemandeReception {
  demandeId: number;
  reference: string;
  titre?: string | null;
  client?: string | null;
  patient?: string | null;
  statut: string;
  dateDecision?: string | null;
  derniereReception?: string | null;
  analyses: number;
  enAttente: number;
  recues: number;
  nonConformes: number;
  etat: EtatReception;
}

export interface ExigenceEchantillon {
  nature: string;
  etatConservation?: string | null;
  quantiteMinimale?: number | null;
  unite?: string | null;
}

export interface LigneReception {
  id: number;
  code: string;
  essaiCode?: string | null;
  essai?: string | null;
  domaine?: string | null;
  technicien?: string | null;
  statut?: string | null;
  aRecevoir: boolean;
  echantillonReference?: string | null;
  echantillonConforme?: boolean | null;
  exigences: ExigenceEchantillon[];
}

export interface NatureAttendue {
  nature?: string | null;
  etatConservation?: string | null;
  quantite?: string | null;
  ligneIds: number[];
}

export interface EchantillonRecu {
  id: number;
  reference: string;
  nature: string;
  dateReception: string;
  conforme: boolean;
  motif?: string | null;
  etatConservationConstate?: string | null;
  receptionneur?: string | null;
  demandeId?: number | null;
  demandeReference?: string | null;
  analyses: string[];
}

export interface DetailReception {
  demande: DemandeReception;
  lignes: LigneReception[];
  naturesAttendues: NatureAttendue[];
  echantillons: EchantillonRecu[];
}

export interface EchantillonARecevoir {
  nature: string;
  conforme: boolean;
  motif?: string | undefined;
  etatConservationConstate?: string | undefined;
  ligneIds: number[];
}

export interface TableauAccueil {
  demandesARecevoir: number;
  receptionsPartielles: number;
  echantillonsRecusAujourdhui: number;
  nonConformesAujourdhui: number;
  facturesAEncaisser: number;
  resteAEncaisser: number;
  prochainesReceptions: DemandeReception[];
  dernieresReceptions: EchantillonRecu[];
}

export async function lireTableauAccueil(): Promise<TableauAccueil> {
  const res = await apiClient.get<ApiResponse<TableauAccueil>>("/accueil/tableau");
  return res.data;
}

export async function listerReceptions(): Promise<DemandeReception[]> {
  const res = await apiClient.get<ApiResponse<DemandeReception[]>>("/accueil/receptions");
  return res.data;
}

export async function lireReception(demandeId: number): Promise<DetailReception> {
  const res = await apiClient.get<ApiResponse<DetailReception>>(`/accueil/receptions/${demandeId}`);
  return res.data;
}

export async function receptionner(demandeId: number, echantillons: EchantillonARecevoir[]): Promise<DetailReception> {
  const res = await apiClient.post<ApiResponse<DetailReception>>(`/accueil/receptions/${demandeId}`, { echantillons });
  return res.data;
}
