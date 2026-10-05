import { Link, createFileRoute } from "@tanstack/react-router";
import { ArrowLeft, Mail, MapPin, Phone } from "lucide-react";
import type { ReactNode } from "react";
import { AnalyseCard } from "@/components/laboratoires/AnalyseCard";
import { CardsSkeleton, LandingState } from "@/components/landing/LandingState";
import { LabMap } from "@/components/map/LabMap";
import { Badge, Button, Card, SectionHeader } from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { useAnalysesLaboratoire, useLaboratoirePublic } from "@/hooks/useLaboratoiresPublic";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/laboratoires/$id")({
  head: ({ params }) => ({
    meta: [
      { title: `Laboratoire ${params.id} — LabFlow LIMS` },
      {
        name: "description",
        content: "Fiche d'un laboratoire LabFlow : contact, localisation et analyses proposées.",
      },
    ],
  }),
  component: LaboratoireDetailPage,
});

function LaboratoireDetailPage() {
  const { id } = Route.useParams();
  const laboratoireId = Number(id);
  const labQuery = useLaboratoirePublic(laboratoireId);
  const analysesQuery = useAnalysesLaboratoire(laboratoireId);
  const lab = labQuery.data;
  const analyses = analysesQuery.data ?? [];
  const hasCoords = lab?.latitude != null && lab.longitude != null;

  return (
    <PublicLayout>
      <Container className="py-12 sm:py-16">
        <Link to="/laboratoires" className="inline-flex">
          <Button variant="ghost" icon={<ArrowLeft className="size-4" aria-hidden="true" />} iconPosition="left">
            Tous les laboratoires
          </Button>
        </Link>

        <div className="mt-6">
          <LandingState
            loading={labQuery.isPending}
            error={labQuery.error}
            onRetry={() => {
              void labQuery.refetch();
            }}
            empty={!lab}
            emptyMessage="Ce laboratoire est introuvable ou n'est plus actif."
            skeleton={<CardsSkeleton count={2} />}
          >
            {lab ? (
              <div className="flex flex-col gap-10">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <SectionHeader
                    align="left"
                    eyebrow={lab.code}
                    title={lab.raisonSociale}
                    description={
                      lab.ville?.trim()
                        ? `${lab.ville}${lab.adresse?.trim() ? ` — ${lab.adresse}` : ""}`
                        : "Localisation non renseignée"
                    }
                  />
                  <Badge variant="mint">{lab.statut}</Badge>
                </div>

                <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
                  <Card variant="glossy">
                    <h2 className="text-lg font-extrabold text-ink-900">Informations générales</h2>
                    <dl className="mt-5 grid gap-4 sm:grid-cols-2">
                      <InfoItem label="Raison sociale" value={lab.raisonSociale} />
                      <InfoItem label="Code" value={lab.code} />
                      <InfoItem label="Ville" value={lab.ville} />
                      <InfoItem label="Adresse" value={lab.adresse} />
                      <InfoItem
                        label="Téléphone"
                        value={lab.telephone}
                        icon={<Phone className="size-3.5 text-brand-600" aria-hidden="true" />}
                      />
                      <InfoItem
                        label="E-mail"
                        value={lab.email}
                        icon={<Mail className="size-3.5 text-brand-600" aria-hidden="true" />}
                      />
                      <InfoItem label="ICE" value={lab.ice} />
                      {hasCoords ? (
                        <InfoItem
                          label="Coordonnées GPS"
                          value={`${lab.latitude}, ${lab.longitude}`}
                          icon={<MapPin className="size-3.5 text-brand-600" aria-hidden="true" />}
                        />
                      ) : null}
                    </dl>
                  </Card>

                  {hasCoords ? (
                    <LabMap laboratoires={[lab]} variant="embed" className="max-w-none" />
                  ) : (
                    <Card variant="surface" className="flex items-center justify-center">
                      <p className="text-sm text-muted-foreground">
                        Aucune localisation GPS n'est enregistrée pour ce laboratoire.
                      </p>
                    </Card>
                  )}
                </div>

                <section>
                  <SectionHeader
                    align="left"
                    eyebrow="Catalogue"
                    title="Analyses proposées"
                    description="Essais actifs lus dans le schéma de ce laboratoire."
                  />
                  <div className="mt-8">
                    <LandingState
                      loading={analysesQuery.isPending}
                      error={analysesQuery.error}
                      onRetry={() => {
                        void analysesQuery.refetch();
                      }}
                      empty={analyses.length === 0}
                      emptyMessage="Aucune analyse active n'est encore proposée par ce laboratoire."
                      skeleton={<CardsSkeleton count={3} />}
                    >
                      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {analyses.map((analyse) => (
                          <AnalyseCard key={analyse.id} analyse={analyse} />
                        ))}
                      </div>
                    </LandingState>
                  </div>
                </section>
              </div>
            ) : null}
          </LandingState>
        </div>
      </Container>
    </PublicLayout>
  );
}

function InfoItem({
  label,
  value,
  icon,
}: {
  label: string;
  value?: string | null;
  icon?: ReactNode;
}) {
  const text = value?.trim();
  return (
    <div>
      <dt className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">{label}</dt>
      <dd className="mt-1 flex items-start gap-2 text-sm font-semibold text-ink-900">
        {icon}
        {text || "Non renseigné"}
      </dd>
    </div>
  );
}
