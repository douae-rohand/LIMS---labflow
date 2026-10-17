import { createFileRoute, Link } from "@tanstack/react-router";
import { CardsSkeleton, LandingState } from "@/components/landing/LandingState";
import { Container } from "@/components/layout/Container";
import { Button, SectionHeader } from "@/components/lab";
import { PaniersListe } from "@/components/panier/PaniersListe";
import { usePaniers } from "@/hooks/usePanier";

export const Route = createFileRoute("/app/panier/")({
  head: () => ({
    meta: [{ title: "Mon panier — LabFlow LIMS" }],
  }),
  component: PanierIndexPage,
});

function PanierIndexPage() {
  const paniersQuery = usePaniers();
  const paniers = paniersQuery.data ?? [];

  return (
    <Container className="py-10">
      <SectionHeader
        align="left"
        eyebrow="Commande"
        title="Mon panier"
        description="Un panier par laboratoire. Ouvrez-en un pour ajuster les essais et valider la commande."
      />

      <div className="mt-8">
        <LandingState
          loading={paniersQuery.isPending}
          error={paniersQuery.error}
          onRetry={() => {
            void paniersQuery.refetch();
          }}
          empty={paniers.length === 0}
          emptyMessage="Votre panier est vide. Ajoutez des essais depuis le catalogue d'un laboratoire."
          skeleton={<CardsSkeleton count={2} />}
        >
          <PaniersListe paniers={paniers} />
        </LandingState>
      </div>

      {paniers.length === 0 ? (
        <div className="mt-6">
          <Link to="/laboratoires">
            <Button variant="secondary">Parcourir les laboratoires</Button>
          </Link>
        </div>
      ) : null}
    </Container>
  );
}
