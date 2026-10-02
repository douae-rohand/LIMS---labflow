import { createFileRoute } from "@tanstack/react-router";
import { Button, Card, Input, Label } from "@/components/lab";
import { Photo } from "@/components/landing/Photo";
import { Container } from "@/components/layout/Container";
import { photos } from "@/data/images";
import { PublicLayout } from "@/layouts/PublicLayout";

export const Route = createFileRoute("/login")({
  head: () => ({
    meta: [
      { title: "Connexion — LabFlow LIMS" },
      { name: "description", content: "Accédez à votre espace laboratoire LabFlow." },
      { property: "og:title", content: "Connexion — LabFlow LIMS" },
      { property: "og:description", content: "Accédez à votre espace laboratoire LabFlow." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  return (
    <PublicLayout>
      <Container className="grid items-center gap-10 py-16 lg:grid-cols-2">
        <Card variant="glossy" padding="lg" className="mx-auto w-full max-w-md">
          <h1 className="text-2xl font-extrabold text-ink-900">Se connecter</h1>
          <p className="mt-1 text-sm text-muted-foreground">Espace sécurisé de votre laboratoire.</p>
          <form className="mt-6 flex flex-col gap-4" onSubmit={(e) => e.preventDefault()}>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="email">Email professionnel</Label>
              <Input id="email" type="email" placeholder="nom@laboratoire.ma" />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="password">Mot de passe</Label>
              <Input id="password" type="password" />
            </div>
            <Button type="submit" className="mt-2">Continuer</Button>
          </form>
        </Card>
        <Photo src={photos.technicianMicroscope.src} alt={photos.technicianMicroscope.alt} className="hidden aspect-[4/3] lg:block" />
      </Container>
    </PublicLayout>
  );
}
