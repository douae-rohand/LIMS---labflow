import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import type { TypeDocumentIntegration } from "@/components/integration/constants";

export type StatutIntegration =
  | "EN_ATTENTE"
  | "EN_COURS_VALIDATION"
  | "APPROUVEE"
  | "REJETEE"
  | "SUSPENDUE";

export interface TypeDocumentIntegrationDto {
  code: TypeDocumentIntegration;
  libelle: string;
  raison: string;
  obligatoire: boolean;
}

export interface DocumentIntegrationDto {
  id: number;
  typeDocument: TypeDocumentIntegration;
  libelle: string;
  raison: string;
  nomFichier: string;
  typeMime: string;
  taille: number;
  dateAjout: string;
}

export interface SoumettreDemandeIntegrationRequest {
  nomLaboratoire: string;
  raisonSociale: string;
  typeLaboratoire?: string | undefined;
  typesLaboratoire: string[];
  ice?: string | undefined;
  telephoneLaboratoire?: string | undefined;
  emailLaboratoire?: string | undefined;
  siteWeb?: string | undefined;
  informationsComplementaires?: string | undefined;
  adresse: string;
  ville: string;
  region?: string | undefined;
  pays?: string | undefined;
  codePostal?: string | undefined;
  latitude: number;
  longitude: number;
  adminNom: string;
  adminPrenom: string;
  adminEmail: string;
  adminTelephone: string;
  adminFonction: string;
  adminCin: string;
}

export interface DemandeIntegration {
  id: number;
  numero: string;
  statut: StatutIntegration;
  dateSoumission: string;
  dateTraitement?: string | null;
  motifRefus?: string | null;
  nomLaboratoire: string;
  raisonSociale: string;
  typeLaboratoire?: string;
  typesLaboratoire?: string[];
  ice?: string;
  telephoneLaboratoire?: string;
  emailLaboratoire?: string;
  siteWeb?: string;
  informationsComplementaires?: string;
  adresse?: string;
  ville?: string;
  region?: string;
  pays?: string;
  codePostal?: string;
  latitude?: number | null;
  longitude?: number | null;
  adminNom: string;
  adminPrenom?: string;
  adminEmail: string;
  adminTelephone?: string;
  adminFonction?: string;
  adminCin?: string;
  laboratoireId?: number | null;
  laboratoireCode?: string | null;
  nomSchema?: string | null;
  documents?: DocumentIntegrationDto[];
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export async function listerDocumentsRequis(): Promise<TypeDocumentIntegrationDto[]> {
  const res = await apiClient.get<ApiResponse<TypeDocumentIntegrationDto[]>>(
    "/public/integration/documents-requis",
  );
  return res.data;
}

export async function soumettreDemandeIntegration(
  demande: SoumettreDemandeIntegrationRequest,
  fichiers: Record<TypeDocumentIntegration, File>,
  onProgress?: (pct: number) => void,
): Promise<DemandeIntegration> {
  const formData = new FormData();
  formData.append(
    "demande",
    new Blob([JSON.stringify(demande)], { type: "application/json" }),
  );
  (Object.keys(fichiers) as TypeDocumentIntegration[]).forEach((type) => {
    formData.append(type, fichiers[type]);
  });
  const res = await apiClient.upload<ApiResponse<DemandeIntegration>>(
    "/plateforme/demandes",
    formData,
    onProgress,
    180_000,
  );
  return res.data;
}

export async function listerDemandesIntegration(
  statut?: StatutIntegration,
  page = 0,
  size = 20,
): Promise<SpringPage<DemandeIntegration>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (statut) params.set("statut", statut);
  const res = await apiClient.get<ApiResponse<SpringPage<DemandeIntegration>>>(
    `/plateforme/demandes?${params.toString()}`,
  );
  return res.data;
}

export async function trouverDemandeIntegration(id: number): Promise<DemandeIntegration> {
  const res = await apiClient.get<ApiResponse<DemandeIntegration>>(`/plateforme/demandes/${id}`);
  return res.data;
}

export async function traiterDemandeIntegration(
  id: number,
  decision: "APPROUVEE" | "REJETEE",
  motifRefus?: string,
): Promise<DemandeIntegration> {
  const res = await apiClient.post<ApiResponse<DemandeIntegration>>(
    `/plateforme/demandes/${id}/traiter`,
    { decision, motifRefus },
  );
  return res.data;
}

export async function renvoyerInvitation(id: number): Promise<void> {
  await apiClient.post<ApiResponse<null>>(`/plateforme/demandes/${id}/invitation`);
}

export async function ouvrirDocumentIntegration(
  id: number,
  type: TypeDocumentIntegration,
  download = false,
): Promise<void> {
  const blob = await apiClient.getBlob(
    `/plateforme/demandes/${id}/documents/${type}${download ? "?download=true" : ""}`,
  );
  const url = URL.createObjectURL(blob);
  if (download) {
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = type.toLowerCase();
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
  } else {
    window.open(url, "_blank", "noopener,noreferrer");
  }
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
