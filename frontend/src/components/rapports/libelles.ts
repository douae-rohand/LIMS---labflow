/**
 * Libellés français partagés des rapports clients.
 * Aucune valeur inconnue n'est affichée telle quelle sans repli sur le code brut.
 */

/** Libellé de conformité d'une ligne d'essai ; « Non évaluée » si absente. */
export function libelleConformiteRapport(conformite: boolean | null | undefined): string {
  if (conformite == null) return "Non évaluée";
  return conformite ? "Conforme" : "Non conforme";
}
