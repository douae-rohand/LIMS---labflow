import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";
import { messageErreurClient } from "./clientAgrege";

/**
 * profilClient.ts — Profil du client connecté (/api/client/profil).
 *
 * Le PATCH fusionne côté serveur (`ClientService.modifierProfil`) : un champ
 * absent du corps conserve sa valeur stockée, SAUF pour l'adresse où `null`
 * signifie « inchangé » — une chaîne vide `""` efface donc bien l'adresse.
 */

export type TypeClient = "PARTICULIER" | "ENTREPRISE";

export interface ProfilClientDto {
  id: number;
  utilisateurId: number | null;
  nom: string | null;
  prenom: string | null;
  email: string | null;
  telephone: string | null;
  typeClient: TypeClient;
  raisonSociale: string | null;
  ice: string | null;
  adresse: string | null;
  consentementCndp: boolean | null;
  dateConsentementCndp: string | null;
  versionConsentementCndp: string | null;
  dateCreation: string | null;
}

/**
 * Corps du PATCH : seuls les champs réellement modifiés sont renseignés.
 * Les champs absents ne sont jamais envoyés (le serveur conserve la valeur).
 */
export interface ModifierProfilClientBody {
  typeClient?: TypeClient;
  raisonSociale?: string;
  ice?: string;
  adresse?: string;
}

/** Profil : 404 = profil introuvable ; 422 = entreprise incomplète. */
export function messageErreurProfil(error: unknown): string {
  if (error instanceof ApiError && error.code === "PROFIL_ENTREPRISE_INCOMPLET") {
    return "Une entreprise doit renseigner une raison sociale et un ICE valide (15 chiffres).";
  }
  return messageErreurClient(error, "Profil introuvable.");
}

export async function chargerProfilClient(): Promise<ProfilClientDto> {
  const res = await apiClient.get<ApiResponse<ProfilClientDto>>("/client/profil");
  return res.data;
}

export async function modifierProfilClient(
  body: ModifierProfilClientBody,
): Promise<ProfilClientDto> {
  const res = await apiClient.patch<ApiResponse<ProfilClientDto>>("/client/profil", body);
  return res.data;
}
