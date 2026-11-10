import type { LucideIcon } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { obtenirStatistiquesPlateforme } from "@/api/gestionLaboratoires";

export function useStatistiquesPlateforme() {
  return useQuery({
    queryKey: ["statistiques-plateforme"],
    queryFn: obtenirStatistiquesPlateforme,
    refetchInterval: 30_000,
  });
}

export function Indicateurs({
  cartes,
}: {
  cartes: { libelle: string; valeur: number | undefined; icone: LucideIcon }[];
}) {
  return (
    <section aria-label="Indicateurs de la plateforme" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
      {cartes.map((carte) => (
        <article
          key={carte.libelle}
          className="rounded-2xl border border-brand-900/10 bg-card p-4"
        >
          <div className="flex items-start justify-between gap-3">
            <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">{carte.libelle}</p>
            <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-mint-200/80 text-brand-900">
              <carte.icone className="size-4" aria-hidden="true" />
            </span>
          </div>
          <p className="mt-3 text-3xl font-extrabold tracking-tight text-ink-900 tabular-nums">
            {carte.valeur == null ? "—" : carte.valeur}
          </p>
        </article>
      ))}
    </section>
  );
}
