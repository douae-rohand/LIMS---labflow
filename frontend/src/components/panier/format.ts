/** Formatage monétaire (MAD) et de durée partagé par les vues panier. */

/** Montant en dirhams marocains. Retourne « — » si la valeur est absente ou invalide. */
export function formatMontant(value: number | string | null | undefined): string {
  if (value == null || value === "") return "—";
  const montant = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(montant)) return "—";
  return new Intl.NumberFormat("fr-MA", {
    style: "currency",
    currency: "MAD",
  }).format(montant);
}

/** Durée exprimée en minutes, rendue en heures/minutes. */
export function formatDuree(minutes: number | null | undefined): string | null {
  if (minutes == null) return null;
  if (minutes >= 60 && minutes % 60 === 0) return `${minutes / 60} h`;
  if (minutes >= 60) return `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
  return `${minutes} min`;
}
