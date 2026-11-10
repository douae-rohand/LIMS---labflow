import apiClient from "./axios";
import type { ApiResponse } from "./auth";

export type StatutIntegration = "EN_ATTENTE" | "COMPLEMENTS_DEMANDES" | "ACCEPTEE" | "REFUSEE";

export const LIBELLES_STATUT_INTEGRATION: Record<StatutIntegration, string> = {
  EN_ATTENTE: "En attente",
  COMPLEMENTS_DEMANDES: "Compléments demandés",
  ACCEPTEE: "Acceptée",
  REFUSEE: "Refusée",
};

/** Document exigé pour l'intégration ; la liste est paramétrée par le Super Administrateur. */
export interface TypeDocumentIntegrationDto {
  code: string;
  libelle: string;
  raison?: string | null;
  obligatoire: boolean;
  actif: boolean;
  ordre: number;
}

export interface DocumentIntegrationDto {
  id: number;
  typeDocument: string;
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
  nombreDocuments?: number;
  dossierComplet?: boolean;
  complementsDemandes?: string | null;
  documentsComplements?: string[];
  dateDemandeComplements?: string | null;
  dateReceptionComplements?: string | null;
  documentsAttendus?: TypeDocumentIntegrationDto[];
  historique?: HistoriqueDecision[];
  documents?: DocumentIntegrationDto[];
}

export interface HistoriqueDecision {
  evenement: string;
  date: string;
  detail?: string | null;
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
  fichiers: Record<string, File>,
  onProgress?: (pct: number) => void,
): Promise<DemandeIntegration> {
  const formData = new FormData();
  formData.append(
    "demande",
    new Blob([JSON.stringify(demande)], { type: "application/json" }),
  );
  Object.entries(fichiers).forEach(([type, fichier]) => {
    formData.append(type, fichier);
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
  recherche?: string,
): Promise<SpringPage<DemandeIntegration>> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (statut) params.set("statut", statut);
  if (recherche) params.set("recherche", recherche);
  const res = await apiClient.get<ApiResponse<SpringPage<DemandeIntegration>>>(
    `/plateforme/demandes?${params.toString()}`,
  );
  return res.data;
}

export async function trouverDemandeIntegration(id: number): Promise<DemandeIntegration> {
  const res = await apiClient.get<ApiResponse<DemandeIntegration>>(`/plateforme/demandes/${id}`);
  return res.data;
}

export async function trouverDemandeDuLaboratoire(
  laboratoireId: number,
): Promise<DemandeIntegration | null> {
  const res = await apiClient.get<ApiResponse<DemandeIntegration | null>>(
    `/plateforme/laboratoires/${laboratoireId}/demande`,
  );
  return res.data ?? null;
}

export async function contenuDocumentIntegration(
  id: number,
  type: string,
): Promise<Blob> {
  return apiClient.getBlob(`/plateforme/demandes/${id}/documents/${type}`);
}

export async function traiterDemandeIntegration(
  id: number,
  decision: "ACCEPTEE" | "REFUSEE",
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
  type: string,
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

export async function ajouterDocumentIntegration(
  id: number,
  type: string,
  fichier: File,
): Promise<DemandeIntegration> {
  const formData = new FormData();
  formData.append("fichier", fichier);
  const res = await apiClient.upload<ApiResponse<DemandeIntegration>>(
    `/plateforme/demandes/${id}/documents?type=${encodeURIComponent(type)}`,
    formData,
  );
  return res.data;
}

export async function demanderComplements(
  id: number,
  message: string,
  documents: string[],
): Promise<DemandeIntegration> {
  const res = await apiClient.post<ApiResponse<DemandeIntegration>>(
    `/plateforme/demandes/${id}/complements`,
    { message, documents },
  );
  return res.data;
}

export type FormatExport = "csv" | "xlsx" | "pdf";

export const EXTENSIONS_EXPORT: Record<FormatExport, string> = { csv: "csv", xlsx: "xlsx", pdf: "pdf" };

export async function exporterDemandesIntegration(
  format: FormatExport,
  statut?: StatutIntegration,
  recherche?: string,
): Promise<void> {
  const params = new URLSearchParams({ format });
  if (statut) params.set("statut", statut);
  if (recherche) params.set("recherche", recherche);
  await apiClient.download(
    `/plateforme/demandes/export?${params.toString()}`,
    `demandes-integration.${EXTENSIONS_EXPORT[format]}`,
  );
}

// ---------------------------------------------------------------------------
// Compléments : accès public du demandeur par le lien reçu par e-mail
// ---------------------------------------------------------------------------

export interface DocumentComplement {
  code: string;
  libelle: string;
  raison?: string | null;
  dejaFourni: boolean;
}

export interface ComplementsPublic {
  numero: string;
  nomLaboratoire: string;
  message: string;
  dateDemande: string;
  expiration: string;
  documents: DocumentComplement[];
}

export async function consulterComplements(jeton: string): Promise<ComplementsPublic> {
  const res = await apiClient.post<ApiResponse<ComplementsPublic>>(
    "/public/integration/complements/consulter",
    { jeton },
  );
  return res.data;
}

export async function deposerComplements(
  jeton: string,
  reponse: string,
  fichiers: Record<string, File>,
  onProgress?: (pct: number) => void,
): Promise<ComplementsPublic> {
  const formData = new FormData();
  formData.append("jeton", jeton);
  if (reponse.trim()) formData.append("reponse", reponse.trim());
  Object.entries(fichiers).forEach(([code, fichier]) => formData.append(code, fichier));
  const res = await apiClient.upload<ApiResponse<ComplementsPublic>>(
    "/public/integration/complements",
    formData,
    onProgress,
    180_000,
  );
  return res.data;
}
