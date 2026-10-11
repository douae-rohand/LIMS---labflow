import type { LucideIcon } from "lucide-react";
import { Building2, CirclePause, CircleCheck, Inbox } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { obtenirStatistiquesPlateforme, type StatistiquesPlateforme } from "@/api/gestionLaboratoires";

export function useStatistiquesPlateforme() {
  return useQuery({
    queryKey: ["statistiques-plateforme"],
    queryFn: obtenirStatistiquesPlateforme,
  });
}

export function Indicateurs({ statistiques }: { statistiques: StatistiquesPlateforme | undefined }) {
  const cartes: { libelle: string; valeur: number | undefined; icone: LucideIcon }[] = [
    { libelle: "Total des laboratoires", valeur: statistiques?.totalLaboratoires, icone: Building2 },
    { libelle: "Laboratoires actifs", valeur: statistiques?.laboratoiresActifs, icone: CircleCheck },
    { libelle: "Laboratoires désactivés", valeur: statistiques?.laboratoiresDesactives, icone: CirclePause },
    { libelle: "Demandes en attente", valeur: statistiques?.demandesEnAttente, icone: Inbox },
  ];

  return (
    <section aria-label="Indicateurs de la plateforme" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {cartes.map((carte) => (
        <article
          key={carte.libelle}
          className="flex items-center gap-3 rounded-2xl border border-brand-900/10 bg-card px-4 py-3"
        >
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-surface-alt text-brand-900">
            <carte.icone className="size-4" aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <p className="text-xs font-medium text-muted-foreground">{carte.libelle}</p>
            <p className="text-2xl font-extrabold tracking-tight text-ink-900">
              {carte.valeur == null ? "—" : carte.valeur}
            </p>
          </div>
        </article>
      ))}
    </section>
  );
}
