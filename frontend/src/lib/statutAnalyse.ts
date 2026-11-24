export type EtatEchantillon = "ATTENDU" | "CONFORME" | "NON_CONFORME";

/**
 * Libellé d'une analyse pas encore démarrée. Le backend garde le statut ATTENTE_ECHANTILLON jusqu'à ce
 * que le technicien démarre l'essai : c'est l'échantillon et l'affectation qui disent où elle en est.
 */
export function libelleNonDemarree(
  etatEchantillon: EtatEchantillon | null | undefined,
  affectee: boolean,
): string {
  if (etatEchantillon === "NON_CONFORME") return "Échantillon non conforme";
  if (etatEchantillon !== "CONFORME") return "En attente d'échantillon";
  return affectee ? "Planifiée" : "À affecter";
}
