import { ApiError } from "@/api/axios";

export function formaterDate(valeur?: string | null): string {
  if (!valeur) return "—";
  const date = new Date(valeur);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "long" }).format(date);
}

export function messageErreur(erreur: unknown, defaut: string): string {
  if (erreur instanceof ApiError) {
    return erreur.message.replace(/^\[[A-Z0-9_]+\]\s*/, "");
  }
  return defaut;
}

export function texte(valeur?: string | null): string {
  return valeur && valeur.trim() ? valeur : "—";
}
