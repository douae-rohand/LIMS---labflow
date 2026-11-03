import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import { messageErreurClient, type ListeAgregee } from "./clientAgrege";

/**
 * facturesClient.ts — Mes factures (endpoints /api/client/factures).
 *
 * Le statut est recalculé côté serveur : seules les valeurs PAYEE et IMPAYEE
 * sont exposées. Les dates (`dateEmission`, `dateEcheance`) sont des LocalDate
 * « YYYY-MM-DD » : ne jamais les passer à `new Date(...)`.
 */

export type StatutFactureClient = "PAYEE" | "IMPAYEE";

export interface FactureResumeClient {
  factureId: number;
  laboratoireId: number;
  laboratoireNom: string | null;
  numero: string;
  demandeReference: string | null;
  demandeTitre: string | null;
  dateEmission: string | null;
  dateEcheance: string | null;
  /** Montant HT (aucune TVA en base). */
  montantTotal: number | string | null;
  montantPaye: number | string | null;
  resteAPayer: number | string | null;
  statut: StatutFactureClient;
}

export interface FactureLigneClient {
  ligneId: number | null;
  ligneEssaiId: number | null;
  designation: string | null;
  montant: number | string | null;
}

export interface PaiementClient {
  /** Instant ISO. */
  date: string | null;
  montant: number | string | null;
  mode: string | null;
}

export interface FactureDetailClient extends FactureResumeClient {
  type: string | null;
  demandeId: number | null;
  montantHt: number | string | null;
  /** ICE du client au sein du laboratoire, si renseigné. */
  ice: string | null;
  lignes: FactureLigneClient[];
  paiements: PaiementClient[];
}

/** Liste « Mes factures » : aucune 404 attendue. */
export function messageErreurFactures(error: unknown): string {
  return messageErreurClient(error, "Impossible de charger vos factures.");
}

/** Détail d'une facture : 404 = facture introuvable. */
export function messageErreurFacture(error: unknown): string {
  return messageErreurClient(error, "Facture introuvable.");
}

export async function listerFacturesClient(): Promise<ListeAgregee<FactureResumeClient>> {
  const res = await apiClient.get<ApiResponse<ListeAgregee<FactureResumeClient>>>(
    "/client/factures",
  );
  return res.data;
}

export async function detailFactureClient(
  laboratoireId: number,
  factureId: number,
): Promise<FactureDetailClient> {
  const res = await apiClient.get<ApiResponse<FactureDetailClient>>(
    `/client/factures/${laboratoireId}/${factureId}`,
  );
  return res.data;
}
