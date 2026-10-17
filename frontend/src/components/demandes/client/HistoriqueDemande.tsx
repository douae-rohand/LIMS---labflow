import type { HistoriqueEntree } from "@/api/demandesClient";
import { formatDate, libelleAction, libelleStatutDemande } from "./libelles";

/** Journal chronologique des événements d'une demande. */
export function HistoriqueDemande({ historique }: { historique: HistoriqueEntree[] }) {
  if (historique.length === 0) {
    return <p className="text-sm text-muted-foreground">Aucun événement enregistré.</p>;
  }

  return (
    <ol className="flex flex-col">
      {historique.map((entree, index) => {
        const transition =
          entree.statutAvant !== null || entree.statutApres !== null
            ? `${libelleStatutDemande(entree.statutAvant)} → ${libelleStatutDemande(entree.statutApres)}`
            : null;
        return (
          <li key={index} className="flex gap-4 pb-5 last:pb-0">
            <div className="flex flex-col items-center">
              <span className="mt-1.5 size-2.5 shrink-0 rounded-full bg-brand-900" aria-hidden="true" />
              <span className="w-px flex-1 bg-brand-900/10" aria-hidden="true" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-ink-900">{libelleAction(entree.action)}</p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                {formatDate(entree.dateAction)}
                {entree.acteur !== null && entree.acteur !== "" ? ` — ${entree.acteur}` : ""}
              </p>
              {transition !== null ? (
                <p className="mt-1 text-xs text-muted-foreground">{transition}</p>
              ) : null}
              {entree.motif !== null && entree.motif !== "" ? (
                <p className="mt-1 text-sm text-ink-900">{entree.motif}</p>
              ) : null}
            </div>
          </li>
        );
      })}
    </ol>
  );
}
