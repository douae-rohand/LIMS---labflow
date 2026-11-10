import { Badge } from "@/components/lab";
import type { DemandeReception, EtatReception } from "@/api/accueil";

export const LIBELLES_ETAT: Record<EtatReception, string> = {
  A_RECEVOIR: "À recevoir",
  PARTIELLE: "Réception partielle",
  COMPLETE: "Reçue",
};

export function BadgeEtatReception({ demande }: { demande: DemandeReception }) {
  const variante = demande.etat === "COMPLETE" ? "mint" : demande.etat === "PARTIELLE" ? "soft" : "lime";
  return <Badge size="sm" variant={variante}>{LIBELLES_ETAT[demande.etat]}</Badge>;
}

/** Client ou patient affiché pour une demande. */
export function demandeur(demande: DemandeReception): string {
  if (demande.patient && demande.client && demande.patient !== demande.client) {
    return `${demande.patient} (${demande.client})`;
  }
  return demande.patient ?? demande.client ?? "Client";
}

export function formaterMontant(valeur: number): string {
  return new Intl.NumberFormat("fr-MA", { style: "currency", currency: "MAD" }).format(valeur ?? 0);
}
