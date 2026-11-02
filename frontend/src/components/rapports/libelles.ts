/**
 * Libellés français partagés des rapports clients.
 * Aucune valeur inconnue n'est affichée telle quelle sans repli sur le code brut.
 */

const LIBELLE_TYPE_RAPPORT: Record<string, string> = {
  FINAL: "Final",
  PARTIEL: "Partiel",
  INTERMEDIAIRE: "Intermédiaire",
};

/** Libellé du type de rapport ; repli sur le code brut, jamais de plantage. */
export function libelleTypeRapport(type: string | null | undefined): string {
  if (type == null || type === "") return "—";
  return LIBELLE_TYPE_RAPPORT[type] ?? type;
}

/** Libellé de conformité d'une ligne d'essai ; « Non évaluée » si absente. */
export function libelleConformiteRapport(conformite: boolean | null | undefined): string {
  if (conformite == null) return "Non évaluée";
  return conformite ? "Conforme" : "Non conforme";
}
