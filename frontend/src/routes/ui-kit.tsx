import { createFileRoute } from "@tanstack/react-router";
import { FlaskConical } from "lucide-react";
import {
  Avatar,
  Badge,
  Button,
  Card,
  IconBox,
  Modal,
  Skeleton,
  Table,
  Tabs,
  Tooltip,
} from "@/components/lab";
import { Container } from "@/components/layout/Container";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/ui-kit")({
  head: () => ({
    meta: [
      { title: "Design system — LabFlow LIMS" },
      { name: "description", content: "Composants visuels de la plateforme LabFlow." },
      { property: "og:title", content: "Design system — LabFlow LIMS" },
      { property: "og:description", content: "Composants visuels de la plateforme LabFlow." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: UiKit,
});

function UiKit() {
  return (
    <PublicLayout>
      <Container className="flex flex-col gap-8 py-16">
        <h1 className="text-4xl font-extrabold text-ink-900">Design system</h1>
        <Card className="flex flex-wrap gap-3">
          <Button>Primaire</Button>
          <Button variant="secondary">Secondaire</Button>
          <Button variant="lime">Lime</Button>
          <Button variant="ghost">Ghost</Button>
          <Button loading>Chargement</Button>
        </Card>
        <Card className="flex flex-wrap items-center gap-3">
          <Badge>Lime</Badge><Badge variant="mint">Mint</Badge><Badge variant="soft">Soft</Badge>
          <Badge variant="dark">Dark</Badge><Badge variant="outline">Outline</Badge>
          <IconBox icon={FlaskConical} variant="brand" /><IconBox icon={FlaskConical} variant="lime" shape="circle" />
          <Avatar initials="SA" />
          <Tooltip content="Infobulle"><Button variant="secondary" size="sm">Survoler</Button></Tooltip>
          <Modal title="Fenêtre" trigger={<Button size="sm">Ouvrir</Button>}>Contenu de la fenêtre.</Modal>
        </Card>
        <div className="grid gap-4 md:grid-cols-3">
          <Card variant="glass">Glass</Card><Card variant="dark">Dark</Card><Card variant="lime">Lime</Card>
        </div>
        <Tabs items={[{ value: "a", label: "Onglet A", content: "Contenu A" }, { value: "b", label: "Onglet B", content: "Contenu B" }]} />
        <Table columns={["Demande", "Statut", "Délai"]} rows={[["DEM-0412", "EN_VALIDATION", "2 j"], ["DEM-0409", "EN_COURS", "1 j"]]} />
        <div className="flex gap-3"><Skeleton className="h-10 w-40" /><Skeleton className="h-10 w-24" /></div>
      </Container>
    </PublicLayout>
  );
}
