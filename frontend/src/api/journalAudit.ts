import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import type { SpringPage } from "./plateforme";

export interface EntreeAudit {
  id: number;
  code: string;
  dateAction: string;
  action: string;
  objet: string;
  valeurAvant?: string | null;
  valeurApres?: string | null;
  motif?: string | null;
  acteurEmail?: string | null;
  acteurNom?: string | null;
}

export interface FiltreAudit {
  acteur?: string | undefined;
  action?: string | undefined;
  laboratoireId?: number | undefined;
  du?: string | undefined;
  au?: string | undefined;
  page?: number;
  size?: number;
  tri?: string;
  ordre?: "asc" | "desc";
}

function params(filtre: FiltreAudit): URLSearchParams {
  const recherche = new URLSearchParams();
  if (filtre.acteur) recherche.set("acteur", filtre.acteur);
  if (filtre.action) recherche.set("action", filtre.action);
  if (filtre.laboratoireId) recherche.set("laboratoireId", String(filtre.laboratoireId));
  if (filtre.du) recherche.set("du", filtre.du);
  if (filtre.au) recherche.set("au", filtre.au);
  return recherche;
}

export async function listerJournalAudit(filtre: FiltreAudit): Promise<SpringPage<EntreeAudit>> {
  const recherche = params(filtre);
  recherche.set("page", String(filtre.page ?? 0));
  recherche.set("size", String(filtre.size ?? 10));
  recherche.set("tri", filtre.tri ?? "dateAction");
  recherche.set("ordre", filtre.ordre ?? "desc");
  const res = await apiClient.get<ApiResponse<SpringPage<EntreeAudit>>>(
    `/plateforme/audit?${recherche.toString()}`,
  );
  return res.data;
}

export async function listerActionsAudit(): Promise<string[]> {
  const res = await apiClient.get<ApiResponse<string[]>>("/plateforme/audit/actions");
  return res.data;
}

export async function exporterJournalAudit(filtre: FiltreAudit): Promise<void> {
  await apiClient.download(`/plateforme/audit/export?${params(filtre).toString()}`, "journal-audit.csv");
}
