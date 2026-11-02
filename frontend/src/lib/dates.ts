/**
 * dates.ts — Formatage des dates partagé par l'espace client.
 *
 * Deux natures de dates cohabitent :
 *  - Instant ISO (rapports, enquêtes, paiements) → `formatDateFr` ;
 *  - LocalDate « YYYY-MM-DD » (factures) → `formatDateLocaleFr`, qui n'appelle
 *    JAMAIS `new Date("YYYY-MM-DD")` (ce parse est interprété en UTC et décale
 *    la date selon le fuseau du navigateur).
 */

/** Date-heure (Instant ISO) au format français ; « — » si absente ou invalide. */
export function formatDateFr(valeur: string | null | undefined): string {
  if (valeur == null || valeur === "") return "—";
  const date = new Date(valeur);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" }).format(date);
}

/**
 * Date locale « YYYY-MM-DD » au format français, sans conversion de fuseau :
 * la chaîne est découpée puis recomposée en heure locale.
 */
export function formatDateLocaleFr(valeur: string | null | undefined): string {
  if (valeur == null) return "—";
  const correspondance = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valeur);
  if (correspondance === null) return "—";
  const annee = Number(correspondance[1]);
  const mois = Number(correspondance[2]);
  const jour = Number(correspondance[3]);
  if (!Number.isInteger(annee) || !Number.isInteger(mois) || !Number.isInteger(jour)) {
    return "—";
  }
  if (mois < 1 || mois > 12 || jour < 1 || jour > 31) return "—";
  const date = new Date(annee, mois - 1, jour);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium" }).format(date);
}

/** Date du jour au format « YYYY-MM-DD », en heure locale. */
export function aujourdHuiLocale(): string {
  const maintenant = new Date();
  const annee = maintenant.getFullYear();
  const mois = String(maintenant.getMonth() + 1).padStart(2, "0");
  const jour = String(maintenant.getDate()).padStart(2, "0");
  return `${annee}-${mois}-${jour}`;
}

/**
 * Indique si une échéance « YYYY-MM-DD » est dépassée. La comparaison se fait
 * entre chaînes (l'ordre lexicographique coïncide avec l'ordre chronologique),
 * sans aucune conversion de fuseau.
 */
export function echeanceDepassee(
  dateEcheance: string | null | undefined,
  reference: string = aujourdHuiLocale(),
): boolean {
  if (dateEcheance == null || dateEcheance === "") return false;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dateEcheance)) return false;
  return dateEcheance < reference;
}
