import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import type { SpringPage } from "./plateforme";

export interface CompteUtilisateur {
  id: number;
  nom: string;
  prenom?: string | null;
  email: string;
  telephone?: string | null;
  fonction?: string | null;
  role: string;
  laboratoireId?: number | null;
  laboratoireNom?: string | null;
  statut: "ACTIF" | "INACTIF";
  derniereConnexion?: string | null;
  personnalise: boolean;
}

export interface FiltreUtilisateurs {
  recherche?: string;
  role?: string;
  laboratoireId?: number;
  statut?: "" | "ACTIF" | "INACTIF";
  page?: number;
  size?: number;
  tri?: string;
  ordre?: "asc" | "desc";
}

export interface LigneImportUtilisateur {
  numeroLigne: number;
  email?: string | null;
  valide: boolean;
  erreurs: string[];
}

export interface RapportImportUtilisateur {
  total: number;
  valides: number;
  invalides: number;
  enregistres: number;
  invitationsNonEnvoyees: number;
  jetonAnalyse?: string | null;
  apercu: boolean;
  message?: string | null;
  lignes: LigneImportUtilisateur[];
  invitationsEchouees: LigneImportUtilisateur[];
}

export interface ResultatEnvoi {
  emailAccepte: boolean;
  message?: string | null;
}

function params(filtre: FiltreUtilisateurs): URLSearchParams {
  const recherche = new URLSearchParams({
    page: String(filtre.page ?? 0),
    size: String(filtre.size ?? 20),
    tri: filtre.tri ?? "nom",
    ordre: filtre.ordre ?? "asc",
  });
  if (filtre.recherche) recherche.set("recherche", filtre.recherche);
  if (filtre.role) recherche.set("role", filtre.role);
  if (filtre.laboratoireId) recherche.set("laboratoireId", String(filtre.laboratoireId));
  if (filtre.statut) recherche.set("statut", filtre.statut);
  return recherche;
}

export async function listerComptes(filtre: FiltreUtilisateurs): Promise<SpringPage<CompteUtilisateur>> {
  const res = await apiClient.get<ApiResponse<SpringPage<CompteUtilisateur>>>(
    `/plateforme/utilisateurs?${params(filtre).toString()}`,
  );
  return res.data;
}

export async function exporterComptes(format: "csv" | "xlsx", filtre: FiltreUtilisateurs): Promise<void> {
  const recherche = params({ ...filtre, page: 0, size: 20 });
  recherche.set("format", format);
  await apiClient.download(
    `/plateforme/utilisateurs/export?${recherche.toString()}`,
    format === "csv" ? "utilisateurs.csv" : "utilisateurs.xlsx",
  );
}

export async function creerAdministrateur(payload: {
  laboratoireId: number;
  nom: string;
  prenom?: string;
  email: string;
  telephone?: string;
  fonction?: string;
}): Promise<CompteUtilisateur> {
  const res = await apiClient.post<ApiResponse<CompteUtilisateur>>("/plateforme/utilisateurs", payload);
  return res.data;
}

export async function modifierCompte(
  id: number,
  payload: { nom: string; prenom?: string; telephone?: string; fonction?: string },
): Promise<CompteUtilisateur> {
  const res = await apiClient.patch<ApiResponse<CompteUtilisateur>>(`/plateforme/utilisateurs/${id}`, payload);
  return res.data;
}

export async function desactiverCompte(id: number, motif?: string): Promise<CompteUtilisateur> {
  const res = await apiClient.post<ApiResponse<CompteUtilisateur>>(`/plateforme/utilisateurs/${id}/desactiver`, { motif });
  return res.data;
}

export async function reactiverCompte(id: number, motif?: string): Promise<CompteUtilisateur> {
  const res = await apiClient.post<ApiResponse<CompteUtilisateur>>(`/plateforme/utilisateurs/${id}/reactiver`, { motif });
  return res.data;
}

export async function reinitialiserMotDePasseCompte(id: number): Promise<ResultatEnvoi> {
  const res = await apiClient.post<ApiResponse<ResultatEnvoi>>(`/plateforme/utilisateurs/${id}/reinitialiser-mot-de-passe`);
  return res.data;
}

export async function renvoyerInvitationCompte(id: number): Promise<ResultatEnvoi> {
  const res = await apiClient.post<ApiResponse<ResultatEnvoi>>(`/plateforme/utilisateurs/${id}/renvoyer-invitation`);
  return res.data;
}

export async function telechargerModeleUtilisateurs(format: "csv" | "xlsx"): Promise<void> {
  await apiClient.download(
    `/plateforme/utilisateurs/import/modele?format=${format}`,
    format === "csv" ? "modele-importation-utilisateurs.csv" : "modele-importation-utilisateurs.xlsx",
  );
}

export async function analyserImportUtilisateurs(fichier: File): Promise<RapportImportUtilisateur> {
  const form = new FormData();
  form.append("fichier", fichier);
  const res = await apiClient.upload<ApiResponse<RapportImportUtilisateur>>("/plateforme/utilisateurs/import/analyser", form);
  return res.data;
}

export interface LignePermission {
  code: string;
  module: string;
  action: string;
  libelle: string;
  parDefaut: boolean;
  ecart: "AJOUT" | "RETRAIT" | null;
  effective: boolean;
}

export interface PermissionsCompte {
  personnalisable: boolean;
  personnalisees: boolean;
  permissions: LignePermission[];
  actionsNonDelegables: string[];
}

export async function lirePermissions(id: number): Promise<PermissionsCompte> {
  const res = await apiClient.get<ApiResponse<PermissionsCompte>>(`/plateforme/utilisateurs/${id}/permissions`);
  return res.data;
}

export async function remplacerPermissions(
  id: number,
  ecarts: { code: string; effet: "AJOUT" | "RETRAIT" }[],
): Promise<PermissionsCompte> {
  const res = await apiClient.put<ApiResponse<PermissionsCompte>>(`/plateforme/utilisateurs/${id}/permissions`, { ecarts });
  return res.data;
}

export async function retablirPermissions(id: number): Promise<PermissionsCompte> {
  const res = await apiClient.post<ApiResponse<PermissionsCompte>>(`/plateforme/utilisateurs/${id}/permissions/retablir`);
  return res.data;
}

export async function confirmerImportUtilisateurs(jetonAnalyse: string): Promise<RapportImportUtilisateur> {
  const res = await apiClient.post<ApiResponse<RapportImportUtilisateur>>("/plateforme/utilisateurs/import/confirmer", { jetonAnalyse });
  return res.data;
}
