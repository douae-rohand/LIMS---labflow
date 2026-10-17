import { Badge } from "@/components/lab";
import type { EchantillonClient } from "@/api/demandesClient";
import { libelleConformite } from "./libelles";

/** Échantillons reçus par le laboratoire pour cette demande. */
export function EchantillonsRecus({ echantillons }: { echantillons: EchantillonClient[] }) {
  if (echantillons.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        Aucun échantillon enregistré par le laboratoire pour le moment.
      </p>
    );
  }

  return (
    <ul className="flex flex-col gap-3">
      {echantillons.map((echantillon, index) => (
        <li
          key={echantillon.reference ?? index}
          className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-3"
        >
          <div className="min-w-0">
            <p className="font-mono text-xs text-muted-foreground">
              {echantillon.reference ?? "Sans référence"}
            </p>
            <p className="mt-0.5 text-sm font-semibold text-ink-900">
              {echantillon.nature ?? "Nature non précisée"}
            </p>
            {echantillon.motif !== null && echantillon.motif !== "" ? (
              <p className="mt-0.5 text-xs text-muted-foreground">{echantillon.motif}</p>
            ) : null}
          </div>
          <Badge variant={echantillon.conformite === false ? "outline" : "soft"} size="sm">
            {libelleConformite(echantillon.conformite)}
          </Badge>
        </li>
      ))}
    </ul>
  );
}
