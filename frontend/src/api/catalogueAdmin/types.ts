import type { PageResponse } from "../utilisateurs";

export interface EssaiAdminDto {
  id: number;
  code: string;
  designation: string;
  description: string | null;
  methode: string | null;
  tarif: number;
  dureeEstimee: number | null;
  unite: string | null;
  limiteMin: number | null;
  limiteMax: number | null;
  seuilCritiqueMin: number | null;
  seuilCritiqueMax: number | null;
  actif: boolean;
  domaineId: number | null;
  domaineLibelle: string | null;
  exigences: ExigenceEchantillonDto[];
}

export interface ExigenceEchantillonDto {
  id: number;
  nature: string;
  etatConservation: string | null;
  quantiteMinimale: number | null;
  unite: string | null;
}

export interface DomaineDto {
  id: number;
  code: string;
  libelle: string;
}

export interface CreerEssaiRequest {
  code: string;
  designation: string;
  description?: string | undefined;
  methode?: string | undefined;
  tarif: number;
  dureeEstimee?: number | undefined;
  unite?: string | undefined;
  limiteMin?: number | undefined;
  limiteMax?: number | undefined;
  seuilCritiqueMin?: number | undefined;
  seuilCritiqueMax?: number | undefined;
  domaineId: number;
  exigences?: CreerExigenceRequest[] | undefined;
}

export interface ModifierEssaiRequest {
  code?: string | undefined;
  designation?: string | undefined;
  description?: string | undefined;
  methode?: string | undefined;
  tarif?: number | undefined;
  dureeEstimee?: number | undefined;
  unite?: string | undefined;
  limiteMin?: number | undefined;
  limiteMax?: number | undefined;
  seuilCritiqueMin?: number | undefined;
  seuilCritiqueMax?: number | undefined;
  domaineId?: number | undefined;
}

export interface CreerDomaineRequest {
  code: string;
  libelle: string;
}

export interface ModifierDomaineRequest {
  libelle?: string | undefined;
}

export interface CreerExigenceRequest {
  nature: string;
  etatConservation: string;
  quantiteMinimale?: number | undefined;
  unite?: string | undefined;
}

export type ModifierExigenceRequest = CreerExigenceRequest;

export interface CatalogueStatsDto {
  totalEssais: number;
  essaisActifs: number;
  totalDomaines: number;
}

export interface ListerEssaisParams {
  domaineId?: number | undefined;
  actif?: boolean | undefined;
  q?: string | undefined;
  page?: number | undefined;
  size?: number | undefined;
}
