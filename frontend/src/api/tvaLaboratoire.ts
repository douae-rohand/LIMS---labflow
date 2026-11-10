import { useQuery } from "@tanstack/react-query";
import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import { getSession } from "./session";

/** TVA du laboratoire, visible uniquement par l'Administrateur du laboratoire. */
export interface TvaLaboratoire {
  /** Taux propre au laboratoire, ou null s'il suit le taux de la plateforme. */
  tauxLaboratoire: number | null;
  tauxPlateforme: number;
  tauxEffectif: number;
}

export async function lireTvaLaboratoire(): Promise<TvaLaboratoire> {
  const res = await apiClient.get<ApiResponse<TvaLaboratoire>>("/laboratoire/tva");
  return res.data;
}

export async function enregistrerTvaLaboratoire(tauxTva: number | null): Promise<TvaLaboratoire> {
  const res = await apiClient.put<ApiResponse<TvaLaboratoire>>("/laboratoire/tva", { tauxTva });
  return res.data;
}

/** Taux effectif du laboratoire ; désactivé hors de l'espace Administrateur. */
export function useTvaLaboratoire() {
  return useQuery({
    queryKey: ["tva-laboratoire"],
    queryFn: lireTvaLaboratoire,
    enabled: getSession()?.user.role === "ADMINISTRATEUR",
    staleTime: 60_000,
  });
}

/** Part de TVA comprise dans un prix TTC : TTC × taux / (100 + taux). */
export function tvaComprise(montantTtc: number, taux: number): number {
  if (!Number.isFinite(montantTtc) || !Number.isFinite(taux) || taux <= 0) return 0;
  return Math.round(((montantTtc * taux) / (100 + taux)) * 100) / 100;
}

const FORMAT = new Intl.NumberFormat("fr-MA", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function formaterDh(montant: number): string {
  return `${FORMAT.format(montant)} DH`;
}

/** Libellé « dont −XX DH de TVA (YY %) » pour un prix TTC. */
export function libelleTva(montantTtc: number, taux: number): string {
  return `dont ${formaterDh(tvaComprise(montantTtc, taux))} de TVA (${taux} %)`;
}
