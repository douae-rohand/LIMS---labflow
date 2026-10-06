import { createFileRoute } from "@tanstack/react-router";
import { IntegrationWizard } from "@/components/integration/IntegrationWizard";
import { Photo } from "@/components/landing/Photo";
import { Container } from "@/components/layout/Container";
import { photos } from "@/data/images";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/integration")({
  head: () => ({
    meta: [
      { title: "Intégrer mon laboratoire — LabFlow LIMS" },
      { name: "description", content: "Demandez le déploiement de LabFlow dans votre laboratoire." },
      { property: "og:title", content: "Intégrer mon laboratoire — LabFlow LIMS" },
      { property: "og:description", content: "Demandez le déploiement de LabFlow dans votre laboratoire." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: IntegrationPage,
});

function IntegrationPage() {
  return (
    <PublicLayout>
      <Container className="grid gap-10 py-16 lg:grid-cols-[0.85fr_1.15fr]">
        <div className="flex flex-col gap-5">
          <h1 className="text-4xl font-extrabold text-ink-900">Intégrer mon laboratoire</h1>
          <p className="text-muted-foreground">
            Déposez une demande complète : informations du laboratoire, localisation, administrateur
            et les 6 documents officiels. Le Super Administrateur vérifie le dossier avant de créer
            votre espace isolé (un laboratoire = un schéma SQL dédié).
          </p>
          <Photo src={photos.sampling.src} alt={photos.sampling.alt} className="aspect-[4/3]" />
        </div>
        <IntegrationWizard />
      </Container>
    </PublicLayout>
  );
}
