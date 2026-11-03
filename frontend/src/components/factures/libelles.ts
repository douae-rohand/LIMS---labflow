/**
 * Libellés français partagés des factures de l'espace client.
 * Le statut est recalculé côté serveur : seules PAYEE et IMPAYEE existent ici.
 */

export type VariantBadge = "lime" | "mint" | "soft" | "dark" | "outline";

const LIBELLE_STATUT: Record<string, string> = {
  PAYEE: "Payée",
  IMPAYEE: "Impayée",
};

const VARIANT_STATUT: Record<string, VariantBadge> = {
  PAYEE: "lime",
  IMPAYEE: "outline",
};

/** Libellé du statut d'une facture ; repli sur le code brut. */
export function libelleStatutFacture(statut: string | null | undefined): string {
  if (statut == null || statut === "") return "—";
  return LIBELLE_STATUT[statut] ?? statut;
}

/** Variante d'affichage du badge de statut d'une facture. */
export function variantStatutFacture(statut: string): VariantBadge {
  return VARIANT_STATUT[statut] ?? "soft";
}
