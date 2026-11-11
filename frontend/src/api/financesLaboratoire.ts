import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import { EXTENSIONS_EXPORT, type FormatExport } from "./plateforme";

export type Regroupement = "JOUR" | "SEMAINE" | "MOIS";

/** Une ligne du détail : un jour, une semaine (commençant le lundi) ou un mois. */
export interface PeriodeFinance {
  debut: string;
  libelle: string;
  demandesFacturees: number;
  /** Montant des factures (base de calcul). */
  facture: number;
  /** Commission de la plateforme sur les factures. */
  commission: number;
  /** Commission sur les paiements reçus. */
  commissionEncaissee: number;
}

/** Commission de la plateforme sur les factures de demandes d'un laboratoire, sur une période. */
export interface FinancesLaboratoire {
  laboratoireId: number;
  laboratoire: string;
  du: string;
  au: string;
  regroupement: Regroupement;
  /** Taux de commission actuel de la plateforme (%). */
  tauxCommission: number;
  demandesFacturees: number;
  facture: number;
  commission: number;
  commissionEncaissee: number;
  /** État à ce jour, indépendant de la période. */
  commissionAPercevoir: number;
  commissionEnRetard: number;
  details: PeriodeFinance[];
}

export interface FiltreFinances {
  du: string;
  au: string;
  /** Vide : choisi selon la durée. */
  regroupement: Regroupement | "";
}

function parametres(filtre: FiltreFinances): URLSearchParams {
  const params = new URLSearchParams({ du: filtre.du, au: filtre.au });
  if (filtre.regroupement) params.set("regroupement", filtre.regroupement);
  return params;
}

export async function lireFinancesLaboratoire(id: number, filtre: FiltreFinances): Promise<FinancesLaboratoire> {
  const res = await apiClient.get<ApiResponse<FinancesLaboratoire>>(
    `/plateforme/laboratoires/${id}/finances?${parametres(filtre).toString()}`,
  );
  return res.data;
}

export async function exporterFinancesLaboratoire(
  id: number,
  code: string,
  filtre: FiltreFinances,
  format: FormatExport,
): Promise<void> {
  const params = parametres(filtre);
  params.set("format", format);
  await apiClient.download(
    `/plateforme/laboratoires/${id}/finances/export?${params.toString()}`,
    `finances-${code}-${filtre.du}_${filtre.au}.${EXTENSIONS_EXPORT[format]}`,
  );
}
