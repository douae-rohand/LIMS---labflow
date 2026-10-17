import { formatMontant } from "./format";

/**
 * Récapitulatif du panier. Le total affiché est celui renvoyé par le serveur :
 * il ne compte que les essais disponibles.
 */
export function ResumePanier({
  sousTotal,
  nombreLignes,
  nombreIndisponibles,
}: {
  sousTotal: number | string | null;
  nombreLignes: number;
  nombreIndisponibles: number;
}) {
  return (
    <div className="rounded-3xl border border-brand-900/10 bg-surface-card p-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Total des essais disponibles
          </p>
          <p className="mt-1 text-2xl font-extrabold text-ink-900">{formatMontant(sousTotal)}</p>
        </div>
        <p className="text-sm text-muted-foreground">
          {nombreLignes} ligne{nombreLignes > 1 ? "s" : ""} au panier
        </p>
      </div>
      {nombreIndisponibles > 0 ? (
        <p className="mt-3 text-xs text-amber-800">
          {nombreIndisponibles} ligne{nombreIndisponibles > 1 ? "s" : ""} indisponible
          {nombreIndisponibles > 1 ? "s" : ""} — exclue{nombreIndisponibles > 1 ? "s" : ""} du total.
        </p>
      ) : null}
    </div>
  );
}
