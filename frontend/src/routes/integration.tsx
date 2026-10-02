import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { Button, Card, Checkbox, Input, Label, Select, Textarea } from "@/components/lab";
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
  const [sent, setSent] = useState(false);
  return (
    <PublicLayout>
      <Container className="grid gap-10 py-16 lg:grid-cols-[1fr_1.1fr]">
        <div className="flex flex-col gap-5">
          <h1 className="text-4xl font-extrabold text-ink-900">Intégrer mon laboratoire</h1>
          <p className="text-muted-foreground">
            Décrivez votre laboratoire, nous préparons un espace isolé avec vos rôles, votre catalogue d'essais et vos modèles de rapports.
          </p>
          <Photo src={photos.sampling.src} alt={photos.sampling.alt} className="aspect-[4/3]" />
        </div>
        <Card variant="glossy" padding="lg">
          {sent ? (
            <p className="text-lg font-bold text-brand-900">Merci, votre demande a bien été enregistrée.</p>
          ) : (
            <form className="flex flex-col gap-4" onSubmit={(e) => { e.preventDefault(); setSent(true); }}>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="lab">Nom du laboratoire</Label>
                <Input id="lab" required />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="mail">Email de contact</Label>
                <Input id="mail" type="email" required />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="domain">Domaine d'analyse</Label>
                <Select id="domain">
                  <option>Médical</option>
                  <option>Industriel</option>
                  <option>Environnemental</option>
                  <option>Pharmaceutique et cosmétique</option>
                </Select>
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="msg">Besoins</Label>
                <Textarea id="msg" />
              </div>
              <Checkbox id="iso" label="Laboratoire accrédité ISO/IEC 17025" />
              <Button type="submit" className="mt-2">Envoyer la demande</Button>
            </form>
          )}
        </Card>
      </Container>
    </PublicLayout>
  );
}
