import { createFileRoute, redirect } from "@tanstack/react-router";
import { Hero } from "@/components/landing/Hero";
import {
  AiSection,
  Automation,
  Domains,
  Faq,
  Features,
  FinalCta,
  Laboratoires,
  Roles,
  Security,
  Stats,
  Validation,
  WorkflowSection,
} from "@/components/landing/Sections";
import { photos } from "@/data/images";
import { PublicLayout } from "@/layouts/PublicLayout";
import { redirectionClientConnecte } from "@/lib/redirect";

const title = "LabFlow LIMS — Gestion de laboratoire multi-tenant";
const description =
  "De la demande client au rapport validé : planification, échantillons, résultats, double validation et traçabilité complète.";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title },
      { name: "description", content: description },
      { property: "og:title", content: title },
      { property: "og:description", content: description },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
      { property: "og:image", content: photos.teamLab.src },
      { name: "twitter:image", content: photos.teamLab.src },
    ],
  }),
  beforeLoad: ({ location }) => {
    const cible = redirectionClientConnecte(location.href);
    if (cible !== null) throw redirect({ href: cible });
  },
  component: Home,
});

function Home() {
  return (
    <PublicLayout>
      <Hero />
      <Stats />
      <Laboratoires />
      <Features />
      <WorkflowSection />
      <Roles />
      <Validation />
      <AiSection />
      <Automation />
      <Security />
      <Domains />
      <Faq />
      <FinalCta />
    </PublicLayout>
  );
}
