import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import type { DemandeDetailClient } from "@/api/demandesClient";
import { messageErreurPanier } from "@/api/paniers";
import { CardsSkeleton, LandingState } from "@/components/landing/LandingState";
import { Container } from "@/components/layout/Container";
import { Button, Card, SectionHeader } from "@/components/lab";
import { CarteConfirmationDemande } from "@/components/panier/CarteConfirmationDemande";
import { FormulaireCommande } from "@/components/panier/FormulaireCommande";
import { LignePanier } from "@/components/panier/LignePanier";
import { ResumePanier } from "@/components/panier/ResumePanier";
import { usePanier, useRetirerDuPanier } from "@/hooks/usePanier";

export const Route = createFileRoute("/app/panier/$laboratoireId")({
  head: () => ({
    meta: [{ title: "Panier — LabFlow LIMS" }],
  }),
  component: PanierLaboratoirePage,
});

function PanierLaboratoirePage() {
  const { laboratoireId } = Route.useParams();
  const id = Number(laboratoireId);
  const [demande, setDemande] = useState<DemandeDetailClient | null>(null);

  const panierQuery = usePanier(id);
  const retraitMutation = useRetirerDuPanier(id);
  const retirerEssai = retraitMutation.mutate;

  const panier = panierQuery.data;
  const lignes = panier?.lignes ?? [];
  const nombreIndisponibles = lignes.filter((ligne) => !ligne.disponible).length;

  const handleRetirer = (essaiId: number) => {
    retirerEssai(essaiId, {
      onSuccess: () => {
        toast.success("Essai retiré du panier.");
      },
      onError: (error) => {
        toast.error(messageErreurPanier(error));
      },
    });
  };

  if (demande) {
    return (
      <Container className="py-10">
        <CarteConfirmationDemande demande={demande} laboratoireId={id} />
      </Container>
    );
  }

  return (
    <Container className="py-10">
      <Link to="/app/panier" className="inline-flex">
        <Button
          variant="ghost"
          icon={<ArrowLeft className="size-4" aria-hidden="true" />}
          iconPosition="left"
        >
          Tous mes paniers
        </Button>
      </Link>

      <div className="mt-6 flex flex-col gap-8">
        <LandingState
          loading={panierQuery.isPending}
          error={panierQuery.error}
          onRetry={() => {
            void panierQuery.refetch();
          }}
          skeleton={<CardsSkeleton count={2} />}
        >
          <div>
            <SectionHeader
              align="left"
              eyebrow="Commande"
              title={panier?.laboratoireNom ?? "Panier"}
              description="Vérifiez les essais puis validez la commande."
            />

            {lignes.length === 0 ? (
              <Card
                variant="surface"
                className="mt-6 flex flex-col items-center gap-4 py-10 text-center"
              >
                <p className="text-sm text-muted-foreground">Ce panier est vide.</p>
                <Link to="/laboratoires">
                  <Button variant="secondary">Parcourir le catalogue</Button>
                </Link>
              </Card>
            ) : (
              <div className="mt-6 flex flex-col gap-3">
                {lignes.map((ligne) => (
                  <LignePanier
                    key={ligne.essaiId}
                    ligne={ligne}
                    enRetrait={
                      retraitMutation.isPending && retraitMutation.variables === ligne.essaiId
                    }
                    onRetirer={() => handleRetirer(ligne.essaiId)}
                  />
                ))}
              </div>
            )}
          </div>
        </LandingState>

        {lignes.length > 0 ? (
          <div className="grid gap-6 lg:grid-cols-[1fr_0.9fr]">
            <ResumePanier
              sousTotal={panier?.sousTotal ?? null}
              nombreLignes={lignes.length}
              nombreIndisponibles={nombreIndisponibles}
            />
            <Card variant="glossy" padding="lg">
              <h2 className="text-lg font-extrabold text-ink-900">Valider la commande</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                Le panier de ce laboratoire sera vidé après la création de la demande.
              </p>
              <div className="mt-5">
                <FormulaireCommande laboratoireId={id} onCree={setDemande} />
              </div>
            </Card>
          </div>
        ) : null}
      </div>
    </Container>
  );
}
