import apiClient, { ApiError } from "./axios";
import type { ApiResponse } from "./auth";

/**
 * Assistant IA de LabFlow (tous les espaces et pages publiques).
 *
 *  POST /api/ia/assistant          (utilisateur connecté, tous rôles)
 *  POST /api/public/ia/assistant   (pages publiques, sans connexion)
 *    Corps   : { question: string; historique?: MessageHistorique[] } (6 messages au plus)
 *    Réponse : { reponse: string; sources: SourceAssistant[]; noms?; donneesA? }
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
  /**
   * Personnel : noms à afficher à la place des pseudonymes « TECH_<id> » de la réponse.
   * Le fournisseur d'IA ne voit jamais ces noms ; l'historique renvoyé garde les pseudonymes.
   */
  noms?: Record<string, string>;
  /** Instant ISO de lecture des données du laboratoire, ou null si la réponse vient de la documentation seule. */
  donneesA?: string | null;
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

// -----------------------------------------------------------------------------
// Synthèse IA des résultats (CDC M07) : générée automatiquement à la signature du rapport.
//  GET  /api/ia/syntheses/{demandeId}            → SyntheseIa | null
//  POST /api/ia/syntheses/{demandeId}/regenerer  → SyntheseIa (rapport signé uniquement)
// -----------------------------------------------------------------------------

export interface SyntheseIa {
  demandeId: number;
  contenu: string;
  dateGeneration: string;
  mention: string;
}

export async function lireSyntheseIa(demandeId: number): Promise<SyntheseIa | null> {
  const res = await apiClient.get<ApiResponse<SyntheseIa | null>>(`/ia/syntheses/${demandeId}`);
  return res.data ?? null;
}

export async function regenererSyntheseIa(demandeId: number): Promise<SyntheseIa> {
  const res = await apiClient.post<ApiResponse<SyntheseIa>>(`/ia/syntheses/${demandeId}/regenerer`, {}, { timeout: 60_000 });
  return res.data;
}

export function messageErreurSynthese(erreur: unknown): string {
  if (erreur instanceof ApiError && erreur.status !== 0) {
    const message = erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "");
    if (message) return message;
  }
  return "La synthèse n'a pas pu être générée. Réessayez plus tard.";
}
