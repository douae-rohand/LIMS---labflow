import { LabMap } from "@/components/map/LabMap";
import { LandingState, CardsSkeleton } from "@/components/landing/LandingState";
import { Container } from "@/components/layout/Container";
import { SectionHeader } from "@/components/lab";
import { useLandingPublic } from "@/hooks/useLandingPublic";
import type { BaseCatalogue } from "@/lib/redirect";

/**
 * Carte des laboratoires actifs, rendue à l'identique dans l'espace public et
 * dans l'espace client. `base` détermine la cible du lien « Voir la fiche ».
 */
export function PageCarte({ base }: { base: BaseCatalogue }) {
  const { data, isPending, error, refetch } = useLandingPublic();
  const laboratoires = data?.laboratoires ?? [];
  const withCoords = laboratoires.filter(
    (lab) => lab.latitude != null && lab.longitude != null,
  );

  return (
    <Container className="py-12 sm:py-16">
      <SectionHeader
        align="left"
        eyebrow="Carte"
        title="Laboratoires actifs au Maroc"
        description="Les marqueurs correspondent aux laboratoires ACTIF exposés par l'API, uniquement s'ils ont des coordonnées GPS en base."
      />
      <div className="mt-10">
        <LandingState
          loading={isPending}
          error={error}
          onRetry={() => {
            void refetch();
          }}
          empty={withCoords.length === 0}
          emptyMessage={
            laboratoires.length === 0
              ? "Aucun laboratoire actif n'est encore enregistré."
              : "Les laboratoires actifs n'ont pas encore de coordonnées GPS."
          }
          skeleton={<CardsSkeleton count={1} />}
        >
          <LabMap laboratoires={withCoords} variant="full" base={base} />
        </LandingState>
      </div>
    </Container>
  );
}
