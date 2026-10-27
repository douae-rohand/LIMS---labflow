import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { LaboratoireCard } from "@/components/laboratoires/LaboratoireCard";
import { CardsSkeleton, LandingState } from "@/components/landing/LandingState";
import { Input, SectionHeader } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { useLaboratoiresPublic } from "@/hooks/useLaboratoiresPublic";
import type { BaseCatalogue } from "@/lib/redirect";
import { matchesLab } from "@/lib/map/geocode";

/**
 * Annuaire des laboratoires actifs, rendu à l'identique dans l'espace public et
 * dans l'espace client. `base` détermine la cible des fiches (`/laboratoires`
 * ou `/app/laboratoires`). La mise en page (PublicLayout / shell client) est
 * fournie par la route appelante.
 */
export function PageAnnuaire({ base }: { base: BaseCatalogue }) {
  const { data, isPending, error, refetch } = useLaboratoiresPublic();
  const laboratoires = data ?? [];
  const [query, setQuery] = useState("");

  const filtered = useMemo(() => {
    if (query.trim().length < 2) return laboratoires;
    return laboratoires.filter((lab) => matchesLab(lab, query));
  }, [laboratoires, query]);

  return (
    <Container className="py-12 sm:py-16">
      <SectionHeader
        align="left"
        eyebrow="Laboratoires"
        title="Tous les laboratoires actifs"
        description="Fiches issues de la base centrale. Ouvrez un laboratoire pour voir ses coordonnées et les analyses qu'il propose."
      />
      <div className="mt-8 max-w-md">
        <div className="relative">
          <Search
            className="pointer-events-none absolute top-1/2 left-4 size-4 -translate-y-1/2 text-brand-600"
            aria-hidden="true"
          />
          <Input
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Rechercher par nom, ville ou code"
            className="pl-11"
            autoComplete="off"
          />
        </div>
      </div>
      <div className="mt-10">
        <LandingState
          loading={isPending}
          error={error}
          onRetry={() => {
            void refetch();
          }}
          empty={filtered.length === 0}
          emptyMessage={
            laboratoires.length === 0
              ? "Aucun laboratoire actif n'est encore enregistré."
              : "Aucun laboratoire ne correspond à cette recherche."
          }
          skeleton={<CardsSkeleton count={3} />}
        >
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((lab) => (
              <LaboratoireCard key={lab.id} laboratoire={lab} base={base} />
            ))}
          </div>
        </LandingState>
      </div>
    </Container>
  );
}
