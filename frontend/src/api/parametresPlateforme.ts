import apiClient from "./axios";
import type { ApiResponse } from "./auth";
import type { RoleUtilisateur } from "./session";
import type { TypeDocumentIntegrationDto } from "./plateforme";

export interface ParametresSecurite {
  roles2faObligatoire: RoleUtilisateur[];
  longueurMinMotDePasse: number;
  exigerCaractereSpecial: boolean;
  dureeSessionJours: number;
}

export interface PolitiqueMotDePasse {
  longueurMin: number;
  exigerCaractereSpecial: boolean;
}

export interface ParametresPublics {
  politiqueMotDePasse: PolitiqueMotDePasse;
  typesLaboratoire: string[];
}

export interface TypeLaboratoireReference {
  id: number;
  libelle: string;
  actif: boolean;
  ordre: number;
}

export interface EnregistrerTypeDocument {
  libelle: string;
  raison?: string;
  obligatoire: boolean;
  actif: boolean;
  ordre: number;
}

const BASE = "/plateforme/parametres";

/** Politique de mot de passe et types de laboratoire : lisibles sans connexion. */
export async function lireParametresPublics(): Promise<ParametresPublics> {
  const res = await apiClient.get<ApiResponse<ParametresPublics>>("/public/parametres");
  return res.data;
}

export async function lireSecurite(): Promise<ParametresSecurite> {
  const res = await apiClient.get<ApiResponse<ParametresSecurite>>(`${BASE}/securite`);
  return res.data;
}

export async function enregistrerSecurite(valeurs: ParametresSecurite): Promise<ParametresSecurite> {
  const res = await apiClient.put<ApiResponse<ParametresSecurite>>(`${BASE}/securite`, valeurs);
  return res.data;
}

export async function listerTypesDocuments(): Promise<TypeDocumentIntegrationDto[]> {
  const res = await apiClient.get<ApiResponse<TypeDocumentIntegrationDto[]>>(`${BASE}/documents`);
  return res.data;
}

export async function creerTypeDocument(valeurs: EnregistrerTypeDocument): Promise<TypeDocumentIntegrationDto> {
  const res = await apiClient.post<ApiResponse<TypeDocumentIntegrationDto>>(`${BASE}/documents`, valeurs);
  return res.data;
}

export async function modifierTypeDocument(
  code: string,
  valeurs: EnregistrerTypeDocument,
): Promise<TypeDocumentIntegrationDto> {
  const res = await apiClient.put<ApiResponse<TypeDocumentIntegrationDto>>(
    `${BASE}/documents/${encodeURIComponent(code)}`,
    valeurs,
  );
  return res.data;
}

export async function listerTypesLaboratoire(): Promise<TypeLaboratoireReference[]> {
  const res = await apiClient.get<ApiResponse<TypeLaboratoireReference[]>>(`${BASE}/types-laboratoire`);
  return res.data;
}

export async function creerTypeLaboratoire(libelle: string, ordre: number): Promise<TypeLaboratoireReference> {
  const res = await apiClient.post<ApiResponse<TypeLaboratoireReference>>(`${BASE}/types-laboratoire`, {
    libelle,
    ordre,
  });
  return res.data;
}

export async function modifierTypeLaboratoire(
  id: number,
  actif: boolean,
  ordre: number,
): Promise<TypeLaboratoireReference> {
  const res = await apiClient.patch<ApiResponse<TypeLaboratoireReference>>(`${BASE}/types-laboratoire/${id}`, {
    actif,
    ordre,
  });
  return res.data;
}
