/**
 * Libellés français partagés des enquêtes de satisfaction de l'espace client.
 * Le serveur expose un booléen `repondue` : le filtre d'URL en dérive deux états.
 */

export type VariantBadge = "lime" | "mint" | "soft" | "dark" | "outline";

/** État d'une enquête tel que porté par l'URL (`statut`). */
export type StatutEnqueteClient = "A_REPONDRE" | "REPONDUE";

export const STATUTS_ENQUETE: StatutEnqueteClient[] = ["A_REPONDRE", "REPONDUE"];

export const LIBELLE_STATUT_ENQUETE: Record<StatutEnqueteClient, string> = {
  A_REPONDRE: "À répondre",
  REPONDUE: "Répondue",
};

const VARIANT_STATUT: Record<StatutEnqueteClient, VariantBadge> = {
  A_REPONDRE: "outline",
  REPONDUE: "lime",
};

export const NOTE_MIN = 1;
export const NOTE_MAX = 5;

/** Libellé de l'état d'une enquête à partir du booléen serveur. */
export function libelleStatutEnquete(repondue: boolean): string {
  return repondue ? LIBELLE_STATUT_ENQUETE.REPONDUE : LIBELLE_STATUT_ENQUETE.A_REPONDRE;
}

/** Variante du badge d'état d'une enquête. */
export function variantStatutEnquete(repondue: boolean): VariantBadge {
  return repondue ? VARIANT_STATUT.REPONDUE : VARIANT_STATUT.A_REPONDRE;
}

/** Note affichée sur 5 ; « — » si absente. */
export function libelleNote(note: number | null | undefined): string {
  if (note == null) return "—";
  return `${note} / ${NOTE_MAX}`;
}
