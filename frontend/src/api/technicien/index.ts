/**
 * index.ts — Fonctions API de l'espace technicien.
 *
 * Toutes les routes filtrent par technicienId = principal côté backend.
 * Aucune fonction ne prend d'identifiant technicien en paramètre.
 *
 * Référence backend : TechnicienController (/api/technicien/**).
 */

import apiClient, { ApiError } from "../axios";
import type { ApiResponse } from "../auth";
import type { PageResponse } from "../utilisateurs";
import type {
  ImportApercuResponse,
  ImportConfirmerResponse,
  LigneEssaiDetailTechnicienDto,
  LigneEssaiTechnicienDto,
  ListerEssaisParams,
  NonRealisablePayload,
  PlanningJourDto,
  ResultatSaisiePayload,
  SyntheseTechnicienDto,
  TempsPayload,
  ValidationTechniquePayload,
} from "./types";

// ---------------------------------------------------------------------------
// Gestion d'erreur normalisée
// ---------------------------------------------------------------------------

const MESSAGES_ERREUR: Record<string, string> = {
  // Saisie / démarrage
  ESSAI_SANS_ECHANTILLON: "Aucun échantillon n'est encore rattaché à cet essai.",
  ECHANTILLON_NON_CONFORME: "L'échantillon est non conforme, l'essai ne peut pas démarrer.",
  ESSAI_STATUT_INVALIDE: "Le statut de l'essai ne permet pas cette action.",
  ESSAI_NON_DEMARRE: "L'essai doit être démarré avant de saisir un résultat.",
  RESULTAT_DEJA_VALIDE_RESPONSABLE:
    "Ce résultat a déjà été validé par le Responsable, la modification est interdite.",
  MOTIF_MODIFICATION_REQUIS:
    "Un commentaire (motif) est obligatoire pour modifier un résultat déjà saisi.",
  ESSAI_NON_EN_COURS: "L'essai doit être en cours pour être validé techniquement.",
  RESULTAT_ABSENT: "Un résultat doit être saisi avant la validation technique.",
  // Temps
  TEMPS_FIN_AVANT_DEBUT: "La date de fin doit être postérieure à la date de début.",
  TEMPS_FORMAT_INVALIDE:
    "Indiquez soit une durée en minutes, soit une plage début/fin.",
  DUREE_MAX_DEPASSEE: "La durée déclarée dépasse le plafond autorisé (480 minutes).",
  // Planning
  PLANNING_DATES_REQUISES: "Les dates de début et de fin sont obligatoires.",
  PLANNING_FIN_AVANT_DEBUT: "La date de fin doit être postérieure à la date de début.",
  PLANNING_PLAGE_TROP_GRANDE: "La plage de dates ne peut pas dépasser 62 jours.",
  // Tri
  TRI_INVALIDE: "Le champ de tri demandé n'est pas autorisé.",
  DIRECTION_TRI_INVALIDE: "La direction de tri demandée n'est pas autorisée.",
  // Import fichier
  IMPORT_FICHIER_VIDE: "Le fichier CSV est obligatoire et ne peut pas être vide.",
  IMPORT_FICHIER_TROP_GRAND: "Le fichier dépasse la taille maximale autorisée.",
  IMPORT_FORMAT_INVALIDE: "Le fichier doit être au format CSV (.csv).",
  IMPORT_ENCODAGE_INVALIDE: "Le fichier doit être encodé en UTF-8.",
  IMPORT_LECTURE_IMPOSSIBLE: "Impossible de lire le fichier CSV.",
  IMPORT_TROP_DE_LIGNES: "Le fichier contient trop de lignes.",
  IMPORT_CONFLICT: "Conflit de mise à jour lors de l'import, réessayez.",
  // Import lignes
  IMPORT_LIGNE_MAL_FORMEE:
    "La ligne doit contenir au moins 2 colonnes : code_ligne et valeur.",
  IMPORT_FORMULE_INTERDITE:
    "La valeur ne peut pas commencer par = + - @ (protection contre l'injection de formules).",
  IMPORT_CODE_VIDE: "Le code de ligne est obligatoire.",
  IMPORT_VALEUR_VIDE: "La valeur est obligatoire.",
  IMPORT_LIGNE_INTROUVABLE:
    "Aucun essai ne correspond à ce code pour le technicien courant.",
  IMPORT_ESSAI_NON_EN_COURS: "L'essai n'est pas en cours, la saisie est impossible.",
  IMPORT_DEJA_VALIDE_RESPONSABLE:
    "Ce résultat a déjà été validé par le Responsable.",
  IMPORT_MOTIF_REQUIS:
    "Un commentaire (motif) est obligatoire pour modifier un résultat déjà saisi.",
};

/**
 * Traduit une erreur API en message français lisible.
 * Retire le préfixe [CODE] éventuel et utilise le catalogue ci-dessus.
 * Gère spécifiquement le 403 (permission manquante) et le 404 (essai introuvable).
 */
export function messageErreurTechnicien(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) {
      return "Vous n'avez pas la permission d'effectuer cette action.";
    }
    if (error.status === 404) {
      return "Cet essai n'existe pas ou ne vous est pas attribué.";
    }
    if (error.status === 0 || error.message === "Network Error") {
      return "Serveur indisponible. Vérifiez votre connexion.";
    }
    if (error.code !== undefined && MESSAGES_ERREUR[error.code] !== undefined) {
      return MESSAGES_ERREUR[error.code]!;
    }
    if (typeof error.message === "string" && error.message.trim().length > 0) {
      return error.message.replace(/^\[[A-Z0-9_]+\]\s*/, "").trim();
    }
  }
  return "Une erreur inattendue est survenue.";
}

// ---------------------------------------------------------------------------
// Lecture
// ---------------------------------------------------------------------------

/** GET /api/technicien/essais — liste paginée des essais du technicien courant. */
export async function listerMesEssais(
  params?: ListerEssaisParams,
): Promise<PageResponse<LigneEssaiTechnicienDto>> {
  const queryParams: Record<string, string | number | boolean> = {};
  if (params) {
    if (params.page !== undefined && params.page >= 0) queryParams["page"] = params.page;
    if (params.size !== undefined && params.size > 0) queryParams["size"] = Math.min(params.size, 50);
    if (params.tri !== undefined && params.tri.length > 0) queryParams["tri"] = params.tri;
    if (params.direction !== undefined) queryParams["direction"] = params.direction;
    if (params.statut !== undefined) queryParams["statut"] = params.statut;
    if (params.domaineId !== undefined) queryParams["domaineId"] = params.domaineId;
    if (params.demandeId !== undefined) queryParams["demandeId"] = params.demandeId;
    if (params.debut !== undefined) queryParams["debut"] = params.debut;
    if (params.fin !== undefined) queryParams["fin"] = params.fin;
    if (params.enRetard !== undefined) queryParams["enRetard"] = params.enRetard;
    if (params.q !== undefined && params.q.trim().length > 0) queryParams["q"] = params.q.trim();
  }
  const res = await apiClient.get<ApiResponse<PageResponse<LigneEssaiTechnicienDto>>>(
    "/technicien/essais",
    { params: queryParams },
  );
  return res.data;
}

/** GET /api/technicien/essais/{id} — détail d'un essai. */
export async function detailEssai(id: number): Promise<LigneEssaiDetailTechnicienDto> {
  const res = await apiClient.get<ApiResponse<LigneEssaiDetailTechnicienDto>>(
    `/technicien/essais/${id}`,
  );
  return res.data;
}

/** GET /api/technicien/planning — essais regroupés par jour. */
export async function planning(debut: string, fin: string): Promise<PlanningJourDto[]> {
  const res = await apiClient.get<ApiResponse<PlanningJourDto[]>>("/technicien/planning", {
    params: { debut, fin },
  });
  return res.data;
}

/** GET /api/technicien/synthese — tableau de bord agrégé. */
export async function synthese(): Promise<SyntheseTechnicienDto> {
  const res = await apiClient.get<ApiResponse<SyntheseTechnicienDto>>("/technicien/synthese");
  return res.data;
}

// ---------------------------------------------------------------------------
// Écriture
// ---------------------------------------------------------------------------

/** POST /api/technicien/essais/{id}/demarrer — passe EN_COURS. */
export async function demarrerEssai(id: number): Promise<LigneEssaiTechnicienDto> {
  const res = await apiClient.post<ApiResponse<LigneEssaiTechnicienDto>>(
    `/technicien/essais/${id}/demarrer`,
    {},
  );
  return res.data;
}

/** PUT /api/technicien/essais/{id}/resultat — saisit ou modifie le résultat. */
export async function saisirResultat(
  id: number,
  payload: ResultatSaisiePayload,
): Promise<LigneEssaiTechnicienDto> {
  const res = await apiClient.put<ApiResponse<LigneEssaiTechnicienDto>>(
    `/technicien/essais/${id}/resultat`,
    payload,
  );
  return res.data;
}

/** POST /api/technicien/essais/{id}/non-realisable — déclare l'essai non réalisable. */
export async function declareNonRealisable(
  id: number,
  payload: NonRealisablePayload,
): Promise<LigneEssaiTechnicienDto> {
  const res = await apiClient.post<ApiResponse<LigneEssaiTechnicienDto>>(
    `/technicien/essais/${id}/non-realisable`,
    payload,
  );
  return res.data;
}

/** POST /api/technicien/essais/{id}/temps — déclare le temps passé. */
export async function declarerTemps(
  id: number,
  payload: TempsPayload,
): Promise<LigneEssaiTechnicienDto> {
  const res = await apiClient.post<ApiResponse<LigneEssaiTechnicienDto>>(
    `/technicien/essais/${id}/temps`,
    payload,
  );
  return res.data;
}

/** POST /api/technicien/essais/{id}/validation-technique — validation niveau 1. */
export async function validerTechniquement(
  id: number,
  payload: ValidationTechniquePayload,
): Promise<LigneEssaiTechnicienDto> {
  const res = await apiClient.post<ApiResponse<LigneEssaiTechnicienDto>>(
    `/technicien/essais/${id}/validation-technique`,
    payload,
  );
  return res.data;
}

// ---------------------------------------------------------------------------
// Import CSV
// ---------------------------------------------------------------------------

/** POST /api/technicien/essais/import/apercu — parse et valide sans écrire. */
export async function importApercu(fichier: File): Promise<ImportApercuResponse> {
  const formData = new FormData();
  formData.append("fichier", fichier);
  const res = await apiClient.upload<ApiResponse<ImportApercuResponse>>(
    "/technicien/essais/import/apercu",
    formData,
  );
  return res.data;
}

/** POST /api/technicien/essais/import/confirmer — applique l'import (tout ou rien). */
export async function importConfirmer(fichier: File): Promise<ImportConfirmerResponse> {
  const formData = new FormData();
  formData.append("fichier", fichier);
  const res = await apiClient.upload<ApiResponse<ImportConfirmerResponse>>(
    "/technicien/essais/import/confirmer",
    formData,
  );
  return res.data;
}
