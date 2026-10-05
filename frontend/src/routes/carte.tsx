import { createFileRoute } from "@tanstack/react-router";
import { LabMap } from "@/components/map/LabMap";
import { LandingState, CardsSkeleton } from "@/components/landing/LandingState";
import { Container } from "@/components/layout/Container";
import { SectionHeader } from "@/components/lab";
import { useLandingPublic } from "@/hooks/useLandingPublic";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/carte")({
  head: () => ({
    meta: [
      { title: "Carte des laboratoires — LabFlow LIMS" },
      {
        name: "description",
        content: "Localisez les laboratoires actifs LabFlow au Maroc et calculez un itinéraire.",
      },
      { property: "og:title", content: "Carte des laboratoires — LabFlow LIMS" },
      { property: "og:type", content: "website" },
    ],
  }),
  component: CartePage,
});

function CartePage() {
  const { data, isPending, error, refetch } = useLandingPublic();
  const laboratoires = data?.laboratoires ?? [];
  const withCoords = laboratoires.filter(
    (lab) => lab.latitude != null && lab.longitude != null,
  );

  return (
    <PublicLayout>
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
            <LabMap laboratoires={withCoords} variant="full" />
          </LandingState>
        </div>
      </Container>
    </PublicLayout>
  );
}
