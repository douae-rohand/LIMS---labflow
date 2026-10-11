import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import type { SpringPage } from "./plateforme";

export interface AdministrateurLaboratoire {
  id: number;
  nom: string;
  prenom?: string;
  nomComplet: string;
  email: string;
  telephone?: string | null;
  fonction?: string | null;
  cin?: string | null;
  actif: boolean;
}

export interface LaboratoireAdmin {
  id: number;
  code: string;
  nom: string;
  schemaName?: string;
  adresse?: string | null;
  ville?: string | null;
  region?: string | null;
  pays?: string | null;
  codePostal?: string | null;
  ice?: string | null;
  typeLaboratoire?: string | null;
  typesLaboratoire?: string[];
  telephone?: string | null;
  emailContact?: string | null;
  siteWeb?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  statut: string;
  actif: boolean;
  dateCreation?: string | null;
  administrateur?: AdministrateurLaboratoire | null;
}

export interface StatistiquesPlateforme {
  totalLaboratoires: number;
  laboratoiresActifs: number;
  laboratoiresDesactives: number;
  demandesEnAttente: number;
  totalDemandes: number;
}

export async function obtenirStatistiquesPlateforme(): Promise<StatistiquesPlateforme> {
  const res = await apiClient.get<ApiResponse<StatistiquesPlateforme>>(
    "/plateforme/laboratoires/statistiques",
  );
  return res.data;
}

export interface FiltreLaboratoires {
  recherche?: string | undefined;
  statut?: "" | "ACTIF" | "INACTIF" | undefined;
  ville?: string | undefined;
  page?: number;
  size?: number;
  tri?: string;
  ordre?: "asc" | "desc";
}

export interface ModifierLaboratoirePayload {
  nom: string;
  ice?: string;
  typeLaboratoire?: string;
  adresse?: string;
  ville?: string;
  region?: string;
  pays?: string;
  codePostal?: string;
  telephone?: string;
  email?: string;
  siteWeb?: string;
  latitude?: number | null;
  longitude?: number | null;
}

export interface ModifierAdministrateurPayload {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone?: string;
  fonction?: string;
  cin?: string;
}

export interface LigneImportLaboratoire {
  numeroLigne?: number;
  nomLaboratoire?: string | null;
  raisonSociale?: string | null;
  typesLaboratoire?: string | null;
  ice?: string | null;
  ville?: string | null;
  adresse?: string | null;
  region?: string | null;
  pays?: string | null;
  codePostal?: string | null;
  telephoneLaboratoire?: string | null;
  emailLaboratoire?: string | null;
  siteWeb?: string | null;
  adminNom?: string | null;
  adminPrenom?: string | null;
  adminEmail?: string | null;
  adminTelephone?: string | null;
  adminFonction?: string | null;
  adminCin?: string | null;
  latitude?: string | null;
  longitude?: string | null;
}

export interface ResultatLigneImport {
  numeroLigne: number;
  nomLaboratoire?: string | null;
  adminEmail?: string | null;
  valide: boolean;
  erreurs: string[];
  demandeId?: number | null;
  numeroDemande?: string | null;
  donnees: LigneImportLaboratoire;
}

export interface RapportImport {
  total: number;
  valides: number;
  invalides: number;
  enregistrees: number;
  apercu: boolean;
  message: string;
  lignes: ResultatLigneImport[];
}

export async function rechercherLaboratoires(
  filtre: FiltreLaboratoires,
): Promise<SpringPage<LaboratoireAdmin>> {
  const params = new URLSearchParams({
    page: String(filtre.page ?? 0),
    size: String(filtre.size ?? 10),
    tri: filtre.tri ?? "dateCreation",
    ordre: filtre.ordre ?? "desc",
  });
  if (filtre.recherche) params.set("recherche", filtre.recherche);
  if (filtre.statut) params.set("statut", filtre.statut);
  if (filtre.ville) params.set("ville", filtre.ville);
  const res = await apiClient.get<ApiResponse<SpringPage<LaboratoireAdmin>>>(
    `/plateforme/laboratoires?${params.toString()}`,
  );
  return res.data;
}

export async function listerVillesLaboratoires(): Promise<string[]> {
  const res = await apiClient.get<ApiResponse<string[]>>("/plateforme/laboratoires/villes");
  return res.data;
}

export async function modifierLaboratoire(
  id: number,
  payload: ModifierLaboratoirePayload,
): Promise<LaboratoireAdmin> {
  const res = await apiClient.patch<ApiResponse<LaboratoireAdmin>>(
    `/plateforme/laboratoires/${id}`,
    payload,
  );
  return res.data;
}

export async function modifierAdministrateurLaboratoire(
  id: number,
  payload: ModifierAdministrateurPayload,
): Promise<LaboratoireAdmin> {
  const res = await apiClient.patch<ApiResponse<LaboratoireAdmin>>(
    `/plateforme/laboratoires/${id}/administrateur`,
    payload,
  );
  return res.data;
}

export async function changerStatutLaboratoire(
  id: number,
  actif: boolean,
): Promise<LaboratoireAdmin> {
  const res = await apiClient.patch<ApiResponse<LaboratoireAdmin>>(
    `/plateforme/laboratoires/${id}/${actif ? "activer" : "desactiver"}`,
  );
  return res.data;
}

export async function exporterLaboratoires(
  format: "pdf" | "xlsx" | "csv",
  portee: "filtres" | "tous",
  filtre: FiltreLaboratoires,
): Promise<void> {
  const params = new URLSearchParams({ format, portee });
  if (portee === "filtres") {
    if (filtre.recherche) params.set("recherche", filtre.recherche);
    if (filtre.statut) params.set("statut", filtre.statut);
    if (filtre.ville) params.set("ville", filtre.ville);
  }
  const extension = format === "xlsx" ? "xlsx" : format;
  await apiClient.download(
    `/plateforme/laboratoires/export?${params.toString()}`,
    `laboratoires.${extension}`,
  );
}

export async function telechargerModeleImport(format: "xlsx" | "csv"): Promise<void> {
  await apiClient.download(
    `/plateforme/laboratoires/import/modele?format=${format}`,
    format === "csv"
      ? "modele-importation-laboratoires.csv"
      : "modele-importation-laboratoires.xlsx",
  );
}

export async function analyserImport(fichier: File): Promise<RapportImport> {
  const formData = new FormData();
  formData.append("fichier", fichier);
  const res = await apiClient.upload<ApiResponse<RapportImport>>(
    "/plateforme/laboratoires/import/analyser",
    formData,
  );
  return res.data;
}

export async function verifierImport(lignes: LigneImportLaboratoire[]): Promise<RapportImport> {
  const res = await apiClient.post<ApiResponse<RapportImport>>(
    "/plateforme/laboratoires/import/verifier",
    { lignes },
  );
  return res.data;
}

export async function confirmerImport(lignes: LigneImportLaboratoire[]): Promise<RapportImport> {
  const res = await apiClient.post<ApiResponse<RapportImport>>(
    "/plateforme/laboratoires/import/confirmer",
    { lignes },
  );
  return res.data;
}
