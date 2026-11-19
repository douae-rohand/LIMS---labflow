import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";

/**
 * Assistant IA de LabFlow (tous les espaces et pages publiques).
 *
 *  POST /api/ia/assistant          (utilisateur connecté, tous rôles)
 *  POST /api/public/ia/assistant   (pages publiques, sans connexion)
 *    Corps   : { question: string; historique?: MessageHistorique[] } (6 messages au plus)
 *    Réponse : { reponse: string; sources: SourceAssistant[] }
 *    Erreurs : 429 (limite de questions ou fournisseur saturé), 503 (assistant indisponible)
 */

export interface MessageHistorique {
  role: "client" | "assistant";
  texte: string;
}

export interface SourceAssistant {
  titre: string;
  /** Fichier de documentation, ou « catalogue » pour une fiche d'analyse. */
  source: string;
  /** Page de l'application à ouvrir, calculée par le serveur selon l'espace (jamais par le modèle). */
  lien: string | null;
  /** Tarif de l'analyse (fiches de catalogue). */
  prix: string | null;
  /** Lien qui ajoute l'analyse au panier (visiteur et client). */
  lienAjout: string | null;
}

export interface ReponseAssistant {
  reponse: string;
  sources: SourceAssistant[];
}

/**
 * @param publique vrai sur les pages publiques (visiteur non connecté) : route sans authentification,
 *                 limitée par adresse IP côté serveur.
 */
export async function poserQuestionAssistant(
  question: string,
  historique: MessageHistorique[],
  publique = false,
): Promise<ReponseAssistant> {
  const res = await apiClient.post<ApiResponse<ReponseAssistant>>(
    publique ? "/public/ia/assistant" : "/ia/assistant",
    { question, historique: historique.slice(-6) },
    // Le modèle peut mettre quelques secondes à répondre
    { timeout: 45_000 },
  );
  return res.data;
}

export function messageErreurAssistant(erreur: unknown): string {
  if (erreur instanceof ApiError) {
    if (erreur.status === 0) return "Connexion impossible. Vérifiez votre réseau et réessayez.";
    const message = erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "");
    if (erreur.status === 429 || erreur.status === 503 || erreur.status === 400) {
      return message || "L'assistant est momentanément indisponible.";
    }
  }
  return "L'assistant est momentanément indisponible. Réessayez plus tard.";
}
