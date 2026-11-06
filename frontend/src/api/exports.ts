import apiClient, { ApiError } from "./axios";

export type ExportJeu = "demandes" | "factures" | "employes" | "enquetes" | "produits" | "audit";
export type ExportFormat = "csv" | "xlsx" | "pdf";

export const PERMISSION_EXPORT: Record<ExportJeu, string> = {
  demandes: "EXPORT_DEMANDES",
  factures: "EXPORT_FACTURES",
  employes: "EXPORT_EMPLOYES",
  enquetes: "EXPORT_ENQUETES",
  produits: "EXPORT_PRODUITS",
  audit: "EXPORT_AUDIT",
};

export const PERMISSIONS_EXPORT = Object.values(PERMISSION_EXPORT);

export function messageErreurExports(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429) return "Trop de tentatives, réessayez dans un instant.";
    if (error.status === 403) return "Accès refusé.";
    if (error.status === 422) return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    if (error.status === 0 || error.message === "Network Error") return "Serveur indisponible.";
    if (error.status === 400) return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

export async function exporter(jeu: ExportJeu, format: ExportFormat): Promise<Blob> {
  return apiClient.getBlob(`/export/${jeu}?format=${format}`);
}

export function genererNomFichier(jeu: ExportJeu, format: ExportFormat): string {
  const date = new Date().toISOString().split("T")[0];
  const extension = format === "csv" ? "csv" : format === "xlsx" ? "xlsx" : "pdf";
  return `labflow-${jeu}-${date}.${extension}`;
}

export function telecharger(blob: Blob, nomFichier: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = nomFichier;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}
